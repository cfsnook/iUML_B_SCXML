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
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;

import ac.soton.emf.translator.TranslationDescriptor;
import ac.soton.emf.translator.configuration.IRule;
import ac.soton.emf.translator.utils.Find;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.scxml.eventb.strings.Strings;
import ac.soton.scxml.eventb.utils.IumlbScxmlAdapter;
import ac.soton.scxml.eventb.utils.Make;
import ac.soton.scxml.eventb.utils.Utils;

public class ScxmlStateType2StateRule extends AbstractSCXMLImporterRule implements IRule {


	ScxmlScxmlType scxmlContainer=null;
	ScxmlStateType stateContainer=null;

	private List<RefinementLevelDescriptor> refinements = new ArrayList<RefinementLevelDescriptor>();
	private class RefinementLevelDescriptor {
		private int level = -1;
		private Statemachine statemachine = null;	
	}
	/**
	 * if the scxml state is contained in a parallel it does not generate a iUML-B state, otherwise, if
	 * the scxml state is contained in another state or in a top level scxml element then it will be used to generate an iUML-B state
	 * 
	 */
	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		stateContainer = (ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, sourceElement.eContainer());
		return scxmlContainer!=null && sourceElement.eContainer().eClass() !=ScxmlPackage.Literals.SCXML_PARALLEL_TYPE;
	}
	
	/**
	 * 
	 *  check that the corresponding parent statemachines have already been generated.
	 * 
	 */
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> translatedElements) throws Exception  {
		refinements.clear();
		int refinementLevel = Utils.getRefinementLevel(sourceElement.eContainer()); //refinement level is from the parent, not this ScxmlState. 
		int depth = getRefinementDepth(sourceElement);
		for (int i=refinementLevel; i<=depth; i++){
			Machine m = (Machine) Find.translatedElement(translatedElements, null, null, MachinePackage.Literals.MACHINE, Utils.getMachineName(scxmlContainer,i));
			String parentSmName = (stateContainer==null? scxmlContainer.getName() : stateContainer.getId())+"_sm";
			Statemachine psm = (Statemachine) Find.element(m, null, null, StatemachinesPackage.Literals.STATEMACHINE, parentSmName);
			if (psm==null) return false;
			
			RefinementLevelDescriptor ref = new RefinementLevelDescriptor();
			ref.level = i;
			ref.statemachine = psm;
			refinements.add(ref);
		}
		return true;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {

		ScxmlStateType scxmlState = (ScxmlStateType)sourceElement;
		int baseLevel = refinements.get(0).level;
		// states translate into iUML-B states.. 
		State state = null;
		for (RefinementLevelDescriptor ref : refinements){
			Statemachine psm = ref.statemachine;
			if (state==null){
				state = (State)Make.state(scxmlState.getId(), "");
			}else{
				state = (State) Utils.refine(scxmlContainer, state);
			}
			psm.getNodes().add(state);
			
			List<IumlbScxmlAdapter> invs = new IumlbScxmlAdapter(scxmlState).getinvariants();
			for (IumlbScxmlAdapter inv : invs){
				int refLevel = inv.getBasicRefinementLevel();
				if (refLevel==-1) refLevel = baseLevel;
				if (refLevel==ref.level){
					String name = (String)inv.getAnyAttributeValue("name");
					String derived = (String)inv.getAnyAttributeValue("derived");
					String predicate = (String)inv.getAnyAttributeValue("predicate");
					String comment = (String)inv.getAnyAttributeValue("comment");
					Invariant invariant =  (Invariant) Make.invariant(name,Boolean.parseBoolean(derived),Strings.INV_PREDICATE(predicate),comment); 
					state.getInvariants().add(invariant);
				}
			}
		}
		
		return Collections.emptyList();
	}
	
}
