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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.tests.sample.scxml.ScxmlInitialType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlParallelType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlRaiseType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlTransitionType;
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
		int depth = getRefinementDepth(sourceElement);
		
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
		
		findTriggers(scxml);
		
		
		return ret;
	}


	/**
	 * @param scxml
	 */
	private void findTriggers(ScxmlScxmlType scxml) {
		Map<String, List<ScxmlTransitionType>> triggers = new HashMap<String, List<ScxmlTransitionType>>();
		Map<String, List<ScxmlTransitionType>> raisedTriggers = new HashMap<String, List<ScxmlTransitionType>>();
//		Map<ScxmlTransitionType, Set<ScxmlTransitionType>> synchronous = new HashMap<ScxmlTransitionType, Set<ScxmlTransitionType>>();
		TreeIterator<EObject> it = scxml.eAllContents();
		while (it.hasNext()){
			EObject next = it.next(); 
			if (next instanceof ScxmlTransitionType && !initialTransition((ScxmlTransitionType)next)){
				ScxmlTransitionType scxmlTransition = (ScxmlTransitionType)next;
				String triggerName = scxmlTransition.getEvent();
				if (triggerName==null || triggerName.trim().length()==0) triggerName = "null"; //make sure it is not an empty identifier
				//if (triggerName!=null && triggerName.length()>0){
					List<ScxmlTransitionType> tlist = triggers.containsKey(triggerName)? 
							triggers.get(triggerName) :
							new ArrayList<ScxmlTransitionType>();
					tlist.add(scxmlTransition);
					triggers.put(triggerName, tlist);
					//synchronous.put(scxmlTransition, findSynchronisations(scxmlTransition));   //NOT SURE HOW TO DO THIS
				//}
				EList<ScxmlRaiseType> raises = scxmlTransition.getRaise();
				for (ScxmlRaiseType raise : raises){
					List<ScxmlTransitionType> trans; 
					if (raisedTriggers.containsKey(raise.getEvent())){	
						trans = raisedTriggers.get(raise.getEvent());
					}else{
						trans = new ArrayList<ScxmlTransitionType>();
					}
					trans.add(scxmlTransition);
					raisedTriggers.put(raise.getEvent(), trans);
				}
			}
		}
		
		//for each trigger, the transitions that are triggered
		storage.stash("triggers", triggers);
		// for each raised trigger, the transitions that raise it
		storage.stash("raises", raisedTriggers);
		//for each transition, the set of transitions that could possibly synchronise with it (but might not)
//		storage.stash("synchronous", synchronous);
		
		// for each trigger, the valid combinations of transitions that we need to provide events for
		Map<String, Set<Set<ScxmlTransitionType>>> combinations = new HashMap<String,Set<Set<ScxmlTransitionType>>>();
		for (String t : triggers.keySet()){
			List<ScxmlTransitionType> trs = triggers.get(t);
			//for (int i = 0; i<trs.size(); i++){
			combinations.put(t,getCombinations(trs));
				
//				for (Set<ScxmlTransitionType> combination : combinations.get(t)){
//					Event e = creatEventForTransitionSet(t, combination);
//					if (e!=null){
//						
//					}
//				}

			//}
		}
		storage.stash("combinations", combinations);
	}

