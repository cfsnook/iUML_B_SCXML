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

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.tests.sample.scxml.ScxmlFinalType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;

import ac.soton.emf.translator.TranslationDescriptor;
import ac.soton.emf.translator.configuration.IRule;
import ac.soton.emf.translator.utils.Find;
import ac.soton.eventb.statemachines.AbstractNode;
import ac.soton.eventb.statemachines.Final;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.scxml.eventb.utils.Make;
import ac.soton.scxml.eventb.utils.Utils;


/**
 * This rule generates, from an Scxml final state,
 * the final iUML-B node and the final transition that targets it.
 * 
 * The pre-final state, also generated from an Scxml final state,
 * is the source of the final transitions and so
 * must already have been generated (is a dependancy)
 * (see rule ScxmlFinalType2StateRule) 
 * 
 * This rule needs to run late so that it can find the events that are elaborated 
 * by outgoing transitions of the parent state.. the final transition also elaborates them.
 * 
 * @author cfs
 *
 */
public class ScxmlFinalType2FinalRule extends AbstractSCXMLImporterRule implements IRule {


	ScxmlScxmlType scxmlContainer=null;
	ScxmlStateType stateContainer=null;
	List<Statemachine> statemachines = new ArrayList<Statemachine>();

	
	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		stateContainer = (ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, sourceElement.eContainer());
		return scxmlContainer!=null;
	}
	
	/**
	 * the scxml final will be used to generate an iUML-B state
	 *   therefore we need to check that the corresponding parent state-machines have already been generated.
	 * 
	 */
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> translatedElements) throws Exception  {
		String finalName = ((ScxmlFinalType) sourceElement).getId();
		String parentSmName = (stateContainer==null? scxmlContainer.getName() : stateContainer.getId())+"_sm";
		statemachines.clear();
		int refinementLevel = Utils.getRefinementLevel(sourceElement.eContainer());
		int depth = getRefinementDepth(sourceElement);
		for (int i=refinementLevel; i<=depth; i++){
			Machine m = (Machine) Find.translatedElement(translatedElements, null, null, MachinePackage.Literals.MACHINE, Utils.getMachineName(scxmlContainer,i));
			Statemachine sm = (Statemachine) Find.element(m, null, null, StatemachinesPackage.Literals.STATEMACHINE, parentSmName);
			if (sm==null) return false;
			//check that the 'pre-final' state has already been generated from the ScxmlFinal
			statemachines.add(sm);
			if (findState(sm, finalName)==null) return false; 
		}
		return true;
	}
	
	private State findState(Statemachine sm, String finalName) {
		if (finalName==null) return null;
		for (AbstractNode nd : sm.getNodes()){
			if (nd instanceof State && finalName.equals(nd.getName())){
				return (State) nd;
			}
		}
		return null;
	}

	/**
	 * fire late to make sure that the parent state has its outgoing transitions
	 */
	@Override
	public boolean fireLate(){
		return true;
	}
	

	@Override  
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {

		ScxmlFinalType scxmlFinal = (ScxmlFinalType)sourceElement;
		
		for (Statemachine sm : statemachines){
			
			State preFinalState = findState(sm, scxmlFinal.getId());
			
			Final finals = (Final)Make.finalState(sm.getName()+"_final");
			sm.getNodes().add(finals);
		
			Transition tr = (Transition)Make.transition(preFinalState, finals, "");
			sm.getTransitions().add(tr);
			
			State parentState = (State) (sm.eContainer() instanceof State? sm.eContainer() : null);
			EList<Transition> outgoing = parentState.getOutgoing();
			for (Transition out : outgoing){
				tr.getElaborates().addAll(out.getElaborates());
			}
		}
		return Collections.emptyList();
	}

}
