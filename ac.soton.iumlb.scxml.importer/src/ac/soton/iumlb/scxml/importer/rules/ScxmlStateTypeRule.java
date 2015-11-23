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
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;

import ac.soton.eventb.emf.diagrams.importExport.TranslationDescriptor;
import ac.soton.eventb.emf.diagrams.importExport.IRule;
import ac.soton.eventb.statemachines.Initial;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachineOwner;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.emf.diagrams.importExport.utils.Find;
import ac.soton.iumlb.scxml.importer.utils.Make;

public class ScxmlStateTypeRule extends AbstractSCXMLImporterRule implements IRule {


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
	 * if the scxml state is contained in a parallel it does not generate a UML-B state but it probably does generate a iuml-b nested statemachine (if it has child states)
	 * 	therefore we need to check that the corresponding parent statemachine owner (machine or state) has already been generated
	 * otherwise (i.e. the scxml state is contained in another state or in a top level scxml element) then it will be used to generate an iUML-B state
	 *   therefore we need to check that the corresponding parent statemachine has already been generated.
	 * 
	 */
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> generatedElements) throws Exception  {
		((ScxmlStateType) sourceElement).getId();
		if (sourceElement.eContainer().eClass() ==ScxmlPackage.Literals.SCXML_PARALLEL_TYPE){
			String smOwnerName = stateContainer==null? scxmlContainer.getName() : stateContainer.getId();
			smOwner = (StatemachineOwner) Find.translatedElement(generatedElements, null, null, StatemachinesPackage.Literals.STATEMACHINE_OWNER, smOwnerName);
			return smOwner!=null;			
		}else{
			String parentSmName = stateContainer==null? scxmlContainer.getName() : stateContainer.getId()+"_sm";
			parentSm = (Statemachine) Find.translatedElement(generatedElements, null, null, StatemachinesPackage.Literals.STATEMACHINE, parentSmName);
			return parentSm!=null;
		}
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {
		assert(parentSm!=null || smOwner!=null) : "Not ready to fire()";
		
		ScxmlStateType scxmlState = (ScxmlStateType)sourceElement;
		List<TranslationDescriptor> ret = new ArrayList<TranslationDescriptor>();
		
		// states translate into iUML-B states.. when not contained in a parallel 
		if (!(scxmlState.eContainer().eClass() ==ScxmlPackage.Literals.SCXML_PARALLEL_TYPE)){
			State state = (State)Make.state(scxmlState.getId(), "");
			ret.add(Make.descriptor(parentSm, nodes, state ,1));
			smOwner = state; //in case we also generate a nested statemachine below
		}
		
		if (!scxmlState.getState().isEmpty()){
			String nestedSmName = scxmlState.getId()+"_sm";

			Statemachine statemachine = (Statemachine) Make.statemachine(nestedSmName, tkind, "");
			ret.add(Make.descriptor(smOwner, statemachines, statemachine, 1));

			if (scxmlState.getInitial()!=null){
				Initial initialState = (Initial) Make.initialState(nestedSmName+"_initialState");
				ret.add(Make.descriptor(statemachine, nodes, initialState, 1));
			}

		}
		
		return ret;
	}
	
	
	

}
