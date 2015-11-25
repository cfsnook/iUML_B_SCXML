/*******************************************************************************
 *  Copyright (c) 2015 University of Southampton.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *   
 *  Contributors:
 *  University of Southampton - Initial implementation
 *******************************************************************************/
package ac.soton.iumlb.scxml.importer.rules;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.sirius.tests.sample.scxml.ScxmlInitialType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlTransitionType;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;

import ac.soton.eventb.emf.core.extension.coreextension.CoreextensionPackage;
import ac.soton.eventb.emf.diagrams.importExport.AbstractRule;
import ac.soton.eventb.emf.diagrams.importExport.TranslationDescriptor;
import ac.soton.eventb.emf.diagrams.importExport.IRule;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.TranslationKind;
import ac.soton.iumlb.scxml.importer.utils.Find;
import ac.soton.iumlb.scxml.importer.utils.Make;


/**
 * a basis for the scxml to statemachines imported rules
 * 
 * @author cfs
 *
 */
public abstract class AbstractSCXMLImporterRule extends AbstractRule implements IRule {

	protected static final EReference nodes = StatemachinesPackage.Literals.STATEMACHINE__NODES;
	protected static final EReference statemachines = StatemachinesPackage.Literals.STATEMACHINE_OWNER__STATEMACHINES;
	protected static final EReference transitions = StatemachinesPackage.Literals.STATEMACHINE__TRANSITIONS;
	protected static final EReference entryActions = StatemachinesPackage.Literals.STATE__ENTRY_ACTIONS;
	protected static final EReference exitActions = StatemachinesPackage.Literals.STATE__EXIT_ACTIONS;
	protected static final EReference elaborates = CoreextensionPackage.Literals.EVENT_BEVENT_GROUP__ELABORATES;
	protected static final EReference eventGroupGuards = CoreextensionPackage.Literals.EVENT_BEVENT_GROUP__GUARDS;
	protected static final EReference stateInvariants = StatemachinesPackage.Literals.STATE__INVARIANTS;
	
	protected static final TranslationKind tkind = TranslationKind.MULTIVAR;


	
	
	
	/**
	 * This finds an event by name by looking in the following (in this order)
	 *  the machine's events (machine is a field of this class set up by enable())
	 *  the given list of old GenerationDescriptors
	 *  the given list of new GenerationDescriptors
	 *  if no such event is found a new event is created and added to the new GenerationDescriptors
	 *  
	 * @param oldGD
	 * @param newGD
	 * @param eventName
	 * @return
	 */
	protected static Event getOrCreateEvent(ScxmlScxmlType scxmlContainer, List<TranslationDescriptor> oldGD, List<TranslationDescriptor> newGD, String eventName) {
		Machine machine = (Machine) Find.translatedElement(oldGD, null, null, MachinePackage.Literals.MACHINE, scxmlContainer.getName());
		Event ev = (Event) Find.named(machine.getEvents(), eventName);
		if (ev==null) ev =  (Event) Find.translatedElement(oldGD, machine, events, eventName);
		if (ev==null) ev =  (Event) Find.translatedElement(newGD, machine, events, eventName);
		if (ev==null) {
			ev = (Event) Make.event(eventName);
			newGD.add(Make.descriptor(machine, events, ev ,1));
		}
		return ev;
	}
	
	/**
	 * This finds all the events that are elaborated by transitions that target the real parent containing super-state
	 * of the given EObject (which is assumed to be an element in an SCXML state chart)
	 * (n.b. the real parent super-state is not a state representing a parallel region but a container of the parallel)
	 * 
	 * @param scxmlEObject 			an element of a nested SCXML state chart
	 * @param generatedElements
	 * @param ret
	 * @return
	 */
	protected static List<Event> findIncomerEvents(EObject scxmlEObject, List<TranslationDescriptor> generatedElements, List<TranslationDescriptor> ret) {
		List<Event> eventList = new ArrayList<Event>();
		// find scxml container,
		ScxmlScxmlType scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, scxmlEObject);
		// find scxml parent state (i.e. the real parent state, not a region)
		EObject realScxmlParentState = scxmlEObject.eContainer();
		while (!(realScxmlParentState instanceof ScxmlStateType && 
				(		realScxmlParentState.eContainer() instanceof ScxmlStateType ||
						realScxmlParentState.eContainer() instanceof ScxmlScxmlType || 
						realScxmlParentState.eContainer() ==null))){
			realScxmlParentState = realScxmlParentState.eContainer();
		}
		String scxmlParentStateName = ((ScxmlStateType)realScxmlParentState).getId();
		if (scxmlParentStateName == null) return eventList;
		// search contents for transitions that target the real scxml parent state
		for (EObject possible : Find.eAllContents(scxmlContainer, ScxmlPackage.Literals.SCXML_TRANSITION_TYPE)){
			if (((ScxmlTransitionType)possible).getTarget().contains(scxmlParentStateName)){
				// find names of events for this transition
				List<String> eventNames = getEventNames(((ScxmlTransitionType)possible), generatedElements, ret);
				// find events similarly named in machine and/or generation descriptors
				for (String eventName : eventNames){
					Event ev = getOrCreateEvent(scxmlContainer, generatedElements, ret, eventName);
					if (ev!=null && !eventList.contains(ev)) {
						eventList.add(ev);
					}
				}
			}
		}
		return eventList;
	}
	
	/**
	 * This finds all the events that are elaborated by transitions that target the real parent containing super-state
	 * of the given EObject (which is assumed to be an element in an SCXML state chart)
	 * (n.b. the real parent super-state is not a state representing a parallel region but a container of the parallel)
	 * 
	 * @param scxmlEObject 			an element of a nested SCXML state chart
	 * @param generatedElements
	 * @param ret
	 * @return
	 */
	protected static List<Event> findOutgoingEvents(EObject scxmlEObject, List<TranslationDescriptor> generatedElements, List<TranslationDescriptor> ret) {
		List<Event> eventList = new ArrayList<Event>();
		// find scxml container,
		ScxmlScxmlType scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, scxmlEObject);
		// find scxml parent state (i.e. the real parent state, not a region)
		EObject realScxmlParentState = scxmlEObject.eContainer();
		while (!(realScxmlParentState instanceof ScxmlStateType && 
				(		realScxmlParentState.eContainer() instanceof ScxmlStateType ||
						realScxmlParentState.eContainer() instanceof ScxmlScxmlType || 
						realScxmlParentState.eContainer() ==null))){
			realScxmlParentState = realScxmlParentState.eContainer();
		}
		
