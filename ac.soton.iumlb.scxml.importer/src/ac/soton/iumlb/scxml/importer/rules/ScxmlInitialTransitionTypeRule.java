package ac.soton.iumlb.scxml.importer.rules;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.tests.sample.scxml.ScxmlInitialType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlTransitionType;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Guard;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;

import ac.soton.eventb.emf.diagrams.importExport.TranslationDescriptor;
import ac.soton.eventb.emf.diagrams.importExport.IRule;
import ac.soton.eventb.emf.diagrams.importExport.utils.Find;
import ac.soton.iumlb.scxml.importer.utils.Make;
import ac.soton.eventb.statemachines.AbstractNode;
import ac.soton.eventb.statemachines.Initial;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;

public class ScxmlInitialTransitionTypeRule extends AbstractSCXMLImporterRule implements IRule {


	ScxmlScxmlType scxmlContainer=null;
	ScxmlStateType stateContainer=null;  // the state that contains the state containing this transition (if any)
	Machine machine = null;
	Initial source = null;
	AbstractNode target = null;
	Statemachine parentSm = null;
	
	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		stateContainer = (ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, sourceElement.eContainer().eContainer());
		return scxmlContainer!=null && sourceElement.eContainer() instanceof ScxmlInitialType;
	}
	
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<GenerationDescriptor> generatedElements) throws Exception  {
		machine = (Machine) Find.generatedElement(generatedElements, null, null, MachinePackage.Literals.MACHINE, scxmlContainer.getName());

		String parentSmName = stateContainer==null? scxmlContainer.getName() : stateContainer.getId()+"_sm";
		parentSm = (Statemachine) Find.generatedElement(generatedElements, null, null, StatemachinesPackage.Literals.STATEMACHINE, parentSmName);

		if (parentSm == null) return false;
		
		EObject container = sourceElement.eContainer();
		String sourceStateName = container instanceof ScxmlInitialType? parentSm.getName()+"_initialState" : null;
		source = (Initial) Find.generatedElement(generatedElements, null, null, StatemachinesPackage.Literals.INITIAL, sourceStateName);

		String targetStateName = ((ScxmlTransitionType) sourceElement).getTarget().get(0);		//we only support single target - ignore the rest
		target = (AbstractNode) Find.generatedElement(generatedElements, null, null, StatemachinesPackage.Literals.ABSTRACT_NODE, targetStateName);

		return parentSm!= null && source!=null && target!=null && machine!=null;
	}

	@Override
	public List<GenerationDescriptor> fire(EObject sourceElement, List<GenerationDescriptor> generatedElements) throws Exception {
		assert(source!=null && target!=null && machine!=null) : "Not ready to fire()";

		ScxmlTransitionType scxmlTransition = ((ScxmlTransitionType) sourceElement);
		List<GenerationDescriptor> ret = new ArrayList<GenerationDescriptor>();
		
		Transition transition = Make.transition(source, target, "");
		ret.add(Make.descriptor(parentSm, transitions, transition, 1));
		
		String guardLabel = "cond_";  // may be used in cond guard later
		
		//elaborates	
		String eventName = scxmlTransition.getEvent();
		if (eventName == null || eventName.length()==0 ){
				if (stateContainer==null){
					eventName="INITIALISATION";
					Event ev = (Event) Find.named(machine.getEvents(), eventName);
					if (ev==null) ev =  (Event) Find.generatedElement(generatedElements, machine, events, eventName);
					if (ev==null) {
						ev = (Event) Make.event(eventName);
						ret.add(Make.descriptor(machine, events, ev ,1));
					}
					transition.getElaborates().add(ev);
					//ret.add(Make.descriptor(transition, elaborates, ev ,1));
					guardLabel=guardLabel+"_"+ev.getName();

				}else{
					//when source is a nested Initial we need to elaborate all the parents incomers events
					for (Event ev : findIncomerEvents(scxmlTransition, generatedElements)){
						transition.getElaborates().add(ev);
						//ret.add(Make.descriptor(transition, elaborates, ev ,1));
						guardLabel=guardLabel+"_"+ev.getName();
					}
					eventName = null;
				}
		}

		//cond -> guard
		String cond = scxmlTransition.getCond();
		if (cond!=null && cond.length()>0) {
			Guard guard = (Guard) Make.guard(guardLabel, false, cond, "");
			transition.getGuards().add(guard);
			//ret.add(Make.descriptor(transition, eventGroupGuards, guard, 1));
		}

		return ret;
	}

	private List<Event> findIncomerEvents(EObject scxml, List<GenerationDescriptor> generatedElements) {
		EObject realScxmlParentState = scxml;
		while (!(realScxmlParentState instanceof ScxmlStateType && 
				(		realScxmlParentState.eContainer() instanceof ScxmlStateType ||
						realScxmlParentState.eContainer() instanceof ScxmlScxmlType || 
						realScxmlParentState.eContainer() ==null))){
			realScxmlParentState = realScxmlParentState.eContainer();
		}
		AbstractNode parentState = (AbstractNode) Find.generatedElement(generatedElements, null, null, StatemachinesPackage.Literals.STATE, ((ScxmlStateType)realScxmlParentState).getId());
		List<Event> events = new ArrayList<Event>();
		if (parentState!=null){
			for (Transition in : parentState.getIncoming()){
				for (EObject ev : Find.generatedElements(generatedElements, in, elaborates, MachinePackage.Literals.EVENT))
					if (ev instanceof Event){
						events.add((Event)ev);
					}
			}
		}
		return events;
	}
	
	

}
