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
import org.eclipse.sirius.tests.sample.scxml.ScxmlAssignType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlOnexitType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;

import ac.soton.emf.translator.TranslationDescriptor;
import ac.soton.emf.translator.configuration.IRule;
import ac.soton.emf.translator.utils.Find;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.scxml.eventb.strings.Strings;
import ac.soton.scxml.eventb.utils.Make;
import ac.soton.scxml.eventb.utils.Utils;

public class ScxmlOnexitTypeRule extends AbstractSCXMLImporterRule implements IRule {

	ScxmlStateType stateContainer=null;
	private List<RefinementLevelDescriptor> refinements = new ArrayList<RefinementLevelDescriptor>();
	private class RefinementLevelDescriptor {
		private int level = -1;
		private State state = null;	
	}
	
	public boolean enabled(final EObject sourceElement) throws Exception  {
		stateContainer = (ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, sourceElement.eContainer());
		return stateContainer!=null;
	}
	
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> generatedElements) throws Exception  {
		ScxmlScxmlType scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		refinements.clear();
		int refinementLevel = Utils.getRefinementLevel(sourceElement);
		int depth = Utils.getRefinementDepth(sourceElement);
		for (int i=refinementLevel; i<=depth; i++){
			Machine m = (Machine) Find.translatedElement(generatedElements, null, null, MachinePackage.Literals.MACHINE, Utils.getMachineName(scxmlContainer,i));
			State st =  (State) Find.element(m, null, nodes, StatemachinesPackage.Literals.STATE, stateContainer.getId());
			if (st==null) return false;
			
			RefinementLevelDescriptor ref = new RefinementLevelDescriptor();
			ref.level = i;
			ref.state = st;
			refinements.add(ref);
		}
		return true;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {
		for (RefinementLevelDescriptor ref : refinements){ 
			int i=0;
			for (ScxmlAssignType assign : ((ScxmlOnexitType)sourceElement).getAssign()){
				if(Utils.getRefinementLevel(sourceElement) <= ref.level){
					Action action = (Action) Make.action(stateContainer.getId()+"_onexit_"+i, Strings.ASSIGN_ACTION(assign));
					ref.state.getExitActions().add(action);
					i++;
				}
			}	
		}
		return Collections.emptyList();
	}
	
}
