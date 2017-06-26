/*******************************************************************************
 *  Copyright (c) 2016 University of Southampton.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *   
 *  Contributors:
 *  University of Southampton - Initial implementation
 *******************************************************************************/
package ac.soton.scxml.eventb.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.tests.sample.scxml.ScxmlAssignType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlInitialType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlRaiseType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlTransitionType;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Guard;
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;
import org.eventb.emf.core.machine.Variable;

import ac.soton.emf.translator.TranslationDescriptor;
import ac.soton.emf.translator.configuration.IRule;
import ac.soton.emf.translator.utils.Find;
import ac.soton.eventb.statemachines.AbstractNode;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.scxml.eventb.strings.Strings;
import ac.soton.scxml.eventb.utils.IumlbScxmlAdapter;
import ac.soton.scxml.eventb.utils.Make;
import ac.soton.scxml.eventb.utils.Utils;

/**
 * This rules translates SCXML transitions, excluding initial transitions,
 * into iUML-B transitions.
 * 
 * @author cfs
 *
 */
public class ScxmlTransitionTypeRule extends AbstractSCXMLImporterRule implements IRule {

	private class Refinement {
		private int level = 0;
		private Machine machine = null;
		private Statemachine statemachine = null;
		private AbstractNode source = null;
		private AbstractNode target = null;	
	}
	private List<Refinement> refinements = new ArrayList<Refinement>();

	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		ScxmlScxmlType scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		return scxmlContainer!=null;
	}
	
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> generatedElements) throws Exception  {
		
		ScxmlStateType stateContainer = (ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, sourceElement.eContainer().eContainer());
		ScxmlScxmlType scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		refinements.clear();
		int refinementLevel = Utils.getRefinementLevel(stateContainer==null? scxmlContainer : stateContainer);
		int depth = Utils.getRefinementDepth(sourceElement);		
		String parentSmName = (stateContainer==null? scxmlContainer.getName() : stateContainer.getId())+"_sm";
		
		for (int i=refinementLevel; i<=depth; i++){
			Refinement ref = new Refinement();
			ref.level = i;
			Machine m = (Machine) Find.translatedElement(generatedElements, null, null, MachinePackage.Literals.MACHINE, Utils.getMachineName(scxmlContainer,i));
			ref.machine = m;
			if (ref.machine == null) 
				return false;
			
			ref.statemachine = (Statemachine) Find.element(m, null, null, StatemachinesPackage.Literals.STATEMACHINE, parentSmName);
			if (ref.statemachine == null) 
				return false;
			
			EObject container = sourceElement.eContainer();
			String sourceStateName = container instanceof ScxmlStateType? ((ScxmlStateType)sourceElement.eContainer()).getId() :
										container instanceof ScxmlInitialType? ref.statemachine.getName()+"_initialState" :
											null;
			ref.source = (AbstractNode) Find.element(m, null, null, StatemachinesPackage.Literals.ABSTRACT_NODE, sourceStateName);
			if (ref.source == null) 
				return false;	
			String targetStateName = ((ScxmlTransitionType) sourceElement).getTarget().get(0);		//we only support single target - ignore the rest
			ref.target = (AbstractNode) Find.element(m, null, null, StatemachinesPackage.Literals.ABSTRACT_NODE, targetStateName);
			if (ref.target == null) 
				return false;
			refinements.add(ref);
		}
		return true;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> translatedElements) throws Exception {
		ScxmlTransitionType scxmlTransition = ((ScxmlTransitionType) sourceElement);
		Machine abstractMachine = null;
		for (Refinement ref : refinements){
			
			Transition transition = Make.transition(ref.source, ref.target, "");
			ref.statemachine.getTransitions().add(transition);
			if (ref.machine.getRefinesNames().size()>0){
				abstractMachine = (Machine) Find.translatedElement(translatedElements, null, null, MachinePackage.Literals.MACHINE,
						ref.machine.getRefinesNames().get(0));
			}else{
				abstractMachine=null;
			}
			for (String eventName: Utils.getEventNames(scxmlTransition, ref.machine, translatedElements)){
				Event ev = Utils.getOrCreateEvent(ref.machine, translatedElements, eventName);
				if (abstractMachine!=null){
					Event refinedEvent = (Event) Find.element(abstractMachine, abstractMachine, events, MachinePackage.Literals.EVENT, eventName);
					if (refinedEvent!=null && !ev.getRefinesNames().contains(eventName)){
						//ev.getRefines().add(refinedEvent);
						ev.getRefinesNames().add(eventName);
					}
				}
				transition.getElaborates().add(ev);
			}
//			//cond -> guard
//			String cond = scxmlTransition.getCond();
//			if (cond!=null && cond.length()>0) {
//				Guard guard = (Guard) Make.guard(guardLabel, false, Strings.COND_GUARD(cond), "");
//				transition.getGuards().add(guard);
//			}

			//triggers (these are called events in SCXML)
			String scxmlTransitionEvent = scxmlTransition.getEvent();
			if (scxmlTransitionEvent!=null){
				String[] triggers = scxmlTransitionEvent.split(" ");
				for (String triggerName : triggers){
					String comment = "SCXML trigger event";
					transition.getGuards().add((Guard) Make.guard(
								"scxmlTrigger_"+triggerName,false,
								Strings.INV_PREDICATE(triggerName + "= TRUE"),comment));
					transition.getActions().add((Action) Make.action(
								"scxmlTrigger_"+triggerName+"_reset",
								Strings.ACT_ASSIGN(triggerName + ":= FALSE"),"Consume "+comment));
					
					String eventName = "scxmlTriggerEvent_"+triggerName;
					Event ev = Utils.getOrCreateEvent(ref.machine, translatedElements, eventName);
					if (abstractMachine!=null){
						Event refinedEvent = (Event) Find.element(abstractMachine, abstractMachine, events, MachinePackage.Literals.EVENT, eventName);
						if (refinedEvent!=null && !ev.getRefinesNames().contains(eventName)){
							ev.getRefinesNames().add(eventName);
						}
					}
					if (ev!=null && ev.getActions().isEmpty()){
						ev.getGuards().add((Guard) Make.guard("gd_1", Strings.INV_PREDICATE(triggerName + "= FALSE")));
						ev.getActions().add( (Action) Make.action("act_1", Strings.ACT_ASSIGN(triggerName + ":= TRUE")));
					}
					
					Object var = Find.element(ref.machine, ref.machine, variables, MachinePackage.Literals.VARIABLE, triggerName);
					if (var==null) {
						ref.machine.getVariables().add((Variable) Make.variable(triggerName, comment));
						ref.machine.getInvariants().add((Invariant) Make.invariant("typof_"+triggerName, 
								Strings.INV_PREDICATE(triggerName + ": BOOL"), comment));
						Event initialisation = (Event) Find.element(ref.machine, ref.machine, events, MachinePackage.Literals.EVENT, "INITIALISATION");
						initialisation.getActions().add((Action) Make.action("init_"+triggerName, 
								Strings.ACT_ASSIGN(triggerName + ":= FALSE"), comment));
					}
				}
			}
			
			//guards
			List<IumlbScxmlAdapter> gds = new IumlbScxmlAdapter(scxmlTransition).getGuards();
			for (IumlbScxmlAdapter gd : gds){
				int rl = gd.getRefinementLevel();
				if (rl <= ref.level){
					String name = (String)gd.getAnyAttributeValue("name");
					String derived = (String)gd.getAnyAttributeValue("derived");
					String predicate = (String)gd.getAnyAttributeValue("predicate");
					String comment = (String)gd.getAnyAttributeValue("comment");
					Guard guard =  (Guard) Make.guard(name,Boolean.parseBoolean(derived),Strings.INV_PREDICATE(predicate),comment); 
					transition.getGuards().add(guard);
				}
			}
			
			//actions (assigns in SCXML)
			int i=0;
			for (ScxmlAssignType assign : scxmlTransition.getAssign()){
				if(new IumlbScxmlAdapter(assign).getRefinementLevel() <= ref.level){
					Action action = (Action) Make.action(transition.getLabel()+"_act_"+i, Strings.ASSIGN_ACTION(assign), "SCXML transition assign");
					transition.getActions().add(action);
					i++;
				}
			}	
			
			//raising internal trigger events
			// (n.b. we rely on the triggered transition to generate the trigger variable. Hence if no transition uses this trigger an error will be flagged)
			for (ScxmlRaiseType raise : scxmlTransition.getRaise()){
				if(new IumlbScxmlAdapter(raise).getRefinementLevel() <= ref.level){
					Action action = (Action) Make.action(transition.getLabel()+"_act_"+i, Strings.ACT_ASSIGN(raise.getEvent()+" := TRUE"), "Raise internal SCXML trigger");
					transition.getActions().add(action);
					i++;
				}
			}
		}
		return Collections.emptyList();
	}
	
}
