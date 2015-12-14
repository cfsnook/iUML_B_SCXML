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
import java.util.Collections;
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
import ac.soton.eventb.statemachines.AbstractNode;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.iumlb.scxml.importer.strings.Strings;
import ac.soton.eventb.emf.diagrams.importExport.utils.Find;
import ac.soton.iumlb.scxml.importer.utils.Make;

/**
 * This rules translates SCXML transitions, excluding initial transitions,
 * into iUML-B transitions.
 * 
 * @author cfs
 *
 */
public class ScxmlTransitionTypeRule extends AbstractSCXMLImporterRule implements IRule {

	private class Refinement {
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
		int refinementLevel = getRefinementLevel(sourceElement);
		int depth = getRefinementDepth(sourceElement);		
		String parentSmName = stateContainer==null? scxmlContainer.getName() : stateContainer.getId()+"_sm";
		
		for (int i=refinementLevel; i<=depth; i++){
			Refinement ref = new Refinement();
			Machine m = (Machine) Find.translatedElement(generatedElements, null, null, MachinePackage.Literals.MACHINE, getMachineName(scxmlContainer,i));
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
			String guardLabel = "cond";  // may be used in cond guard later
			if (ref.machine.getRefinesNames().size()>0){
				abstractMachine = (Machine) Find.translatedElement(translatedElements, null, null, MachinePackage.Literals.MACHINE,
						ref.machine.getRefinesNames().get(0));
			}else{
				abstractMachine=null;
			}
			for (String eventName: getEventNames(scxmlTransition, ref.machine, translatedElements)){
				Event ev = getOrCreateEvent(ref.machine, translatedElements, eventName);
				if (abstractMachine!=null){
					Event refinedEvent = (Event) Find.element(abstractMachine, abstractMachine, events, MachinePackage.Literals.EVENT, eventName);
					if (refinedEvent!=null && !ev.getRefinesNames().contains(eventName)){
						//ev.getRefines().add(refinedEvent);
						ev.getRefinesNames().add(eventName);
					}
				}
				transition.getElaborates().add(ev);
				guardLabel=guardLabel+"_"+ev.getName();
			}
			//cond -> guard
			String cond = scxmlTransition.getCond();
			if (cond!=null && cond.length()>0) {
				Guard guard = (Guard) Make.guard(guardLabel, false, Strings.COND_GUARD(cond), "");
				transition.getGuards().add(guard);
			}
		}
		return Collections.emptyList();
	}
	
}