//	/**
//	 * @param combination 
//	 * @param combination
//	 * @return
//	 */
//	private Event creatEventForTransitionSet(String triggerName, Set<ScxmlTransitionType> combination){
//		String name = triggerName;
//		for (ScxmlTransitionType trs : combination){
//			name= name+"_"+Utils.getEventNames(trs, machine, generatedElements);
//		}
//		if (name.length() > triggerName.length()){
//			Event e = (Event) Make.event(name);
//			return e;
//		}
//		return null;
//	}


	private Set<Set<ScxmlTransitionType>> getCombinations(List<ScxmlTransitionType> transitionList){
		Set<Set<ScxmlTransitionType>> combinations= new HashSet<Set<ScxmlTransitionType>>();
		
		//if empty return empty set of combinations
		if (transitionList.size()==0) return combinations;
		
		//add the singleton with the first transition
		HashSet<ScxmlTransitionType> singleton = new HashSet<ScxmlTransitionType>();
		ScxmlTransitionType t0 = transitionList.get(0);
		singleton.add(transitionList.get(0));
		combinations.add(singleton);
		
		//recursion on the tail
		Set<Set<ScxmlTransitionType>> tailCombinations = getCombinations(transitionList.subList(1, transitionList.size()));	
		
		//add combinations of first element with tail combinations
		if (transitionList.size() > 1){
			//look for valid combinations with the first transition
			for (Set<ScxmlTransitionType> subComb : tailCombinations){
				if (parallel(t0,subComb)){
					Set<ScxmlTransitionType> comb = new HashSet<ScxmlTransitionType>(subComb);
					comb.add(t0);
					combinations.add(comb);
				}
			}
			//finished
		}
		
		//add tail combinations without first element
		combinations.addAll(tailCombinations);
		
		return combinations;
	}
	
	/**
	 * returns true if the transition is parallel with all the transitions in the collection
	 * @param tr : scxmlTransition
	 * @param trSet : Set<ScxmlTransitionType>
	 * @return 
	 */
	private boolean parallel(ScxmlTransitionType tr, Set<ScxmlTransitionType> trSet) {
		for (ScxmlTransitionType t2 : trSet){
			if (!parallel(tr,t2)) return false;
		}
		return true;
	}
	

	/**
	 * returns true if 2 transitions are in parallel regions of statemachines
	 *  and neither are initial transitions
	 * @param tr
	 * @param t2
	 * @return
	 */
	private boolean parallel(ScxmlTransitionType t1, ScxmlTransitionType t2) {
		if (initialTransition(t1) || initialTransition(t2)) return false;
		EObject trParent = t1.eContainer();
		while (trParent!=null){
			EObject trGrandParent = trParent.eContainer();
			if (trGrandParent instanceof ScxmlParallelType){
				for (ScxmlStateType state : ((ScxmlParallelType)trGrandParent).getState()){
					if (state!=trParent && contains(state, t2)){
						return true;
					}
				}
			}
			trParent = trGrandParent;
		}
		return false;
	}

	/**
	 * @param t1
	 * @param trParent
	 * @return
	 */
	private boolean initialTransition(ScxmlTransitionType t1) {
		if (t1.eContainer() instanceof ScxmlInitialType){
			return true;			
		}else{
			return false;
		}
	}


	/**
	 * @param state
	 * @param t2
	 * @return
	 */
	private boolean contains(EObject p, EObject e) {
		TreeIterator<EObject> it = p.eAllContents();
		while (it.hasNext()){
			EObject next = it.next(); 
			if (next == e) return true;
		}
		return false;
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

		Event e2 = (Event) Make.event(Strings.futureInternalTriggersEventName);
		Parameter p2 = (Parameter) Make.parameter(Strings.raisedInternalTriggersParameterName, Strings.raisedInternalTriggersParameterComment);
		e2.getParameters().add(p2);
		Guard e2_g1 = (Guard) Make.guard(Strings.e2_g1_Name, false, Strings.e2_g1_Predicate, Strings.e2_g1_Comment);
		e2.getGuards().add(e2_g1);
		Action e2_a1 = (Action) Make.action(Strings.e2_a1_Name, Strings.e2_a1_Action, Strings.e2_a1_Comment);
		e2.getActions().add(e2_a1);
		basis.getEvents().add(e2);
		
		Event e3 = (Event) Make.event(Strings.consumeExternalTriggerEventName);
		Parameter p3 = (Parameter) Make.parameter(Strings.consumedExternalTriggerParameterName, Strings.consumedExternalTriggerParameterComment);
		e3.getParameters().add(p3);
		Guard e3_g1 = (Guard) Make.guard(Strings.e3_g1_Name, false, Strings.e3_g1_Predicate, Strings.e3_g1_Comment);
		e3.getGuards().add(e3_g1);
		Guard e3_g2 = (Guard) Make.guard(Strings.e3_g2_Name, false, Strings.e3_g2_Predicate, Strings.e3_g2_Comment);
		e3.getGuards().add(e3_g2);
		Guard e3_g3 = (Guard) Make.guard(Strings.e3_g3_Name, false, Strings.e3_g3_Predicate, Strings.e3_g3_Comment);
		e3.getGuards().add(e3_g3);
		Action e3_a1 = (Action) Make.action(Strings.e3_a1_Name, Strings.e3_a1_Action, Strings.e3_a1_Comment);
		e3.getActions().add(e3_a1);
		Action e3_a2 = (Action) Make.action(Strings.e3_a2_Name, Strings.e3_a2_Action, Strings.e3_a2_Comment);
		e3.getActions().add(e3_a2);
		basis.getEvents().add(e3);
		
		Event e4 = (Event) Make.event(Strings.consumeInternalTriggerEventName);
		Parameter p4 = (Parameter) Make.parameter(Strings.consumedInternalTriggerParameterName, Strings.consumedInternalTriggerParameterComment);
		e4.getParameters().add(p4);		
		Guard e4_g1 = (Guard) Make.guard(Strings.e4_g1_Name, false, Strings.e4_g1_Predicate, Strings.e4_g1_Comment);
		e4.getGuards().add(e4_g1);
		Guard e4_g2 = (Guard) Make.guard(Strings.e4_g2_Name, false, Strings.e4_g2_Predicate, Strings.e4_g2_Comment);
		e4.getGuards().add(e4_g2);
		Action e4_a1 = (Action) Make.action(Strings.e4_a1_Name, Strings.e4_a1_Action, Strings.e4_a1_Comment);
		e4.getActions().add(e4_a1);
		Action e4_a2 = (Action) Make.action(Strings.e4_a2_Name, Strings.e4_a2_Action, Strings.e4_a2_Comment);
		e4.getActions().add(e4_a2);
		basis.getEvents().add(e4);
		
		Event e5 = (Event) Make.event(Strings.untriggeredEventName);
		Guard e5_g1 = (Guard) Make.guard(Strings.e5_g1_Name, false, Strings.e5_g1_Predicate, Strings.e5_g1_Comment);
		e5.getGuards().add(e5_g1);
		Action e5_a1 = (Action) Make.action(Strings.e5_a1_Name, Strings.e5_a1_Action, Strings.e5_a1_Comment);
		e5.getActions().add(e5_a1);
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
