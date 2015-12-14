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
import org.eclipse.sirius.tests.sample.scxml.ScxmlDataType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlDatamodelType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;
import org.eventb.emf.core.machine.Variable;

import ac.soton.eventb.emf.diagrams.importExport.IRule;
import ac.soton.eventb.emf.diagrams.importExport.TranslationDescriptor;
import ac.soton.eventb.emf.diagrams.importExport.utils.Find;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.iumlb.scxml.importer.strings.Strings;
import ac.soton.iumlb.scxml.importer.utils.Make;

public class ScxmlDataTypeRule extends AbstractSCXMLImporterRule implements IRule {

	private ScxmlScxmlType scxmlContainer=null;
	private class Refinement {
		private Machine machine = null;
		private Event initialisation = null;
		private State state;	
	}
	private List<Refinement> refinements = new ArrayList<Refinement>();
	
	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		if ("Refinement".equals(((ScxmlDataType)sourceElement).getId())){
			return false;
		}
		scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		return scxmlContainer!=null;
	}
	
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> generatedElements) throws Exception  {
		ScxmlDataType scxml = (ScxmlDataType)sourceElement;
		refinements.clear();
		int refinementLevel = getRefinementLevel(sourceElement);
		int depth = getRefinementDepth(sourceElement);		
		
		for (int i=refinementLevel; i<=depth; i++){
			Refinement ref = new Refinement();
			Machine m = (Machine) Find.translatedElement(generatedElements, null, null, MachinePackage.Literals.MACHINE, getMachineName(scxmlContainer,i));
			ref.machine = m;
			if (ref.machine == null) 
				return false;
			ref.initialisation = (Event) Find.translatedElement(generatedElements, m, events, MachinePackage.Literals.EVENT, "INITIALISATION");
			if (ref.initialisation == null) 
				return false;
			if (isOwnedByState(scxml) && i==refinementLevel){
				String stateName = ((ScxmlStateType)Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, scxml)).getId();
				ref.state = (State) Find.translatedElement(generatedElements, null, null, StatemachinesPackage.Literals.STATE, stateName);
				if (ref.state==null) 
					return false;
			}
			refinements.add(ref);
		}
		return true;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {
		ScxmlDataType scxml = (ScxmlDataType)sourceElement;
		String vname = Strings.LOCATION(scxml);
		boolean done = false; //flag used to ensure we only create invariants once
		for (Refinement ref : refinements){
			if (isPredicate(scxml.getExpr())){
				if (done == false){
					Invariant invariant =  (Invariant) Make.invariant(vname, Strings.INV_PREDICATE(null, scxml), "");
					if (ref.state == null){
						ref.machine.getInvariants().add(invariant);
					}else{
						ref.state.getInvariants().add(invariant);
					}
					done=true;
				}
			}else{
				Variable variable =  (Variable) Make.variable(vname, "");
				ref.machine.getVariables().add(variable);
				if (done == false){
					Invariant invariant =  (Invariant) Make.invariant(vname+"_type", Strings.TYPE_PREDICATE(scxml), "");
					ref.machine.getInvariants().add(invariant);
					done=true;
				}
				Action initAction =  (Action) Make.action(vname+"_init", Strings.INIT_ACTION(scxml), "");
				ref.initialisation.getActions().add(initAction);
			}
		}
		return Collections.emptyList();
	}

	private boolean isPredicate(String expr) {
		return expr.contains("=") || expr.contains(">") || expr.contains("<") ;
	}
	
	private boolean isOwnedByState(EObject scxml){
		EObject owner = scxml.eContainer();
		if (owner instanceof ScxmlStateType) return true;
		if (owner instanceof ScxmlDatamodelType) return isOwnedByState(owner);
		return false;
	}
	
}
