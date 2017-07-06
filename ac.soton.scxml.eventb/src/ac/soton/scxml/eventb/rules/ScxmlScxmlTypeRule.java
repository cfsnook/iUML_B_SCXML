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
import org.eventb.emf.core.machine.Guard;
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.Parameter;
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
	
	
	
	/*********************************************************
	 * The remainder generates the basis context and machine
	 *********************************************************/
	private static final  String basisContextName = "basis";	
	private static final  String triggerSetName = "SCXML_TRIGGER";
	private static final  String externalTriggersName = "SCXML_FutureExternalTrigger";
	private static final  String internalTriggersName = "SCXML_FutureInternalTrigger";
	private static final  String triggerPartitionAxiomName = "axm1";
	private static final  String triggerPartitionAxiomPredicate = "partition("+triggerSetName+","+internalTriggersName+","+externalTriggersName+")";	

	private static final  String basisMachineName = "basis";
	private static final  String externalQueueName = "SCXML_eq";
	private static final  String internalQueueName = "SCXML_iq";
	private static final  String completionFlagName = "SCXML_uc";
	private static final  String externalQueueTypeName = "typeof_"+externalQueueName;
	private static final  String externalQueueTypePredicate = externalQueueName+" \u2286 "+externalTriggersName;
	private static final  String internalQueueTypeName = "typeof_"+internalQueueName;
	private static final  String internalQueueTypePredicate = internalQueueName+" \u2286 "+internalTriggersName;
	private static final  String queueDisjunctionName = "disjointQueues";
	private static final  String queueDisjunctionPredicate = internalQueueName+" \u2229 "+externalQueueName+"= \u2205";
	private static final  String completionFlagTypeName = "typeof_"+completionFlagName;
	private static final  String completionFlagTypePredicate = completionFlagName+" \u2208 BOOL";
	
	private static final  String initExternalQName = "init_"+externalQueueName;
	private static final  String initExternalQAction = externalQueueName+" \u2254 \u2205";	
	private static final  String initInternalQName = "init_"+internalQueueName;
	private static final  String initInternalQAction = internalQueueName+" \u2254 \u2205";
	private static final  String initCompletionFlagName = "init_"+completionFlagName;
	private static final  String initCompletionFlagAction = completionFlagName+" \u2254 TRUE";
	//e1
	private static final  String futureExternalTriggersEventName = "SCXML_futureExternalTrigger";
	private static final  String raisedExternalTriggersParameterName = "SCXML_raisedTriggers";
	private static final  String raisedExternalTriggersParameterComment = "";
	private static final  String e1_g1_Name = "typeof_"+raisedExternalTriggersParameterName;
	private static final  String e1_g1_Predicate = raisedExternalTriggersParameterName+" \u2286 "+externalTriggersName;
	private static final  String e1_g1_Comment = "";
	private static final  String e1_a1_Name = "SCXML_raiseExternalTriggers";
	private static final  String e1_a1_Action = externalQueueName+" ≔ "+externalQueueName+" \u222a "+raisedExternalTriggersParameterName;
	private static final  String e1_a1_Comment = "";
	//e2
	private static final  String futureInternalTriggersEventName = "SCXML_futureInternalTrigger";	
	private static final  String raisedInternalTriggersParameterName = "SCXML_raisedTriggers";
	private static final  String raisedInternalTriggersParameterComment = "";
	private static final  String e2_g1_Name = "typeof_"+raisedInternalTriggersParameterName;
	private static final  String e2_g1_Predicate = raisedInternalTriggersParameterName+" \u2286 "+internalTriggersName;
	private static final  String e2_g1_Comment = "";
	private static final  String e2_a1_Name = "SCXML_raiseInternalTriggers";
	private static final  String e2_a1_Action = internalQueueName+" ≔ "+internalQueueName+" \u222a "+raisedInternalTriggersParameterName;
	private static final  String e2_a1_Comment = "";
	//e3
	private static final  String consumeExternalTriggerEventName = "SCXML_futureExternalTransitionSet";
	private static final  String consumedExternalTriggerParameterName = "SCXML_et";
	private static final  String consumedExternalTriggerParameterComment = "";
	private static final  String e3_g1_Name = "typeof_"+consumedExternalTriggerParameterName;
	private static final  String e3_g1_Predicate = consumedExternalTriggerParameterName+" ∈ "+externalQueueName;
	private static final  String e3_g1_Comment = "";
	private static final  String e3_g2_Name = "SCXML_internalQEmpty";
	private static final  String e3_g2_Predicate = internalQueueName+" = \u2205";
	private static final  String e3_g2_Comment = "";
	private static final  String e3_g3_Name = "SCXML_complete";
	private static final  String e3_g3_Predicate = completionFlagName+" = TRUE";
	private static final  String e3_g3_Comment = "";
	private static final  String e3_a1_Name = "SCXML_notComplete";
	private static final  String e3_a1_Action = completionFlagName+" \u2254 FALSE";
	private static final  String e3_a1_Comment = "";
	private static final  String e3_a2_Name = "SCXML_consumeExternalTrigger";
	private static final  String e3_a2_Action = externalQueueName+" \u2254 "+externalQueueName+" \u2216 {"+consumedExternalTriggerParameterName+"}";
	private static final  String e3_a2_Comment = "";
	//e4
	private static final  String consumeInternalTriggerEventName = "SCXML_futureInternalTransitionSet";
	private static final  String consumedInternalTriggerParameterName = "SCXML_it";
	private static final  String consumedInternalTriggerParameterComment = "";
	private static final  String e4_g1_Name = "typeof_"+consumedInternalTriggerParameterName;
	private static final  String e4_g1_Predicate = consumedInternalTriggerParameterName+"  ∈ "+internalQueueName;
	private static final  String e4_g1_Comment = "";
	private static final  String e4_g2_Name = "SCXML_complete";
	private static final  String e4_g2_Predicate = completionFlagName+" = TRUE";
	private static final  String e4_g2_Comment = "";
	private static final  String e4_a1_Name = "SCXML_notComplete";
	private static final  String e4_a1_Action = completionFlagName+" \u2254 FALSE";
	private static final  String e4_a1_Comment = "";
	private static final  String e4_a2_Name = "SCXML_consumeInternalTrigger";
	private static final  String e4_a2_Action = internalQueueName+" \u2254 "+internalQueueName+" \u2216 {"+consumedInternalTriggerParameterName+"}";
	private static final  String e4_a2_Comment = "";
	//e5
	private static final  String untriggeredEventName = "SCXML_futureUntriggeredTransitionSet";
	private static final  String e5_g1_Name = "SCXML_notComplete";
	private static final  String e5_g1_Predicate = completionFlagName+" = FALSE";
	private static final  String e5_g1_Comment = "";
	private static final  String e5_a1_Name = "SCXML_notComplete";
	private static final  String e5_a1_Action = completionFlagName+" \u2254 FALSE";
	private static final  String e5_a1_Comment = "";
	//e6
	private static final  String completionEventName = "SCXML_Completion";
	private static final  String e6_g1_Name = "SCXML_notComplete";
	private static final  String e6_g1_Predicate = completionFlagName+" = FALSE";
	private static final  String e6_g1_Comment = "";
	private static final  String e6_a1_Name = "SCXML_Complete";
	private static final  String e6_a1_Action = completionFlagName+" \u2254 TRUE";
	private static final  String e6_a1_Comment = "";
	
	
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
	//TODO: add triggers raised ???
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
		
		Event e1 = (Event) Make.event(futureExternalTriggersEventName);
		Parameter p1 = (Parameter) Make.parameter(raisedExternalTriggersParameterName, raisedExternalTriggersParameterComment);
		e1.getParameters().add(p1);
		Guard e1_g1 = (Guard) Make.guard(e1_g1_Name, false, e1_g1_Predicate, e1_g1_Comment);
		e1.getGuards().add(e1_g1);
		Action e1_a1 = (Action) Make.action(e1_a1_Name, e1_a1_Action, e1_a1_Comment);
		e1.getActions().add(e1_a1);
		basis.getEvents().add(e1);

		Event e2 = (Event) Make.event(futureInternalTriggersEventName);
		Parameter p2 = (Parameter) Make.parameter(raisedInternalTriggersParameterName, raisedInternalTriggersParameterComment);
		e2.getParameters().add(p2);
		Guard e2_g1 = (Guard) Make.guard(e2_g1_Name, false, e2_g1_Predicate, e2_g1_Comment);
		e2.getGuards().add(e2_g1);
		Action e2_a1 = (Action) Make.action(e2_a1_Name, e2_a1_Action, e2_a1_Comment);
		e2.getActions().add(e2_a1);
		basis.getEvents().add(e2);
		
		Event e3 = (Event) Make.event(consumeExternalTriggerEventName);
		Parameter p3 = (Parameter) Make.parameter(consumedExternalTriggerParameterName, consumedExternalTriggerParameterComment);
		e3.getParameters().add(p3);
		Guard e3_g1 = (Guard) Make.guard(e3_g1_Name, false, e3_g1_Predicate, e3_g1_Comment);
		e3.getGuards().add(e3_g1);
		Guard e3_g2 = (Guard) Make.guard(e3_g2_Name, false, e3_g2_Predicate, e3_g2_Comment);
		e3.getGuards().add(e3_g2);
		Guard e3_g3 = (Guard) Make.guard(e3_g3_Name, false, e3_g3_Predicate, e3_g3_Comment);
		e3.getGuards().add(e3_g3);
		Action e3_a1 = (Action) Make.action(e3_a1_Name, e3_a1_Action, e3_a1_Comment);
		e3.getActions().add(e3_a1);
		Action e3_a2 = (Action) Make.action(e3_a2_Name, e3_a2_Action, e3_a2_Comment);
		e3.getActions().add(e3_a2);
		basis.getEvents().add(e3);
		
		Event e4 = (Event) Make.event(consumeInternalTriggerEventName);
		Parameter p4 = (Parameter) Make.parameter(consumedInternalTriggerParameterName, consumedInternalTriggerParameterComment);
		e4.getParameters().add(p4);		
		Guard e4_g1 = (Guard) Make.guard(e4_g1_Name, false, e4_g1_Predicate, e4_g1_Comment);
		e4.getGuards().add(e4_g1);
		Guard e4_g2 = (Guard) Make.guard(e4_g2_Name, false, e4_g2_Predicate, e4_g2_Comment);
		e4.getGuards().add(e4_g2);
		Action e4_a1 = (Action) Make.action(e4_a1_Name, e4_a1_Action, e4_a1_Comment);
		e4.getActions().add(e4_a1);
		Action e4_a2 = (Action) Make.action(e4_a2_Name, e4_a2_Action, e4_a2_Comment);
		e4.getActions().add(e4_a2);
		basis.getEvents().add(e4);
		
		Event e5 = (Event) Make.event(untriggeredEventName);
		Guard e5_g1 = (Guard) Make.guard(e5_g1_Name, false, e5_g1_Predicate, e5_g1_Comment);
		e5.getGuards().add(e5_g1);
		Action e5_a1 = (Action) Make.action(e5_a1_Name, e5_a1_Action, e5_a1_Comment);
		e5.getActions().add(e5_a1);
		basis.getEvents().add(e5);
		
		Event e6 = (Event) Make.event(completionEventName);
		Guard e6_g1 = (Guard) Make.guard(e6_g1_Name, false, e6_g1_Predicate, e6_g1_Comment);
		e6.getGuards().add(e6_g1);
		Action e6_a1 = (Action) Make.action(e6_a1_Name, e6_a1_Action, e6_a1_Comment);
		e6.getActions().add(e6_a1);
		basis.getEvents().add(e6);
		
		
		return basis;
	}
	


}
