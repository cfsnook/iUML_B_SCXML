/*******************************************************************************
 *  Copyright (c) 2017 University of Southampton.
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
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eventb.emf.core.Project;
import org.eventb.emf.core.context.Axiom;
import org.eventb.emf.core.context.CarrierSet;
import org.eventb.emf.core.context.Constant;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.Variable;

import ac.soton.emf.translator.TranslationDescriptor;
import ac.soton.emf.translator.configuration.IRule;
import ac.soton.eventb.emf.core.extension.navigator.refiner.AbstractElementRefiner;
import ac.soton.eventb.emf.core.extension.navigator.refiner.ElementRefinerRegistry;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.scxml.eventb.strings.Strings;
import ac.soton.scxml.eventb.utils.IumlbScxmlAdapter;
import ac.soton.scxml.eventb.utils.Make;
import ac.soton.scxml.eventb.utils.Utils;

public class ScxmlScxmlTypeRule extends AbstractSCXMLImporterRule implements IRule {
			
	@Override
	public List<TranslationDescriptor> fire(EObject sourceElement, List<TranslationDescriptor> generatedElements) throws Exception {
		int depth = Utils.getRefinementDepth(sourceElement);
		
		ScxmlScxmlType scxml = (ScxmlScxmlType)sourceElement;
		List<TranslationDescriptor> ret = new ArrayList<TranslationDescriptor>();
		String fileName = scxml.eResource().getURI().toPlatformString(true);
		String machineComment ="(generated from SCXML file: "+fileName+")";
		String statechartName = scxml.getName()+"_sm";
		Project project = Utils.findProject(sourceElement);
		
		//get the basis Context
		Context context = getBasisContext();
		ret.add(Make.descriptor(project, components, context ,1));
		
		//get the basis Machine
		Machine machine=null;
		machine =  getBasisMachine(context);
		ret.add(Make.descriptor(project, components, machine ,1));
		
		//create the refinement chain of machines
		for (int i=0; i<=depth; i++){
			machine = refine (scxml, machine, Utils.getMachineName(scxml,i), machineComment);
			ret.add(Make.descriptor(project, components, machine ,1));
			if (i==0){
				Statemachine statemachine = (Statemachine) Make.statemachine(statechartName, tkind, "");
				machine.getExtensions().add(statemachine);
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
	private Machine refine(EObject sourceElement, Machine machine, String refineName, String comment) {
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
		refinement.setComment(comment);
		return refinement;
	}
	
	private static final  String basisContextName = "basis";	
	private static final  String triggerSetName = "SCXML_TRIGGER";
	private static final  String internalTriggersName = "SCXML_FutureInternalTrigger";
	private static final  String externalTriggersName = "SCXML_FutureExternalTrigger";
	private static final  String triggerPartitionAxiomName = "axm1";
	private static final  String triggerPartitionAxiomPredicate = "partition("+triggerSetName+","+internalTriggersName+","+externalTriggersName+")";	

	private static final  String basisMachineName = "basis";
	private static final  String internalQueueName = "SCXML_iq";
	private static final  String externalQueueName = "SCXML_eq";
	private static final  String completionFlagName = "SCXML_uc";
	private static final  String internalQueueTypeName = "typeof_"+internalQueueName;
	private static final  String internalQueueTypePredicate = internalQueueName+" \u2286 "+internalTriggersName;
	private static final  String externalQueueTypeName = "typeof_"+externalQueueName;
	private static final  String externalQueueTypePredicate = externalQueueName+" \u2286 "+externalTriggersName;
	private static final  String queueDisjunctionName = "disjointQueues";
	private static final  String queueDisjunctionPredicate = internalQueueName+" \u2229 "+externalQueueName+"= \u2205";
	private static final  String completionFlagTypeName = "typeof_"+completionFlagName;
	private static final  String completionFlagTypePredicate = completionFlagName+" \u2208 BOOL";
	
	private static final  String initInternalQName = "init_"+internalQueueName;
	private static final  String initInternalQAction = internalQueueName+" \u2254 \u2205";
	private static final  String initExternalQName = "init_"+externalQueueName;
	private static final  String initExternalQAction = externalQueueName+" \u2254 \u2205";	
	private static final  String initCompletionFlagName = "init_"+completionFlagName;
	private static final  String initCompletionFlagAction = completionFlagName+" \u2254 TRUE";
	
	/**
	 * @return
	 */
	private Context getBasisContext() {
		Context basis = (Context) Make.context(basisContextName, "(generated for SCXML)");
		CarrierSet triggerSet = (CarrierSet) Make.set(triggerSetName, "all possible triggers");
		basis.getSets().add(triggerSet);
		Constant const1 = (Constant) Make.constant(internalTriggersName, "all possible internal triggers");
		basis.getConstants().add(const1);
		Constant const2 = (Constant) Make.constant(externalTriggersName, "all possible external triggers");
		basis.getConstants().add(const2);
		Axiom ax = (Axiom) Make.axiom(triggerPartitionAxiomName, triggerPartitionAxiomPredicate, "");
		basis.getAxioms().add(ax);
		return basis;
	}
	
	/**
	 * @return
	 */
	private Machine getBasisMachine(Context basisContext) {
		Machine basis = (Machine) Make.machine(basisMachineName, "(generated for SCXML)");
		basis.getSeesNames().add(basisContextName);
		//basis.getSees().add(basisContext);
		
		Variable v1 = (Variable) Make.variable(internalQueueName, "internal trigger queue");
		basis.getVariables().add(v1);
		Variable v2 = (Variable) Make.variable(externalQueueName, "external trigger queue");
		basis.getVariables().add(v2);
		Variable v3 = (Variable) Make.variable(completionFlagName, "run to completion flag");
		basis.getVariables().add(v3);

		Invariant i1 = (Invariant) Make.invariant(internalQueueTypeName, internalQueueTypePredicate, "internal trigger queue");
		basis.getInvariants().add(i1);
		Invariant i2 = (Invariant) Make.invariant(externalQueueTypeName, externalQueueTypePredicate, "external trigger queue");
		basis.getInvariants().add(i2);
		Invariant i3 = (Invariant) Make.invariant(queueDisjunctionName, queueDisjunctionPredicate, "queues are disjoint");
		basis.getInvariants().add(i3);
		Invariant i4 = (Invariant) Make.invariant(completionFlagTypeName, completionFlagTypePredicate, "completion flag");
		basis.getInvariants().add(i4);
		
		Event initialisation = (Event) Make.event("INITIALISATION");
		basis.getEvents().add(initialisation);
		
		Action a1 = (Action) Make.action(initInternalQName, initInternalQAction, "internal Q is initially empty");
		initialisation.getActions().add(a1);
		Action a2 = (Action) Make.action(initExternalQName, initExternalQAction, "external Q is initially empty");
		initialisation.getActions().add(a2);		
		Action a3 = (Action) Make.action(initCompletionFlagName, initCompletionFlagAction, "completion is initially TRUE");
		initialisation.getActions().add(a3);
		
		Event e1 = (Event) Make.event("SCXML_futureInternalTrigger");
		basis.getEvents().add(e1);

		Event e2 = (Event) Make.event("SCXML_futureExternalTrigger");
		basis.getEvents().add(e2);
		
		Event e3 = (Event) Make.event("SCXML_futureInternalTransitionSet");
		basis.getEvents().add(e3);
		
		Event e4 = (Event) Make.event("SCXML_futureExternalTransitionSet");
		basis.getEvents().add(e4);
		
		Event e5 = (Event) Make.event("SCXML_futureUntriggeredTransitionSet");
		basis.getEvents().add(e5);
		
		Event e6 = (Event) Make.event("SCXML_Completion");
		basis.getEvents().add(e6);
		
		
		return basis;
	}
	


}
