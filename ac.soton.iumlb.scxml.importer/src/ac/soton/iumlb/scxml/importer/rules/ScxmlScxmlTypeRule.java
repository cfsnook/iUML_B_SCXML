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
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eventb.emf.core.Project;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;

import ac.soton.emf.translator.IRule;
import ac.soton.emf.translator.TranslationDescriptor;
import ac.soton.eventb.emf.core.extension.navigator.refiner.AbstractElementRefiner;
import ac.soton.eventb.emf.core.extension.navigator.refiner.ElementRefinerRegistry;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.iumlb.scxml.importer.utils.Find;
import ac.soton.iumlb.scxml.importer.utils.Make;

public class ScxmlScxmlTypeRule extends AbstractSCXMLImporterRule implements IRule {
			
	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {
		int depth = getRefinementDepth(sourceElement);
		
		ScxmlScxmlType scxml = (ScxmlScxmlType)sourceElement;
		List<TranslationDescriptor> ret = new ArrayList<TranslationDescriptor>();
		String fileName = scxml.eResource().getURI().toPlatformString(true);
		String statechartName = scxml.getName();
		Project project = Find.project(sourceElement);

		//String abstractMachineName = statechartName + (depth>0? "_0" : "");
		Machine machine =  (Machine) Make.machine(getMachineName(scxml,0), "(generated from SCXML file: "+fileName+")");
		ret.add(Make.descriptor(project, components, machine ,1));
		
		Event initialisation = (Event) Make.event("INITIALISATION");
		machine.getEvents().add(initialisation);
		//ret.add(Make.descriptor(machine, events, initialisation ,1));

		Statemachine statemachine = (Statemachine) Make.statemachine(statechartName, tkind, "");
		machine.getExtensions().add(statemachine);
		//ret.add(Make.descriptor(machine, extensions, statemachine, 1));

		for (int i=1; i<=depth; i++){
			Machine refinement = refine (scxml, machine, statechartName+"_"+i);
			ret.add(Make.descriptor(project, components, refinement ,1));
			machine = refinement;
		}
		
		return ret;
	}

	/**
	 * @param machine
	 * @return
	 */
	private Machine refine(EObject sourceElement, Machine machine, String refineName) {
		URI uri = EcoreUtil.getURI(sourceElement);
		uri = uri.trimFragment().trimSegments(1);
		uri = uri.appendSegment(machine.getName());
		uri = uri.appendFileExtension("bum");
		uri = uri.appendFragment(machine.getReference());
		AbstractElementRefiner refiner = ElementRefinerRegistry.getRegistry().getRefiner(machine);
		//MachineElementRefiner refiner = new MachineElementRefiner();
		Map<EObject,EObject> copy = refiner.refine(uri, machine, null);
		Machine refinement = (Machine) copy.get(machine);
		refinement.setName(refineName);
		return refinement;
	}

}
