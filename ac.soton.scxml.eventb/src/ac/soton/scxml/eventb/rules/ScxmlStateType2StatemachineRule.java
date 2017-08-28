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
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;

import ac.soton.emf.translator.TranslationDescriptor;
import ac.soton.emf.translator.configuration.IRule;
import ac.soton.emf.translator.utils.Find;
import ac.soton.eventb.statemachines.Initial;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachineOwner;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.scxml.eventb.utils.Make;
import ac.soton.scxml.eventb.utils.Utils;

public class ScxmlStateType2StatemachineRule extends AbstractSCXMLImporterRule implements IRule {


	ScxmlScxmlType scxmlContainer=null;
	ScxmlStateType stateContainer=null;
	List<StatemachineOwner> smOwners = new ArrayList<StatemachineOwner>();

	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		stateContainer = (ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, sourceElement.eContainer());
		return scxmlContainer!=null && !((ScxmlStateType)sourceElement).getState().isEmpty();
	}
	
	/**
	 * if the scxml state is contained in a parallel it does not generate a iUML-B state but it probably does generate a nested statemachine (if it has child states)
	 * 	therefore we need to check that the corresponding parent statemachine owner (machine or state) has already been generated
	 * otherwise (i.e. the scxml state is contained in another state or in a top level scxml element) then it will be used to generate an iUML-B state
	 *   therefore we need to check that the corresponding parent statemachine has already been generated.
	 * 
	 */
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> translatedElements) throws Exception  {
		smOwners.clear();
		int refinementLevel = Utils.getRefinementLevel(sourceElement);
		int depth = getRefinementDepth(sourceElement);
		String smOwnerName = null;
		if (sourceElement.eContainer().eClass() ==ScxmlPackage.Literals.SCXML_PARALLEL_TYPE){
			smOwnerName = stateContainer==null? scxmlContainer.getName() : stateContainer.getId();
		}else{
			smOwnerName = ((ScxmlStateType)sourceElement).getId();
		}
		for (int i=refinementLevel; i<=depth; i++){
			Machine m = (Machine) Find.translatedElement(translatedElements, null, null, MachinePackage.Literals.MACHINE, Utils.getMachineName(scxmlContainer,i));
			StatemachineOwner smo = (StatemachineOwner) Find.element(m, null, null, StatemachinesPackage.Literals.STATEMACHINE_OWNER, smOwnerName);
			if (smo==null) 
				return false;
			smOwners.add(smo);				
		}
		return true;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {

		ScxmlStateType scxmlState = (ScxmlStateType)sourceElement;
		
		String nestedSmName = scxmlState.getId()+"_sm";
		Statemachine statemachine = null;
		for (StatemachineOwner owner : smOwners){
			if (statemachine==null){
				statemachine = (Statemachine) Make.statemachine(nestedSmName, tkind, "");
				if (scxmlState.getInitial()!=null){
					Initial initialState = (Initial) Make.initialState(nestedSmName+"_initialState");
					statemachine.getNodes().add(initialState);
				}
			}else{
				statemachine = (Statemachine) Utils.refine(scxmlContainer, statemachine);
			}
			owner.getStatemachines().add(statemachine);
		}
		return Collections.emptyList();
	}
	
}
