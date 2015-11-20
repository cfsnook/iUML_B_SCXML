package ac.soton.iumlb.scxml.importer.rules;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.tests.sample.scxml.ScxmlDataType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;
import org.eventb.emf.core.machine.Variable;

import ac.soton.eventb.emf.diagrams.importExport.AbstractRule;
import ac.soton.eventb.emf.diagrams.importExport.TranslationDescriptor;
import ac.soton.eventb.emf.diagrams.importExport.IRule;
import ac.soton.iumlb.scxml.importer.strings.Strings;
import ac.soton.eventb.emf.diagrams.importExport.utils.Find;
import ac.soton.iumlb.scxml.importer.utils.Make;

public class ScxmlDataTypeRule extends AbstractRule implements IRule {

	ScxmlScxmlType scxmlContainer=null;
	Machine machine = null;
	Event initialisation = null;
	
	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		return scxmlContainer!=null;
	}
	
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> generatedElements) throws Exception  {
		machine = (Machine) Find.translatedElement(generatedElements, null, null, MachinePackage.Literals.MACHINE, scxmlContainer.getName());
		initialisation = (Event) Find.translatedElement(generatedElements, machine, events, MachinePackage.Literals.EVENT, "INITIALISATION");
		return machine!=null && initialisation!=null;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {
		assert(machine!=null && initialisation!=null) : "Not ready to fire()";
		
		ScxmlDataType scxml = (ScxmlDataType)sourceElement;
		List<TranslationDescriptor> ret = new ArrayList<TranslationDescriptor>();
		String vname = Strings.LOCATION(scxml);
		
		if (isPredicate(scxml.getExpr())){
			String anticedent = null;
			if (scxml.eContainer() instanceof ScxmlStateType){
				String stateName = ((ScxmlStateType)scxml.eContainer()).getId();
				anticedent = "\u2200this_"+stateName+"\u00b7this_"+stateName+"\u2208"+stateName+"\u21d2";
			}
			Invariant invariant =  (Invariant) Make.invariant(vname, Strings.INV_PREDICATE(anticedent, scxml), "");
			ret.add(Make.descriptor(machine, invariants, invariant ,1));			
		}else{
			Variable variable =  (Variable) Make.variable(vname, "");
			ret.add(Make.descriptor(machine, variables, variable ,1));
			Invariant invariant =  (Invariant) Make.invariant(vname+"_type", Strings.TYPE_PREDICATE(scxml), "");
			ret.add(Make.descriptor(machine, invariants, invariant ,1));
			Action initAction =  (Action) Make.action(vname+"_init", Strings.INIT_ACTION(scxml), "");
			ret.add(Make.descriptor(initialisation, actions, initAction ,1));
		}
		return ret;
	}

	private boolean isPredicate(String expr) {
		return expr.contains("=") || expr.contains(">") || expr.contains("<") ;
	}
	
}
