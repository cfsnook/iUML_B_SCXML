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
import org.eclipse.sirius.tests.sample.scxml.ScxmlParallelType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlRaiseType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlTransitionType;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Guard;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;

import ac.soton.emf.translator.TranslationDescriptor;
import ac.soton.emf.translator.configuration.IRule;
import ac.soton.emf.translator.utils.Find;
import ac.soton.eventb.statemachines.AbstractNode;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.scxml.eventb.strings.Strings;
import ac.soton.scxml.eventb.utils.IumlbScxmlAdapter;
import ac.soton.scxml.eventb.utils.Make;
import ac.soton.scxml.eventb.utils.Refinement;
import ac.soton.scxml.eventb.utils.Utils;

/**
 * This rule translates SCXML initial transitions into 
 * an iUML-B initial node and transition.
 * 
 * This rule needs to run late so that it can find the events that are elaborated 
 * by incoming transitions of the parent state.. the initial transition also elaborates them.
 * 
 * @author cfs
 *
 */
public class ScxmlInitialTransitionTypeRule extends AbstractSCXMLImporterRule implements IRule {

	private List<Refinement> refinements = new ArrayList<Refinement>();
	private ScxmlScxmlType scxmlContainer = null;
	
	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		return scxmlContainer!=null && sourceElement.eContainer() instanceof ScxmlInitialType;
	}
	
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> generatedElements) throws Exception  {
		
		ScxmlStateType stateContainer = (ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, sourceElement.eContainer().eContainer());
		//the immediate container state may be in a parallel. If so, the true parent state is the state containing the parallel
		ScxmlStateType parentState = findTrueParentState(sourceElement);
				
		//ScxmlScxmlType scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		
		boolean dependOnIncomers = parentState==null? false :  isATarget(parentState);
		
		refinements.clear();
		int refinementLevel = Utils.getRefinementLevel(stateContainer==null? scxmlContainer : stateContainer);
		int depth = getRefinementDepth(sourceElement);		
		String parentSmName = (stateContainer==null? scxmlContainer.getName() : stateContainer.getId())+"_sm";
		
		for (int i=refinementLevel; i<=depth; i++){
			Refinement ref = new Refinement();
			ref.level = i;
			//check machine is created
			Machine m = (Machine) Find.translatedElement(generatedElements, null, null, MachinePackage.Literals.MACHINE, Utils.getMachineName(scxmlContainer,i));
			ref.machine = m;
			if (ref.machine == null) 
				return false;
			//check statemachine is created
			ref.statemachine = (Statemachine) Find.element(m, null, null, StatemachinesPackage.Literals.STATEMACHINE, parentSmName);
			if (ref.statemachine == null) 
				return false;
			//check source node is created
			String sourceStateName = sourceElement.eContainer() instanceof ScxmlInitialType? ref.statemachine.getName()+"_initialState" : null;
			ref.source = (AbstractNode) Find.element(m, null, null, StatemachinesPackage.Literals.ABSTRACT_NODE, sourceStateName);
			if (ref.source == null) 
				return false;	
			//check target node is created
			String targetStateName = ((ScxmlTransitionType) sourceElement).getTarget().get(0);		//we only support single target - ignore the rest
			ref.target = (AbstractNode) Find.element(m, null, null, StatemachinesPackage.Literals.ABSTRACT_NODE, targetStateName);
			if (ref.target == null) 
				return false;
			//if scxml parent is target, check whether the parent state of the statemachine has its incomers yet
			if (dependOnIncomers){
				EObject parent = ref.statemachine.eContainer();
				if (parent instanceof State && ((State)parent).getIncoming().isEmpty()){
					return false;
				}
			}
			refinements.add(ref);
		}
		return true;
	}

	/**
	 * This returns the closest containing ScxmlState
	 * N.b. it cannot return the element itself - at least one level of containment is achieved.
	 * @param e
	 * @return
	 */
	private ScxmlStateType findParentState(EObject e) {
		ScxmlStateType immediateStateContainer = (ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, e.eContainer());
		return immediateStateContainer;
	}
	
	/**
	 * This returns the closest containing ScxmlState that is not a child of a parallel region
	 * N.b. it cannot return the element itself - at least one level of containment is achieved.
	 * @param e
	 * @return
	 */
	private ScxmlStateType findTrueParentState(EObject e) {
		ScxmlStateType immediateStateContainer = findParentState(e);
		return immediateStateContainer == null? null:
			immediateStateContainer.eContainer() instanceof ScxmlParallelType? (ScxmlStateType) immediateStateContainer.eContainer().eContainer() :
				immediateStateContainer;
	}
	
	

	/**
	 * This checks whether the given state is targeted by any transitions or is specified as an initial state
	 * 
	 * @param state
	 * @return
	 */
	private boolean isATarget(ScxmlStateType state) {
		if (state==null) return false;
		ScxmlStateType parentState = findParentState(state);
		if (parentState==null) {
			if (state.eContainer() instanceof ScxmlScxmlType){
				List<String> ins = ((ScxmlScxmlType)state.eContainer()).getInitial();
				if (ins==null) return false;		//<<<no initial state specified - model is not well-formed
				for (String initialName : ins){
					if (initialName != null && initialName.equals(state.getId())) return true;
				}
			}
		}else{
			List<String> ins = parentState.getInitial1();
			if (ins!=null){
				for (String initialName : ins){
					if (initialName != null && initialName.equals(state.getId())) return true;
				}
			}
			for (ScxmlStateType siblingState : parentState.getState()){
				for (ScxmlTransitionType tr : siblingState.getTransition()){
					for (String targetName : tr.getTarget()){
						if (targetName != null && targetName.equals(state.getId())) return true;
					}
				}
			}
			for (ScxmlInitialType siblingState : parentState.getInitial()){
				ScxmlTransitionType tr = siblingState.getTransition();
				for (String targetName : tr.getTarget()){
					if (targetName != null && targetName.equals(state.getId())) return true;
				}
			}
		}
		return false;
	}

