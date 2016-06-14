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
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;

import ac.soton.emf.translator.TranslationDescriptor;
import ac.soton.emf.translator.configuration.IRule;
import ac.soton.eventb.emf.core.extension.navigator.refiner.AbstractElementRefiner;
import ac.soton.eventb.emf.core.extension.navigator.refiner.ElementRefinerRegistry;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.iumlb.scxml.importer.strings.Strings;
import ac.soton.iumlb.scxml.importer.utils.IumlbScxmlAdapter;
import ac.soton.iumlb.scxml.importer.utils.Make;
import ac.soton.iumlb.scxml.importer.utils.Utils;

public class ScxmlScxmlTypeRule extends AbstractSCXMLImporterRule implements IRule {
			
	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {
		int depth = Utils.getRefinementDepth(sourceElement);
		
		ScxmlScxmlType scxml = (ScxmlScxmlType)sourceElement;
		List<TranslationDescriptor> ret = new ArrayList<TranslationDescriptor>();
		String fileName = scxml.eResource().getURI().toPlatformString(true);
		String statechartName = scxml.getName()+"_sm";
		Project project = Utils.findProject(sourceElement);
		Machine machine=null;
		for (int i=0; i<=depth; i++){
			if (i==0){
				machine =  (Machine) Make.machine(Utils.getMachineName(scxml,0), "(generated from SCXML file: "+fileName+")");
				ret.add(Make.descriptor(project, components, machine ,1));
				
				Event initialisation = (Event) Make.event("INITIALISATION");
				machine.getEvents().add(initialisation);

				Statemachine statemachine = (Statemachine) Make.statemachine(statechartName, tkind, "");
				machine.getExtensions().add(statemachine);
			}else{
				machine = refine (scxml, machine, Utils.getMachineName(scxml,i)); //statechartName+"_"+i);
				ret.add(Make.descriptor(project, components, machine ,1));
			}
			
			List<IumlbScxmlAdapter> invs = new IumlbScxmlAdapter(scxml).getinvariants();
			for (IumlbScxmlAdapter inv : invs){
				int refLevel = inv.getBasicRefinementLevel();
				if (refLevel==-1) refLevel = 0;
				if (refLevel==i){
					String name = (String)inv.getAnyAttributeValue("name");
					String derived = (String)inv.getAnyAttributeValue("derived");
					String predicate = (String)inv.getAnyAttributeValue("predicate");
					String comment = (String)inv.getAnyAttributeValue("comment");
					Invariant invariant =  (Invariant) Make.invariant(name,Boolean.parseBoolean(derived),Strings.INV_PREDICATE(predicate),comment); 
					machine.getInvariants().add(invariant);
				}
			}
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
