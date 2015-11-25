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
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.tests.sample.scxml.ScxmlAssignType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlOnexitType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eventb.emf.core.machine.Action;

import ac.soton.eventb.emf.diagrams.importExport.TranslationDescriptor;
import ac.soton.eventb.emf.diagrams.importExport.IRule;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.iumlb.scxml.importer.strings.Strings;
import ac.soton.eventb.emf.diagrams.importExport.utils.Find;
import ac.soton.iumlb.scxml.importer.utils.Make;

public class ScxmlOnexitTypeRule extends AbstractSCXMLImporterRule implements IRule {


	ScxmlStateType stateContainer=null;
	State generatedParentState = null;
	
	public boolean enabled(final EObject sourceElement) throws Exception  {
		stateContainer = (ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, sourceElement.eContainer());
		return stateContainer!=null;
	}
	
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> generatedElements) throws Exception  {
		generatedParentState = (State) Find.translatedElement(generatedElements, null, nodes, StatemachinesPackage.Literals.STATE, stateContainer.getId());
		return generatedParentState!=null;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {
		assert(generatedParentState!=null) : "Not ready to fire()";
		
		ScxmlOnexitType scxmlOnexit = (ScxmlOnexitType)sourceElement;
		List<TranslationDescriptor> ret = new ArrayList<TranslationDescriptor>();
		
		int i=0;
		for (ScxmlAssignType assign : scxmlOnexit.getAssign()){
			Action action = (Action) Make.action(stateContainer.getId()+"_onexit_"+i, Strings.ASSIGN_ACTION(assign));
			generatedParentState.getExitActions().add(action);
			//ret.add(Make.descriptor(generatedParentState, exitActions, action ,1));
			i++;
		}
		return ret;
	}
	
}
