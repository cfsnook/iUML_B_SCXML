package ac.soton.iumlb.scxml.importer.rules;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.emf.diagrams.importExport.GenerationDescriptor;
import ac.soton.eventb.emf.diagrams.importExport.IRule;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.emf.diagrams.importExport.utils.Find;
import ac.soton.iumlb.scxml.importer.utils.Make;


public class ScxmlScxmlTypeRule extends AbstractSCXMLImporterRule implements IRule {
			
	@Override
	public List<GenerationDescriptor> fire(EObject sourceElement, List<GenerationDescriptor> generatedElements) throws Exception {
		
		ScxmlScxmlType scxml = (ScxmlScxmlType)sourceElement;
		List<GenerationDescriptor> ret = new ArrayList<GenerationDescriptor>();
		String fileName = scxml.eResource().getURI().toPlatformString(true);
		String statechartName = scxml.getName();
		
		Machine machine =  (Machine) Make.machine(statechartName, "(generated from SCXML file: "+fileName+")");
		ret.add(Make.descriptor(Find.project(sourceElement), components, machine ,1));
		
		Event initialisation = (Event) Make.event("INITIALISATION");
		machine.getEvents().add(initialisation);
		//ret.add(Make.descriptor(machine, events, initialisation ,1));
		
		Statemachine statemachine = (Statemachine) Make.statemachine(statechartName, tkind, "");
		machine.getExtensions().add(statemachine);
		//ret.add(Make.descriptor(machine, extensions, statemachine, 1));

		return ret;
	}

}