//	/**
//	 * fire late to make sure that the parent state has its incoming transitions
//	 */
//	@Override
//	public boolean fireLate(){
//		return true;
//	}
	
	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> translatedElements) throws Exception {
		//Map<String, Trigger> triggerStore = (Map<String, Trigger>) storage.fetch("triggers");
		ScxmlTransitionType scxmlTransition = ((ScxmlTransitionType) sourceElement);

		//TODO: calculate finalised
		boolean finalised = false;		// if true => no more refinements
		
		for (Refinement ref : refinements){
			
			//add a transition in the iUML-B statemachine
			Transition transition = Make.transition(ref.source, ref.target, "");
			ref.statemachine.getTransitions().add(transition);
			
			EObject parent = ref.statemachine.eContainer();
			if (parent instanceof State){
				for (Transition in : ((State)parent).getIncoming()){
					transition.getElaborates().addAll(in.getElaborates()); 
				}
			}
					
			//add a guard to define the triggers that are raised by this transition
			String raiseList = "";
			// set parameter value for raised triggers
			for (ScxmlRaiseType raise : scxmlTransition.getRaise()){
				if(new IumlbScxmlAdapter(raise).getRefinementLevel() <= ref.level){
					raiseList = raiseList.length()==0? raise.getEvent() : ","+raise.getEvent();
				}
			}	
			// no guard needed if there are no raised triggers.. unless..
			// refinement has been finalised.. in which case we specify that no future triggers will ever be raised by this event
			if (!"".equals(raiseList) || finalised==true){  
				raiseList = "".equals(raiseList)? "\u2205" : "{"+raiseList+" }";
				Guard guard =  (Guard) Make.guard(
						Strings.specificRaisedInternalTriggersGuardName,false,
						Strings.specificRaisedInternalTriggersGuardPredicate(raiseList, finalised),
						Strings.specificRaisedInternalTriggersGuardComment); 
				transition.getGuards().add(guard);
			}
				
			//add any explicit guards of the scxml transition
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
			
			//add any explicit actions of the scxml transition (assigns in SCXML)
			int i=0;
			for (ScxmlAssignType assign : scxmlTransition.getAssign()){
				if(new IumlbScxmlAdapter(assign).getRefinementLevel() <= ref.level){
					Action action = (Action) Make.action(transition.getLabel()+"_act_"+i, Strings.ASSIGN_ACTION(assign), "SCXML transition assign");
					transition.getActions().add(action);
					i++;
				}
			}	
			
		}
		return Collections.emptyList();
	}
	
}
