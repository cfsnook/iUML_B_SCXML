package ac.soton.iumlb.scxml.importer.rules;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.tests.sample.scxml.ScxmlDataType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlDatamodelType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;
import org.eventb.emf.core.machine.Variable;

import ac.soton.eventb.emf.diagrams.importExport.IRule;
import ac.soton.eventb.emf.diagrams.importExport.TranslationDescriptor;
import ac.soton.eventb.emf.diagrams.importExport.utils.Find;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.iumlb.scxml.importer.strings.Strings;
import ac.soton.iumlb.scxml.importer.utils.Make;

public class ScxmlDataTypeRule extends AbstractSCXMLImporterRule implements IRule {

	private ScxmlScxmlType scxmlContainer=null;
	private Machine machine = null;
	private Event initialisation = null;
	private State state;
	
	@Override
	public boolean enabled(final EObject sourceElement) throws Exception  {
		scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, sourceElement);
		return scxmlContainer!=null;
	}
	
	@Override
	public boolean dependenciesOK(EObject sourceElement, final List<TranslationDescriptor> generatedElements) throws Exception  {
		ScxmlDataType scxml = (ScxmlDataType)sourceElement;
		machine = (Machine) Find.translatedElement(generatedElements, null, null, MachinePackage.Literals.MACHINE, scxmlContainer.getName());
		initialisation = (Event) Find.translatedElement(generatedElements, machine, events, MachinePackage.Literals.EVENT, "INITIALISATION");
		if (isOwnedByState(scxml)){
			String stateName = ((ScxmlStateType)Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, scxml)).getId();
			state = (State) Find.translatedElement(generatedElements, null, null, StatemachinesPackage.Literals.STATE, stateName);
			if (state==null) return false;
		}
		return machine!=null && initialisation!=null;
	}

	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {
		assert(machine!=null && initialisation!=null) : "Not ready to fire()";
		
		ScxmlDataType scxml = (ScxmlDataType)sourceElement;
		List<TranslationDescriptor> ret = new ArrayList<TranslationDescriptor>();
		String vname = Strings.LOCATION(scxml);
		if (isPredicate(scxml.getExpr())){
			if (isOwnedByState(scxml)){
				Invariant invariant =  (Invariant) Make.invariant(vname, Strings.INV_PREDICATE(null, scxml), "");
				ret.add(Make.descriptor(state, stateInvariants, invariant ,1));				
			}else{
				Invariant invariant =  (Invariant) Make.invariant(vname, Strings.INV_PREDICATE(null, scxml), "");
				ret.add(Make.descriptor(machine, invariants, invariant ,1));		
			}
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
	
	private boolean isOwnedByState(EObject scxml){
		EObject owner = scxml.eContainer();
		if (owner instanceof ScxmlStateType) return true;
		if (owner instanceof ScxmlDatamodelType) return isOwnedByState(owner);
		return false;
	}
	
	
}
