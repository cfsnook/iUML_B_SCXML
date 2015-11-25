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
import org.eclipse.sirius.tests.sample.scxml.ScxmlInitialType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlTransitionType;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Guard;

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


	ScxmlScxmlType scxmlContainer=null;
	AbstractNode source = null;
	AbstractNode target = null;
	Statemachine parentSm = null;
	
	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		return scxmlContainer!=null;
	}
	
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> generatedElements) throws Exception  {
		
		ScxmlStateType stateContainer = (ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, sourceElement.eContainer().eContainer());
		
		String parentSmName = stateContainer==null? scxmlContainer.getName() : stateContainer.getId()+"_sm";
		parentSm = (Statemachine) Find.translatedElement(generatedElements, null, null, StatemachinesPackage.Literals.STATEMACHINE, parentSmName);

		if (parentSm == null) return false;
		
		EObject container = sourceElement.eContainer();
		String sourceStateName = container instanceof ScxmlStateType? ((ScxmlStateType)sourceElement.eContainer()).getId() :
									container instanceof ScxmlInitialType? parentSm.getName()+"_initialState" :
										null;
		source = (AbstractNode) Find.translatedElement(generatedElements, null, null, StatemachinesPackage.Literals.ABSTRACT_NODE, sourceStateName);

		String targetStateName = ((ScxmlTransitionType) sourceElement).getTarget().get(0);		//we only support single target - ignore the rest
		target = (AbstractNode) Find.translatedElement(generatedElements, null, null, StatemachinesPackage.Literals.ABSTRACT_NODE, targetStateName);

		return source!=null && target!=null;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {
		assert(source!=null && target!=null) : "Not ready to fire()";

		ScxmlTransitionType scxmlTransition = ((ScxmlTransitionType) sourceElement);
		List<TranslationDescriptor> ret = new ArrayList<TranslationDescriptor>();
		
		Transition transition = Make.transition(source, target, "");
		
		String guardLabel = "cond";  // may be used in cond guard later
		
		for (String eventName: getEventNames(scxmlTransition,  generatedElements, ret)){
			Event ev = getOrCreateEvent(scxmlContainer,generatedElements, ret, eventName);
			transition.getElaborates().add(ev);
			//ret.add(Make.descriptor(transition, elaborates, ev ,1));
			guardLabel=guardLabel+"_"+ev.getName();
		}
		
		//cond -> guard
		String cond = scxmlTransition.getCond();
		if (cond!=null && cond.length()>0) {
			Guard guard = (Guard) Make.guard(guardLabel, false, Strings.COND_GUARD(cond), "");
			transition.getGuards().add(guard);
			//ret.add(Make.descriptor(transition, eventGroupGuards, guard, 1));
		}
		
		ret.add(Make.descriptor(parentSm, transitions, transition, 1));
		
		return ret;
	}
	
}
