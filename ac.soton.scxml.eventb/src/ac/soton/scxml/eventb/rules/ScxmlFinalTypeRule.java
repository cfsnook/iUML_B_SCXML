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
import org.eclipse.sirius.tests.sample.scxml.ScxmlFinalType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;

import ac.soton.emf.translator.TranslationDescriptor;
import ac.soton.emf.translator.configuration.IRule;
import ac.soton.emf.translator.utils.Find;
import ac.soton.eventb.statemachines.Final;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.scxml.eventb.utils.Make;
import ac.soton.scxml.eventb.utils.Utils;

public class ScxmlFinalTypeRule extends AbstractSCXMLImporterRule implements IRule {


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
		((ScxmlFinalType) sourceElement).getId();
		String parentSmName = (stateContainer==null? scxmlContainer.getName() : stateContainer.getId())+"_sm";
		statemachines.clear();
		int refinementLevel = Utils.getRefinementLevel(sourceElement.eContainer());
		int depth = getRefinementDepth(sourceElement);
		for (int i=refinementLevel; i<=depth; i++){
			Machine m = (Machine) Find.translatedElement(translatedElements, null, null, MachinePackage.Literals.MACHINE, Utils.getMachineName(scxmlContainer,i));
			Statemachine sm = (Statemachine) Find.element(m, null, null, StatemachinesPackage.Literals.STATEMACHINE, parentSmName);
			if (sm==null) return false;
			statemachines.add(sm);			
		}
		return true;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {

		ScxmlFinalType scxmlFinal = (ScxmlFinalType)sourceElement;

		State state = null;
		
		for (Statemachine sm : statemachines){
			// scxml final translates into iUML-B state with a transition to an iUML-B final node
			// if nested the transition must elaborate all the events elaborated by transitions that leave the parent state
			if (state==null){
				state = (State)Make.state(scxmlFinal.getId(), "");
			}else{
				state = (State) Utils.refine(scxmlContainer, state);
			}

			sm.getNodes().add(state);
			
			Final finals = (Final)Make.finalState(sm.getName()+"_final");
			sm.getNodes().add(finals);
		
			Transition tr = (Transition)Make.transition(state, finals, "");
			sm.getTransitions().add(tr);

			// add events to tr.elaborates
			Machine machine = (Machine) sm.getContaining(MachinePackage.Literals.MACHINE);
			List<String> eventNames = new ArrayList<String>();
			for (Event ev : Utils.findOutgoingEvents(scxmlFinal, machine, generatedElements)){
				eventNames.add(ev.getName());
			}
			for (String eventName: eventNames){
				Event ev = Utils.getOrCreateEvent(machine, false, generatedElements, eventName);
				tr.getElaborates().add(ev);
			}
		}
		return Collections.emptyList();
	}
	


}
