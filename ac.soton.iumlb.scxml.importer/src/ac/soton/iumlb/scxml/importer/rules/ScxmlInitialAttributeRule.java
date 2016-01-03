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
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;

import ac.soton.emf.translator.IRule;
import ac.soton.emf.translator.TranslationDescriptor;
import ac.soton.emf.translator.utils.Find;
import ac.soton.eventb.statemachines.AbstractNode;
import ac.soton.eventb.statemachines.Initial;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.iumlb.scxml.importer.utils.Make;

public class ScxmlInitialAttributeRule extends AbstractSCXMLImporterRule implements IRule {
	
	private class Refinement {
		private Statemachine statemachine = null;
		private Event initialisation = null;
		private List<AbstractNode> targets = new ArrayList<AbstractNode>();	
	}
	
	private List<Refinement> refinements = new ArrayList<Refinement>();

	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		ScxmlScxmlType scxmlContainer = (ScxmlScxmlType)  sourceElement;
		return scxmlContainer!=null && !scxmlContainer.getInitial().isEmpty() ;
	}
	
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> generatedElements) throws Exception  {
		ScxmlScxmlType scxmlContainer = (ScxmlScxmlType)  sourceElement;
		refinements.clear();
		int refinementLevel = getRefinementLevel(sourceElement);
		int depth = getRefinementDepth(sourceElement);
		for (int i=refinementLevel; i<=depth; i++){
			Refinement ref = new Refinement();
			Machine m = (Machine) Find.translatedElement(generatedElements, null, null, MachinePackage.Literals.MACHINE, getMachineName(scxmlContainer,i));
			ref.statemachine = (Statemachine) Find.element(m, null, null, StatemachinesPackage.Literals.STATEMACHINE, scxmlContainer.getName()); 
			ref.initialisation= (Event) Find.element(m, m, events, MachinePackage.Literals.EVENT, "INITIALISATION");
			if (ref.statemachine==null || ref.initialisation==null) return false;
			ref.targets.clear();
			for (String initialTarget : scxmlContainer.getInitial()){
				AbstractNode target = (AbstractNode) Find.element(ref.statemachine, ref.statemachine, nodes, StatemachinesPackage.Literals.ABSTRACT_NODE, initialTarget);
				if (target==null){
					return false;
				}
				ref.targets.add(target);
			}
			refinements.add(ref);
		}
		return true;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {
		ScxmlScxmlType scxmlContainer = (ScxmlScxmlType)  sourceElement;

		for (Refinement ref : refinements){
			Initial initialState = (Initial) Make.initialState(scxmlContainer.getName()+"_initialState");
			ref.statemachine.getNodes().add(initialState);			
			
			for (AbstractNode target : ref.targets){
				Transition tr = (Transition) Make.transition(initialState, target, "");
				tr.getElaborates().add(ref.initialisation);
				ref.statemachine.getTransitions().add(tr);
			}			

		}

		return Collections.emptyList(); //ret;
	}

}