//		String scxmlParentStateName = ((ScxmlStateType)realScxmlParentState).getId();
//		if (scxmlParentStateName == null) return eventList;
		
		// search contents for transitions that target the real scxml parent state
		
		for (ScxmlTransitionType tr : ((ScxmlStateType)realScxmlParentState).getTransition()){
			// find names of events for this transition
			List<String> eventNames = getEventNames(tr, generatedElements, ret);
			// find events similarly named in machine and/or generation descriptors
			for (String eventName : eventNames){
				Event ev = getOrCreateEvent(scxmlContainer, generatedElements, ret, eventName);
				if (ev!=null && !eventList.contains(ev)) {
					eventList.add(ev);
				}
			}
		}	

		return eventList;
	}
	
	/**
	 * This gets a list of event names that should be elaborated by an iUML-B transition when it is generated from the given
	 * ScxmlTransition. 
	 * 
	 * Usually the list contains one event name which is the trigger event of the scxmlTransition, or if no trigger event,
	 * a name constructed from the source and target state names. 
	 * 
	 * However, initial transitions are treated differently as follows:
	 * if the transition is in an initial state at the outer statemachine level the name is INITIALISATION, 
	 * or if the transition is in an initial state of a nested statemachine the names of all the events that
	 * are associated with incoming transitions to the parent state are returned.
	 * 
	 * @param scxmlTransition
	 * @param generatedElements
	 * @param ret
	 * @return
	 */
	protected static List<String> getEventNames(ScxmlTransitionType scxmlTransition,  List<TranslationDescriptor> generatedElements, List<TranslationDescriptor> ret){
		List<String> eventNames = new ArrayList<String>();
		String eventName = getUniqueName(scxmlTransition);
		EObject source = scxmlTransition.eContainer();
		if (eventName == null || eventName.length()==0 ){
			if (source instanceof ScxmlInitialType) {
				if (getStateContainer(scxmlTransition)==null){
					eventNames.add("INITIALISATION");
				}else{
					//when source is a nested Initial we need to elaborate all the parents incomers events
					for (Event ev : findIncomerEvents(scxmlTransition, generatedElements, ret)){
						eventNames.add(ev.getName());
					}
				}
			}else{
				String sourceName = ((ScxmlStateType)source).getId();
				String targetName = scxmlTransition.getTarget().get(0);
				eventNames.add(sourceName+"_"+targetName);
			}
		}else{
			eventNames.add(eventName);
		}
		return eventNames;
	}
	
	/**
	 * This checks to see whether any other transitions in the same state-machine are triggered by the same event as this one.
	 * If this is the case the containing (source) state is added to the event name to make them unique events.
	 * (because conditional behaviour is not supported in iUML-B)
	 * 
	 * @param scxmlTransition
	 * @return
	 */
	protected static String getUniqueName(ScxmlTransitionType scxmlTransition) {
		String trEvNm = scxmlTransition.getEvent();
		if (trEvNm == null || trEvNm.length()==0) return null;
		EList<ScxmlStateType> states;
		ScxmlStateType stateContainer = getStateContainer(scxmlTransition);
				//(ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, scxmlTransition.eContainer().eContainer());
		if (stateContainer!= null){
			states = stateContainer.getState() ;
		}else{
			ScxmlScxmlType scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, scxmlTransition);			
			states = scxmlContainer.getState();
		}
		for (ScxmlStateType state : states){
			for (ScxmlTransitionType tr : state.getTransition()){
				if (tr!=scxmlTransition && trEvNm.equals(tr.getEvent())){
					return trEvNm+"_"+((ScxmlStateType)scxmlTransition.eContainer()).getId();
				}
			}
		}
		return trEvNm;
	}
	
	/**
	 * returns the containing ScxmlStateType representing the statechart that contains this transition
	 * 
	 * @param scxmlEObject
	 * @return
	 */
	protected static ScxmlStateType getStateContainer(ScxmlTransitionType scxmlTransition){
		return 	scxmlTransition.eContainer() == null? null :
			(ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, scxmlTransition.eContainer().eContainer());
	}
}
