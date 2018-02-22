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

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;

import ac.soton.emf.translator.configuration.IRule;
import ac.soton.emf.translator.eventb.rules.AbstractEventBGeneratorRule;
import ac.soton.emf.translator.utils.Find;
import ac.soton.eventb.emf.core.extension.coreextension.CoreextensionPackage;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.TranslationKind;
import ac.soton.scxml.eventb.utils.IumlbScxmlAdapter;


/**
 * a basis for the scxml to statemachines imported rules
 * 
 * @author cfs
 *
 */
public abstract class AbstractSCXMLImporterRule extends AbstractEventBGeneratorRule implements IRule {

	protected static final EReference nodes = StatemachinesPackage.Literals.STATEMACHINE__NODES;
	protected static final EReference statemachines = StatemachinesPackage.Literals.STATEMACHINE_OWNER__STATEMACHINES;
	protected static final EReference transitions = StatemachinesPackage.Literals.STATEMACHINE__TRANSITIONS;
	protected static final EReference entryActions = StatemachinesPackage.Literals.STATE__ENTRY_ACTIONS;
	protected static final EReference exitActions = StatemachinesPackage.Literals.STATE__EXIT_ACTIONS;
	protected static final EReference elaborates = CoreextensionPackage.Literals.EVENT_BEVENT_GROUP__ELABORATES;
	protected static final EReference eventGroupGuards = CoreextensionPackage.Literals.EVENT_BEVENT_GROUP__GUARDS;
	protected static final EReference stateInvariants = StatemachinesPackage.Literals.STATE__INVARIANTS;
	
	protected static final TranslationKind tkind = TranslationKind.MULTIVAR;

	
	/**
	 * This finds the refinement depth required by the Scxml model containing the given Scxml element
	 * @param scxml element
	 * @return integer representing the number of refinements needed
	 */
	protected int getRefinementDepth(EObject scxmlElement) {
		Integer depth = (Integer) storage.fetch("depth");
		if (depth==null) {
			IumlbScxmlAdapter adapter = new IumlbScxmlAdapter(null);
			depth = 0;
			ScxmlScxmlType scxml = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, scxmlElement);
			List<EObject> eObjects = Find.eAllContents(scxml, EcorePackage.Literals.EOBJECT);
			for (EObject eObject : eObjects){
				int ref = (adapter.adapt(eObject)).getBasicRefinementLevel();
	
				//int ref = new IumlbScxmlAdapter(eObject).getRefinementLevel();
				depth = ref>depth? ref : depth;
			}
			storage.stash("depth",depth);
		}
		return depth;
	}

}
