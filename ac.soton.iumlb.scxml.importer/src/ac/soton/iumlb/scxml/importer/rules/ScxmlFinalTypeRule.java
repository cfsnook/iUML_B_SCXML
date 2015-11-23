/**
 * Copyright (c) 2015 - University of Southampton.
 * All rights reserved. This program and the accompanying materials  are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 *
 * $Id$
 */
package ac.soton.iumlb.scxml.importer.rules;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.tests.sample.scxml.ScxmlFinalType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eventb.emf.core.machine.Event;

import ac.soton.eventb.emf.diagrams.importExport.TranslationDescriptor;
import ac.soton.eventb.emf.diagrams.importExport.IRule;
import ac.soton.eventb.statemachines.Final;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachineOwner;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.eventb.emf.diagrams.importExport.utils.Find;
import ac.soton.iumlb.scxml.importer.utils.Make;

public class ScxmlFinalTypeRule extends AbstractSCXMLImporterRule implements IRule {


	ScxmlScxmlType scxmlContainer=null;
	ScxmlStateType stateContainer=null;
	Statemachine parentSm = null;
	StatemachineOwner smOwner = null;
	
	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		stateContainer = (ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, sourceElement.eContainer());
		return scxmlContainer!=null;
	}
	
	/**
	 * the scxml final will be used to generate an iUML-B state
	 *   therefore we need to check that the corresponding parent statemachine has already been generated.
	 * 
	 */
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> generatedElements) throws Exception  {
		((ScxmlFinalType) sourceElement).getId();
		String parentSmName = stateContainer==null? scxmlContainer.getName() : stateContainer.getId()+"_sm";
		parentSm = (Statemachine) Find.translatedElement(generatedElements, null, null, StatemachinesPackage.Literals.STATEMACHINE, parentSmName);
		return parentSm!=null;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {
		assert(parentSm!=null || smOwner!=null) : "Not ready to fire()";
		
		ScxmlFinalType scxmlFinal = (ScxmlFinalType)sourceElement;
		List<TranslationDescriptor> ret = new ArrayList<TranslationDescriptor>();
		
		// scxml final translates into iUML-B state with a transition to an iUML-B final node
		// if nested the transition must elaborate all the events elaborated by transitions that leave the parent state
		State state = (State)Make.state(scxmlFinal.getId(), "");
		parentSm.getNodes().add(state);
		//ret.add(Make.descriptor(parentSm, nodes, state ,1));
		
		Final finals = (Final)Make.finalState(parentSm.getName()+"_final");
		parentSm.getNodes().add(finals);
	
		Transition tr = (Transition)Make.transition(state, finals, "");
		parentSm.getTransitions().add(tr);

		
		// add events to tr.elaborates
		List<String> eventNames = new ArrayList<String>();
		//when source is a nested Initial we need to elaborate all the parents incomers events
		for (Event ev : findOutgoingEvents(scxmlFinal, generatedElements, ret)){
			eventNames.add(ev.getName());
		}
		
		for (String eventName: eventNames){
			Event ev = getOrCreateEvent(scxmlContainer, generatedElements, ret, eventName);
			tr.getElaborates().add(ev);
			//ret.add(Make.descriptor(transition, elaborates, ev ,1));
		}
		
		return ret;
	}
	


}
