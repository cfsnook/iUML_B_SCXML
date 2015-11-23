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
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;

import ac.soton.eventb.emf.diagrams.importExport.TranslationDescriptor;
import ac.soton.eventb.emf.diagrams.importExport.IRule;
import ac.soton.eventb.statemachines.AbstractNode;
import ac.soton.eventb.statemachines.Initial;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.eventb.emf.diagrams.importExport.utils.Find;
import ac.soton.iumlb.scxml.importer.utils.Make;

public class ScxmlInitialAttributeRule extends AbstractSCXMLImporterRule implements IRule {
			
	private ScxmlScxmlType scxmlContainer;
	private Machine machine;
	private Statemachine statemachine;
	private Event initialisation;
	private List<AbstractNode> targets = new ArrayList<AbstractNode>();

	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		scxmlContainer = (ScxmlScxmlType)  sourceElement;
		return scxmlContainer!=null && !scxmlContainer.getInitial().isEmpty() ;
	}
	
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> generatedElements) throws Exception  {
		machine = (Machine) Find.translatedElement(generatedElements, null, null, MachinePackage.Literals.MACHINE, scxmlContainer.getName());
		statemachine = (Statemachine) Find.translatedElement(generatedElements, null, null, StatemachinesPackage.Literals.STATEMACHINE, scxmlContainer.getName());
		initialisation = (Event) Find.translatedElement(generatedElements, machine, events, "INITIALISATION");
		targets.clear();
		for (String initialTarget : scxmlContainer.getInitial()){
			AbstractNode target = (AbstractNode) Find.translatedElement(generatedElements, statemachine, nodes, StatemachinesPackage.Literals.ABSTRACT_NODE, initialTarget);
			if (target==null){
				return false;
			}
			targets.add(target);
		}
		return initialisation!=null && statemachine!= null;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {

		List<TranslationDescriptor> ret = new ArrayList<TranslationDescriptor>();

		Initial initialState = (Initial) Make.initialState(scxmlContainer.getName()+"_initialState");
		statemachine.getNodes().add(initialState);
		//ret.add(Make.descriptor(statemachine, nodes, initialState, 1));
		
		for (AbstractNode target : targets){
			Transition tr = (Transition) Make.transition(initialState, target, "");
			tr.getElaborates().add(initialisation);
			statemachine.getTransitions().add(tr);
		}
		return ret;
	}

}
