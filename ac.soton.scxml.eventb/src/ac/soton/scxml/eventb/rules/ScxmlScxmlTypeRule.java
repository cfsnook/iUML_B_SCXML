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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.tests.sample.scxml.ScxmlRaiseType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlTransitionType;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
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
		
		// Reset the storage
		storage.reset();

		ScxmlScxmlType scxml = (ScxmlScxmlType)sourceElement;
		
		//find triggers and set up data for them in the storage
		//(triggers are only represented in the scxml model by string attributes of transitions 
		// so much of their details are implicit - hence the need to calculate this before the translation starts)
		Map<String, Trigger> triggers =  findTriggers(scxml);
		storage.stash("triggers", triggers);
		
		int depth = getRefinementDepth(sourceElement);
		List<TranslationDescriptor> ret = new ArrayList<TranslationDescriptor>();
		String fileName = scxml.eResource().getURI().toPlatformString(true);

		String statechartName = scxml.getName()+"_sm";
		Project project = Utils.findProject(sourceElement);
		
		//get the basis Context
		Context context = getBasisContext();
		ret.add(Make.descriptor(project, components, context ,1));
		
		//get the basis Machine
		Machine machine = getBasisMachine(context);
		ret.add(Make.descriptor(project, components, machine ,1));
		
		String allExternals = null;
		String allInternals = null;
		//create the refinement chain of machines
		for (int i=0; i<=depth; i++){
			
			// make a new machine by refining the previous level
			machine = (Machine) refine (scxml, machine, Utils.getMachineName(scxml,i), Strings.generatedFromFileComment(fileName));
			//set all events as extended rather than copied and add extra guards to 'future' events
			for (Event e : machine.getEvents()){
				if (!e.getRefines().isEmpty()){
					e.setExtended(true);
					e.getParameters().clear();
					e.getGuards().clear();
					e.getActions().clear();
					//add guards for next level
					if (Strings.futureExternalTriggersEventName.equals(e.getName())){
						Guard e1_g1 = (Guard) Make.guard(Strings.e1_g1_Name+i, false, Strings.e1_g1_Predicate+i, Strings.e1_g1_Comment);
						e.getGuards().add(e1_g1);
					}
				}else{
					System.out.println("Non refined event: "+e.getName());
				}
			}
			
			//create the descriptor to put the machine in the project
			ret.add(Make.descriptor(project, components, machine ,1));
			// make a new context by refining the previous level
			context = (Context) refine (scxml, context, Utils.getContextName(scxml,i), Strings.generatedFromFileComment(fileName));
			//create the descriptor to put the machine in the project
			ret.add(Make.descriptor(project, components, context ,1));
			
			// all levels see the corresponding context which extends the basis context
			machine.getSeesNames().add(context.getName()); //Strings.basisContextName);

			//if this is the first level add a state-machine (subsequent levels will copy it by refinement)
			if (i==0){
				Statemachine statemachine = (Statemachine) Make.statemachine(statechartName, tkind, "");
				machine.getExtensions().add(statemachine);
			}
			
			//add any invariants that are for this refinement level
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
			
			//define triggers in contexts and 
			// add any external trigger raising events needed at this level
			Constant futureinternals = (Constant) Make.constant(Strings.internalTriggersName(i), "");
			context.getConstants().add(futureinternals);
			Constant futureexternals = (Constant) Make.constant(Strings.externalTriggersName(i), "");
			context.getConstants().add(futureexternals);
			String internals = null;
			String externals = null;			
			for (String tname : triggers.keySet()){
				if (tname == "null") continue;
				Trigger t = triggers.get(tname);
				if (t.getRefinementLevel()==i){
					Constant trigConst = (Constant) Make.constant(t.getName(), "trigger");
					context.getConstants().add(trigConst);
					if (t.isExternal()){
						externals=externals==null? t.getName(): externals+"},{"+t.getName();
						Event e = (Event) Make.event("ExternalTriggerEvent_"+t.getName());
						e.getRefinesNames().add(Strings.futureExternalTriggersEventName);
						e.setExtended(true);
						Guard g = (Guard) Make.guard(
								Strings.specificRaisedExternalTriggerGuardName, false, 
								Strings.specificRaisedExternalTriggerGuardPredicate(t.getName()), 
								Strings.specificRaisedExternalTriggerGuardComment);
						e.getGuards().add(g);
						machine.getEvents().add(e);
					}else{
						internals=internals==null? t.getName(): internals+"},{"+t.getName();
					}
				}
			}
			Axiom eax = (Axiom) Make.axiom(Strings.externalTriggerAxiomName(i), Strings.externalTriggerDefinitionAxiomPredicate(i, externals), "");
			context.getAxioms().add(eax);
			Axiom iax = (Axiom) Make.axiom(Strings.internalTriggerAxiomName(i), Strings.internalTriggerDefinitionAxiomPredicate(i, internals), "");
			context.getAxioms().add(iax);
			if (externals!=null){
				allExternals =  allExternals==null? externals : allExternals+"},{"+externals;
				if (i>0){
					Axiom eax_prob = (Axiom) Make.axiom(Strings.externalProbTriggerAxiomName(i), Strings.externalProbTriggerDefinitionAxiomPredicate(i, allExternals), "help ProB");
					context.getAxioms().add(eax_prob);
				}
			}
			if (internals!=null){
				allInternals = allInternals==null? internals : allInternals+"},{"+internals;			
				if (i>0){
					Axiom iax_prob = (Axiom) Make.axiom(Strings.internalProbTriggerAxiomName(i), Strings.internalProbTriggerDefinitionAxiomPredicate(i, allInternals), "help ProB");
					context.getAxioms().add(iax_prob);
				}
			}
		}
		return ret;
	}

	/**
	 * @param component
	 * @return
	 */
	private EventBNamedCommentedComponentElement refine(EObject sourceElement, EventBNamedCommentedComponentElement component, String refineName, String comment) {
		URI uri = EcoreUtil.getURI(sourceElement);
		uri = uri.trimFragment().trimSegments(1);
		uri = uri.appendSegment(component.getName());
		uri = uri.appendFileExtension(
				component instanceof Machine? "bum" :
					component instanceof Context? "buc" :
						"xmb");
		uri = uri.appendFragment(component.getReference());
		AbstractElementRefiner refiner = ElementRefinerRegistry.getRegistry().getRefiner(component);
		Map<EObject,EObject> copy = refiner.refine(uri, component, null);
		EventBNamedCommentedComponentElement refinement = (EventBNamedCommentedComponentElement) copy.get(component);
		refinement.setName(refineName);
		refinement.setComment(comment);		
		return refinement;
	}
	

	/**
	 * calculate trigger data for the Scxml model
	 * 
	 * @param scxml
	 * @throws Exception 
	 */
	private Map<String, Trigger> findTriggers(ScxmlScxmlType scxml) throws Exception {
		Map<String, Trigger> triggers = new HashMap<String,Trigger>();
		//iterate over the entire model looking for transitions
		//(trigger names are defined by string attribute 'event' of a transition and
		// also by a transition's collection of ScxmlRaiseType elements).
		TreeIterator<EObject> it = scxml.eAllContents();
		while (it.hasNext()){
			EObject next = it.next(); 
			if (next instanceof ScxmlTransitionType){ 
				ScxmlTransitionType scxmlTransition = (ScxmlTransitionType)next;
				
				//record any trigger that triggers this transition (including the null trigger)
				String triggerName = scxmlTransition.getEvent();
				if (triggerName==null || triggerName.trim().length()==0) triggerName = "null"; //make sure it is not an empty identifier
				Trigger trigger = triggers.get(triggerName);
				if (trigger == null){
					trigger = new Trigger(triggerName);
					triggers.put(triggerName, trigger);
				}		
				trigger.addTriggeredTransition(scxmlTransition);
					
				//record any triggers raised by this transition
				EList<ScxmlRaiseType> raises = scxmlTransition.getRaise();
				for (ScxmlRaiseType raise : raises){
					String raisedTriggerName = raise.getEvent();
					Trigger raisedTrigger = triggers.get(raisedTriggerName);
					if (raisedTrigger == null){
						raisedTrigger = new Trigger(raisedTriggerName);
						triggers.put(raisedTriggerName, raisedTrigger);
					}		
					raisedTrigger.addRaisedByTransition(raise);
				}
			}
		}	
		return triggers;
	}
	
	
	/*********************************************************
	 * The remainder generates the basis context and machine
	 *********************************************************/
	
	/**
	 * @return
	 */
	private Context getBasisContext() {
		Context basis = (Context) Make.context(Strings.basisContextName, "(generated for SCXML)");
		CarrierSet triggerSet = (CarrierSet) Make.set(Strings.triggerSetName, "all possible triggers");
		basis.getSets().add(triggerSet);
		Constant const1 = (Constant) Make.constant(Strings.internalTriggersName, "all possible internal triggers");
		basis.getConstants().add(const1);
		Constant const2 = (Constant) Make.constant(Strings.externalTriggersName, "all possible external triggers");
		basis.getConstants().add(const2);
		Axiom ax = (Axiom) Make.axiom(Strings.triggerPartitionAxiomName, Strings.triggerPartitionAxiomPredicate, "");
		basis.getAxioms().add(ax);
		return basis;
	}
	
	/**
	 * @return
	 */
	//TODO: add triggers raised ???
	private Machine getBasisMachine(Context basisContext) {
		Machine basis = (Machine) Make.machine(Strings.basisMachineName, "(generated for SCXML)");
		basis.getSeesNames().add(Strings.basisContextName);
		//basis.getSees().add(basisContext);
		
		Variable v1 = (Variable) Make.variable(Strings.internalQueueName, "internal trigger queue");
		basis.getVariables().add(v1);
		Variable v2 = (Variable) Make.variable(Strings.externalQueueName, "external trigger queue");
		basis.getVariables().add(v2);
		Variable v3 = (Variable) Make.variable(Strings.completionFlagName, "run to completion flag");
		basis.getVariables().add(v3);

		Invariant i1 = (Invariant) Make.invariant(Strings.internalQueueTypeName, Strings.internalQueueTypePredicate, "internal trigger queue");
		basis.getInvariants().add(i1);
		Invariant i2 = (Invariant) Make.invariant(Strings.externalQueueTypeName, Strings.externalQueueTypePredicate, "external trigger queue");
		basis.getInvariants().add(i2);
		Invariant i3 = (Invariant) Make.invariant(Strings.queueDisjunctionName, Strings.queueDisjunctionPredicate, "queues are disjoint");
		basis.getInvariants().add(i3);
		Invariant i4 = (Invariant) Make.invariant(Strings.completionFlagTypeName, Strings.completionFlagTypePredicate, "completion flag");
		basis.getInvariants().add(i4);
		
		Event initialisation = (Event) Make.event("INITIALISATION");
		basis.getEvents().add(initialisation);
		
		Action a1 = (Action) Make.action(Strings.initInternalQName, Strings.initInternalQAction, "internal Q is initially empty");
		initialisation.getActions().add(a1);
		Action a2 = (Action) Make.action(Strings.initExternalQName, Strings.initExternalQAction, "external Q is initially empty");
		initialisation.getActions().add(a2);		
		Action a3 = (Action) Make.action(Strings.initCompletionFlagName, Strings.initCompletionFlagAction, "completion is initially FALSE");
		initialisation.getActions().add(a3);
		
		Event e1 = (Event) Make.event(Strings.futureExternalTriggersEventName);
		Parameter p1 = (Parameter) Make.parameter(Strings.raisedExternalTriggersParameterName, Strings.raisedExternalTriggersParameterComment);
		e1.getParameters().add(p1);
		Guard e1_g1 = (Guard) Make.guard(Strings.e1_g1_Name, false, Strings.e1_g1_Predicate, Strings.e1_g1_Comment);
		e1.getGuards().add(e1_g1);
		Action e1_a1 = (Action) Make.action(Strings.e1_a1_Name, Strings.e1_a1_Action, Strings.e1_a1_Comment);
		e1.getActions().add(e1_a1);
		basis.getEvents().add(e1);
		
		Event e3 = (Event) Make.event(Strings.consumeExternalTriggerEventName);
		Parameter p3_1 = (Parameter) Make.parameter(Strings.consumedExternalTriggerParameterName, Strings.consumedExternalTriggerParameterComment);
		e3.getParameters().add(p3_1);
		Parameter p3_2 = (Parameter) Make.parameter(Strings.raisedInternalTriggersParameterName, Strings.raisedInternalTriggersParameterComment);
		e3.getParameters().add(p3_2);
		Guard e3_g1 = (Guard) Make.guard(Strings.e3_g1_Name, false, Strings.e3_g1_Predicate, Strings.e3_g1_Comment);
		e3.getGuards().add(e3_g1);
		Guard e3_g2 = (Guard) Make.guard(Strings.e3_g2_Name, false, Strings.e3_g2_Predicate, Strings.e3_g2_Comment);
		e3.getGuards().add(e3_g2);
		Guard e3_g3 = (Guard) Make.guard(Strings.e3_g3_Name, false, Strings.e3_g3_Predicate, Strings.e3_g3_Comment);
		e3.getGuards().add(e3_g3);
		Guard e3_g4 = (Guard) Make.guard(Strings.raisedInternalTriggersGuardName, false, Strings.raisedInternalTriggersGuardPredicate, Strings.raisedInternalTriggersGuardComment);
		e3.getGuards().add(e3_g4);
		Action e3_a1 = (Action) Make.action(Strings.e3_a1_Name, Strings.e3_a1_Action, Strings.e3_a1_Comment);
		e3.getActions().add(e3_a1);
		Action e3_a2 = (Action) Make.action(Strings.e3_a2_Name, Strings.e3_a2_Action, Strings.e3_a2_Comment);
		e3.getActions().add(e3_a2);
		Action e3_a3 = (Action) Make.action(Strings.raisedInternalTriggersActionName, Strings.raisedInternalTriggersActionAction, Strings.raisedInternalTriggersActionComment);
		e3.getActions().add(e3_a3);
		basis.getEvents().add(e3);
		
		Event e4 = (Event) Make.event(Strings.consumeInternalTriggerEventName);
		Parameter p4 = (Parameter) Make.parameter(Strings.consumedInternalTriggerParameterName, Strings.consumedInternalTriggerParameterComment);
		e4.getParameters().add(p4);	
		Parameter p4_2 = (Parameter) Make.parameter(Strings.raisedInternalTriggersParameterName, Strings.raisedInternalTriggersParameterComment);
		e4.getParameters().add(p4_2);
		Guard e4_g1 = (Guard) Make.guard(Strings.e4_g1_Name, false, Strings.e4_g1_Predicate, Strings.e4_g1_Comment);
		e4.getGuards().add(e4_g1);
		Guard e4_g2 = (Guard) Make.guard(Strings.e4_g2_Name, false, Strings.e4_g2_Predicate, Strings.e4_g2_Comment);
		e4.getGuards().add(e4_g2);
		Guard e4_g3 = (Guard) Make.guard(Strings.raisedInternalTriggersGuardName, false, Strings.raisedInternalTriggersGuardPredicate, Strings.raisedInternalTriggersGuardComment);
		e4.getGuards().add(e4_g3);
		Action e4_a1 = (Action) Make.action(Strings.e4_a1_Name, Strings.e4_a1_Action, Strings.e4_a1_Comment);
		e4.getActions().add(e4_a1);
		Action e4_a2 = (Action) Make.action(Strings.e4_a2_Name, Strings.e4_a2_Action, Strings.e4_a2_Comment);
		e4.getActions().add(e4_a2);
//		Action e4_a3 = (Action) Make.action(Strings.raisedInternalTriggersActionName, Strings.raisedInternalTriggersActionAction, Strings.raisedInternalTriggersActionComment);
//		e4.getActions().add(e4_a3);
		basis.getEvents().add(e4);
		
		Event e5 = (Event) Make.event(Strings.untriggeredEventName);
		Parameter p5_1 = (Parameter) Make.parameter(Strings.raisedInternalTriggersParameterName, Strings.raisedInternalTriggersParameterComment);
		e5.getParameters().add(p5_1);
		Guard e5_g1 = (Guard) Make.guard(Strings.e5_g1_Name, false, Strings.e5_g1_Predicate, Strings.e5_g1_Comment);
		e5.getGuards().add(e5_g1);
		Guard e5_g2 = (Guard) Make.guard(Strings.raisedInternalTriggersGuardName, false, Strings.raisedInternalTriggersGuardPredicate, Strings.raisedInternalTriggersGuardComment);
		e5.getGuards().add(e5_g2);
		Action e5_a1 = (Action) Make.action(Strings.e5_a1_Name, Strings.e5_a1_Action, Strings.e5_a1_Comment);
		e5.getActions().add(e5_a1);
		Action e5_a2 = (Action) Make.action(Strings.raisedInternalTriggersActionName, Strings.raisedInternalTriggersActionAction, Strings.raisedInternalTriggersActionComment);
		e5.getActions().add(e5_a2);
		basis.getEvents().add(e5);
		
		Event e6 = (Event) Make.event(Strings.completionEventName);
		Guard e6_g1 = (Guard) Make.guard(Strings.e6_g1_Name, false, Strings.e6_g1_Predicate, Strings.e6_g1_Comment);
		e6.getGuards().add(e6_g1);
		Action e6_a1 = (Action) Make.action(Strings.e6_a1_Name, Strings.e6_a1_Action, Strings.e6_a1_Comment);
		e6.getActions().add(e6_a1);
		basis.getEvents().add(e6);
		
		return basis;
	}
	


}
