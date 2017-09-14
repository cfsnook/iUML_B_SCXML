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
/**
 * 
 */
package ac.soton.scxml.eventb.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.tests.sample.scxml.ScxmlDataType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlFinalType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlInitialType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlLogType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlPackage;
import org.eclipse.sirius.tests.sample.scxml.ScxmlScxmlType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlTransitionType;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.Project;
import org.eventb.emf.core.machine.Convergence;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;

import ac.soton.emf.translator.TranslationDescriptor;
import ac.soton.emf.translator.utils.Find;
import ac.soton.eventb.emf.core.extension.navigator.refiner.AbstractElementRefiner;
import ac.soton.eventb.emf.core.extension.navigator.refiner.ElementRefinerRegistry;
import ac.soton.scxml.eventb.rules.ITranslatorStorage;
import ac.soton.scxml.eventb.rules.TranslatorStorage;
import ac.soton.scxml.eventb.rules.Trigger;
import ac.soton.scxml.eventb.strings.Strings;

/**
 * <p> 
 * Utility methods used in scxml to iUML-B translation rules
 * 
 * </p>
 * 
 * @author cfs
 */
public class Utils {
	
	protected static ITranslatorStorage storage = TranslatorStorage.getDefault();
	
	/**
	 * Find by name, an element in a list of EventBNamed elements
	 * @param collection
	 * @param name
	 * @return
	 */
		public static EventBNamed findNamedElement(EList<? extends EventBNamed> collection, String name){
			for (EventBNamed element : collection){
				if (name.equals(element.getName())) return element;
			}
			return null;
		}
		
		
	/**
	 * find the containing Project for this element
	 * 
	 * CURRENTLY RETURNS NULL
	 * 
	 * @param machine
	 * @return
	 * @throws IOException
	 */
		public static Project findProject(EObject sourceElement) throws IOException {
//			URI eventBelementUri = eventBelement.getURI();
//			URI projectUri = eventBelementUri.trimFragment().trimSegments(1);
//			ProjectResource projectResource = new ProjectResource();
//			projectResource.setURI(eventBelement.getURI());
//			projectResource.load(null);
//			for (EObject eObject : projectResource.getContents()){
//				if (eObject instanceof Project){
//					return (Project)eObject;
//				}
//			}
			return null;
		}
	
	/**
	 * This finds an event in the given machine by name by looking in the following (in this order)
	 *  the given machine's events
	 *  the given list of GenerationDescriptors
	 *  if no such event is found a new event is created and added to the machine
	 *  
	 * @param machine
	 * @param descriptors
	 * @param eventName
	 * @return
	 */
	public static Event getOrCreateEvent(Machine machine, boolean extended, List<TranslationDescriptor> descriptors, String eventName) {
		Event ev = (Event) findNamedElement(machine.getEvents(), eventName);
		if (ev==null) ev =  (Event) Find.translatedElement(descriptors, machine, MachinePackage.Literals.MACHINE__EVENTS, eventName);
		if (ev==null) {
			ev = (Event) Make.event(eventName, extended, Convergence.ORDINARY, Collections.<String> emptyList(), "");
			machine.getEvents().add(ev);
		}
		return ev;
	}
	
	/**
	 * This finds the refinement depth required by the Scxml model containing the given Scxml element
	 * @param scxml element
	 * @return integer representing the number of refinements needed
	 */
	public static int getRefinementDepth(EObject scxmlElement) {
		Integer depth = (Integer) storage.fetch("depth");
		if (depth==null) {
			IumlbScxmlAdapter adapter = new IumlbScxmlAdapter(null);
			depth = 0;
			ScxmlScxmlType scxml = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, scxmlElement);
			List<EObject> eObjects = Find.eAllContents(scxml, EcorePackage.Literals.EOBJECT);
			for (EObject eObject : eObjects){
				int ref = (adapter.adapt(eObject)).getBasicRefinementLevel();
				depth = ref>depth? ref : depth;
			}
			storage.stash("depth",depth);
		}
		return depth;
	}
	
	/**
	 * Finds an event for the given transitions combination in the given machine or descriptors
	 *  if no such event is found a new event is created and added to the machine
	 *  
	 * @param machine
	 * @param translatedElements
	 * @param triggerName
	 * @param combi
	 * @return
	 */
	public static Set<Event> getOrCreateInitialEvents(Refinement ref, List<TranslationDescriptor> descriptors, Trigger trigger, Set<ScxmlTransitionType> combi) {
		Set<Event> ret = new HashSet<Event>();
		
		printCombi("getOrCreateInitialEvents",combi, ref);
		for (ScxmlTransitionType tr : combi){
			List<String> eventNames = getInitialEventNames(tr, ref.machine, descriptors);
			if (eventNames==null) continue;
			for (String eventName : eventNames){
				Event ev = getOrCreateEvent(ref.machine, false, descriptors,eventName);
				ret.add(ev);
			}
		}
		return ret;
	}
	
	/**
	 * Finds an event for the given transitions combination in the given machine or descriptors
	 *  if no such event is found a new event is created and added to the machine
	 *  
	 * @param machine
	 * @param translatedElements
	 * @param triggerName
	 * @param combi
	 * @return
	 */
	public static Event getOrCreateEvent(Refinement ref, List<TranslationDescriptor> descriptors, Trigger trigger, Set<ScxmlTransitionType> combi) {
		printCombi("getOrCreateEvent",combi, ref);
		String eventName = getCombiEventName(trigger.getName(), combi);		//PARAMETERS REMOVED... ref.machine, descriptors,
		Event ev = getOrCreateEvent(ref.machine, false, descriptors,eventName);
		String refinedEventName = getRefinesName(ref, descriptors, trigger, combi);
		if (!ev.getRefinesNames().contains(refinedEventName)){
			ev.getRefinesNames().add(refinedEventName);
		}
		if (ev.getRefinesNames().size()==0){
			ev.setExtended(false);
		}else{
			ev.setExtended(true);
		}
		return ev;
	}

	/**
	 * finds the appropriate refined event name for an event of this combi
	 * 
	 * @param ref
	 * @param descriptors
	 * @param trigger
	 * @param combi
	 * @return
	 */
	private static String getRefinesName(Refinement ref, List<TranslationDescriptor> descriptors,  Trigger trigger, Set<ScxmlTransitionType> combi) {
		//set the event refinement up
		String refinedEventName = null;						
		Set<ScxmlTransitionType> refinedCombi = findRefinedCombi(trigger, combi, ref);
		if (refinedCombi.size()>0){   
			refinedEventName =  Utils.getCombiEventName(trigger.getName(), refinedCombi); ///parameters removed... ref.machine, descriptors, ;
		}else{
			if ("null".equals(trigger.getName())){
					refinedEventName = Strings.untriggeredEventName;
			}else if(trigger.isInternal()){ 	//raisedTriggers.containsKey(triggerName)){
				refinedEventName = Strings.consumeInternalTriggerEventName;
			}else{
				refinedEventName = Strings.consumeExternalTriggerEventName;								
			}
		}
		return refinedEventName;
	}

	/**
	 * finds a combi that the given combi should refine
	 * 
	 * @param triggerName 
	 * @param combi
	 * @param ref
	 * @return
	 * @throws Exception 
	 */
	private static Set<ScxmlTransitionType> findRefinedCombi(Trigger trigger, Set<ScxmlTransitionType> combi, Refinement ref)  {
		if (ref.level==0) return Collections.emptySet();
		//first find all possible candidates that are subsets in the previous refinement level (can include itself)
		Set<Set<ScxmlTransitionType>> possibles = new HashSet<Set<ScxmlTransitionType>>();
		for (Set<ScxmlTransitionType> otherCombi : trigger.getTransitionCombinations(ref.level-1)){
			if (isSubset(combi,otherCombi)){
				possibles.add(otherCombi);
			}
		}
		// now find the biggest subset out of these possibles
		Set<ScxmlTransitionType> refinedCombi = Collections.emptySet();
		for (Set<ScxmlTransitionType> poss : possibles){
			if (isSubset(poss, refinedCombi)){
				refinedCombi = poss;
			}
		}
		return refinedCombi;
	}


	/**
	 * @param combi
	 * @param otherCombi
	 * @return
	 */
	private static boolean isSubset(Set<ScxmlTransitionType> combi, Set<ScxmlTransitionType> otherCombi) {
		for (ScxmlTransitionType tr : otherCombi){
			if (!combi.contains(tr)){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * constructs a name from a trigger name and a set of transitions.
	 * Any transitions that do not have a ScxmlStateType source are ignored.
	 * 
	 * @param triggerName
	 * @param combi
	 * @return
	 */
	public static String getCombiEventName(String triggerName, Set<ScxmlTransitionType> combi) {
		String eventName = (triggerName == null || triggerName.length()==0 || "null".equals(triggerName))? "" : triggerName;
		for (ScxmlTransitionType tr : combi){
			if (tr.eContainer() instanceof ScxmlStateType){
				String trName = Utils.getEventName(tr);
				eventName = eventName.length()==0 ?  trName : eventName+ "__" + trName;
			}
		}
		return eventName;
	}
	
	/**
	 * This finds all the events that are elaborated by transitions that target the real parent containing super-state
	 * of the given EObject (which is assumed to be an element in an SCXML state chart)
	 * (n.b. the real parent super-state is not a state representing a parallel region but a container of the parallel)
	 * 
	 * @param scxmlEObject - an element of a nested SCXML state chart
	 * @param machine	-	the relevant machine for the refinement level
	 * @param generatedElements - new elements to also be searched (machine must be the parent)
	 * @return
	 */
	public static List<Event> findIncomerEvents(EObject scxmlEObject, Machine machine, List<TranslationDescriptor> generatedElements) {
		List<Event> eventList = new ArrayList<Event>();
		// find scxml container,
		ScxmlScxmlType scxmlContainer = (ScxmlScxmlType) Find.containing(ScxmlPackage.Literals.SCXML_SCXML_TYPE, scxmlEObject);
		// find scxml parent state (i.e. the real parent state, not a region)
		EObject realScxmlParentState = scxmlEObject.eContainer();
		while (!(realScxmlParentState instanceof ScxmlStateType && 
				(		realScxmlParentState.eContainer() instanceof ScxmlStateType ||
						realScxmlParentState.eContainer() instanceof ScxmlScxmlType || 
						realScxmlParentState.eContainer() ==null))){
			realScxmlParentState = realScxmlParentState.eContainer();
		}
		String scxmlParentStateName = ((ScxmlStateType)realScxmlParentState).getId();
		if (scxmlParentStateName == null) return eventList;
		// search contents for transitions that target the real scxml parent state
		for (EObject possible : Find.eAllContents(scxmlContainer, ScxmlPackage.Literals.SCXML_TRANSITION_TYPE)){
			if (((ScxmlTransitionType)possible).getTarget().contains(scxmlParentStateName)){
				// find names of events for this transition
				List<String> eventNames = getEventNames(((ScxmlTransitionType)possible), machine, generatedElements);
				// find events similarly named in machine and/or generation descriptors
				for (String eventName : eventNames){
					Event ev = getOrCreateEvent(machine, true, generatedElements, eventName);
					if (ev!=null && !eventList.contains(ev)) {
						eventList.add(ev);
					}
				}
			}
		}
		if (scxmlContainer.getInitial().contains(scxmlParentStateName)){
			Event ev = getOrCreateEvent(machine, true, generatedElements, "INITIALISATION");
			eventList.add(ev);
		}
		return eventList;
	}
	
	/**
	 * This finds all the events that are elaborated by transitions that target the real parent containing super-state
	 * of the given EObject (which is assumed to be an element in an SCXML state chart)
	 * (n.b. the real parent super-state is not a state representing a parallel region but a container of the parallel)
	 * 
	 * @param scxmlEObject 			an element of a nested SCXML state chart
	 * @param machine	-	the relevant machine for the refinement level
	 * @param generatedElements - new elements to also be searched (machine must be the parent)
	 * @return
	 */
	public static List<Event> findOutgoingEvents(EObject scxmlEObject, Machine machine, List<TranslationDescriptor> generatedElements) {
		List<Event> eventList = new ArrayList<Event>();
		// find scxml parent state (i.e. the real parent state, not a region)
		EObject realScxmlParentState = scxmlEObject.eContainer();
		while (!(realScxmlParentState instanceof ScxmlStateType && 
				(		realScxmlParentState.eContainer() instanceof ScxmlStateType ||
						realScxmlParentState.eContainer() instanceof ScxmlScxmlType || 
						realScxmlParentState.eContainer() ==null))){
			realScxmlParentState = realScxmlParentState.eContainer();
		}
		
		// for transitions that source the real scxml parent state
		for (ScxmlTransitionType tr : ((ScxmlStateType)realScxmlParentState).getTransition()){
			// find names of events for this transition
			String eventName = getEventName(tr);
			// find events similarly named in machine and/or generation descriptors
			Event ev = getOrCreateEvent(machine, true, generatedElements, eventName);
			if (ev!=null && !eventList.contains(ev)) {
				eventList.add(ev);
			}
		}	

		return eventList;
	}
	
	/**
	 * This gets a list of event names that should be elaborated by an iUML-B transition when it is generated from the given
	 * INITIAL ScxmlTransition. 
	 * DO NOT USE THIS FOR NON-INITIAL TRANSITIONS -> returns null if the source is not a real ScxmlInitialType
	 * 
	 * the event names are obtained (cumulatively) by the following methods:
	 * 
	 * if the transition is in an initial state at the outer state chart level a single name, INITIALISATION, is returned 
	 * if the transition is in an initial state of a nested state chart the names of all the events that
	 *    are associated with incoming transitions to the parent state are used.
	 * 
	 * @param scxmlTransition
	 * @param machine			- the relevant machine for the refinement level (used for nested initial transitions to find incomers)
	 * @param generatedElements - new elements to also be searched (machine must be the parent)  (used for nested initial transitions to find incomers)
	 * @return
	 */
	
	public static List<String> getInitialEventNames(ScxmlTransitionType scxmlTransition, Machine machine, List<TranslationDescriptor> generatedElements){
		if (!(scxmlTransition.eContainer() instanceof ScxmlInitialType)) return null;	
		List<String> eventNames = new ArrayList<String>();
		//add initialisation events if source is an initial state

		if (getStateContainer(scxmlTransition)==null){
			eventNames.add("INITIALISATION");
		}else{
			//when source is a nested Initial we need to elaborate all the parents incomers events
			for (Event ev : findIncomerEvents(scxmlTransition, machine, generatedElements)){
				eventNames.add(ev.getName());
			}
		}
		return eventNames;
	}

	
	/**
	 * This gets the name of an event that should be elaborated by an iUML-B transition when it is generated from the given ScxmlTransition. 
	 * DO NOT USE THIS FOR INITIAL TRANSITIONS -> returns null if the source is not a real ScxmlStateType
	 * 
	 * the event name is obtained by the following methods (in order of precedence):
	 * 
	 * a) the transition has an iuml-b:label attribute
	 * b) the transition has a log label
	 * c) if none of the above provide any labels a default 'source_targets' format is used
	 * 
	 * 
	 * @param scxmlTransition
	 * @return
	 */
	public static String getEventName(ScxmlTransitionType scxmlTransition){

		EObject source = scxmlTransition.eContainer();
		
		//add initialisation events if source is an initial state
		if (!(source instanceof ScxmlStateType)) {
			return null;
		}
	
		// iuml-b label
		IumlbScxmlAdapter adapter = new IumlbScxmlAdapter(scxmlTransition);
		Object label = adapter.getAnyAttributeValue("label");
		if (label instanceof String){
			return (String) label;
		}
		
		//log label
		for (ScxmlLogType log : scxmlTransition.getLog()){
			String logLabel = log.getLabel();
			if (logLabel != null && logLabel.length()>0) {
				return logLabel;
			}
		}
			
		//if no names found default to 'source_targets' format
		String eventName =  ((ScxmlStateType)source).getId();
		for (String targetName : scxmlTransition.getTarget()){
			eventName=eventName+"_"+targetName;
		}
		return eventName;
	}
	
	/**
	 * This gets a list of event names that should be elaborated by an iUML-B transition when it is generated from the given
	 * ScxmlTransition. 
	 * 
	 * It either calls getEventName(scxmlTransition) for normal transitions (source is a proper state)
	 * or calls getInitialEventNames(scxmlTransition, machine, generatedElements) for initial transitions (source is a initial blob)
	 * for anything else returns an empty list
	 * 
	 * @param scxmlTransition
	 * @param machine	-	the relevant machine for the refinement level (Only used for nested initial transitions to find incomers)
	 * @param generatedElements - new elements to also be searched (machine must be the parent)  (Only used for nested initial transitions to find incomers)
	 * @return
	 */
	public static List<String> getEventNames(ScxmlTransitionType scxmlTransition, Machine machine, List<TranslationDescriptor> generatedElements){
		List<String> eventNames = new ArrayList<String>();
		if (scxmlTransition.eContainer() instanceof ScxmlStateType){
			eventNames.add(getEventName(scxmlTransition));
		}else if (scxmlTransition.eContainer() instanceof ScxmlInitialType) {
			eventNames.addAll(getInitialEventNames(scxmlTransition, machine, generatedElements));
		}
		return eventNames;
	}
	
	
	/**
	 * returns the containing ScxmlStateType representing the statechart that contains this transition
	 * 
	 * @param scxmlEObject
	 * @return
	 */
	public static ScxmlStateType getStateContainer(ScxmlTransitionType scxmlTransition){
		return 	scxmlTransition.eContainer() == null? null :
			(ScxmlStateType) Find.containing(ScxmlPackage.Literals.SCXML_STATE_TYPE, scxmlTransition.eContainer().eContainer());
	}
	
	
	
	/**
	 * Returns the starting refinement level for this scxml element
	 * This is given in a 'refinement' iumlb:attribute attached to the element,
	 * or, if none, the refinement level of its parent,
	 * or, if none, 0
	 * 
	 * @param scxmlElement
	 * @return
	 */
	public static int getRefinementLevel(EObject scxmlElement){
		return new IumlbScxmlAdapter(scxmlElement).getRefinementLevel();
	}
	
	/**
	 * convenience method that returns a list of data elements that are in the dataModel
	 *  attached directly to the given scxml element
	 * 
	 * @param scxmlElement
	 * @return
	 */
	public static List<EObject> getData(EObject scxmlElement){
		ArrayList<EObject> ret = new ArrayList<EObject>();
		List<EObject> dataModels = Find.eContents(scxmlElement, ScxmlPackage.Literals.SCXML_DATAMODEL_TYPE);
		for (EObject dataModel : dataModels){
			ret.addAll(Find.eContents(dataModel, ScxmlPackage.Literals.SCXML_DATA_TYPE));
		}
		return ret;
	}
	
	/**
	 * refine - this makes a new element that refines the abstract one
	 * 
	 * @param sourceElement  - contained in a resource (used as basis for constructing a URI for the abstract element)
	 * @param abstractElement - to be refined (must be contained in a machine but not necessarily in a resource)
	 * @return refined element
	 */
	public static EObject refine(EObject sourceElement, EventBElement abstractElement) {
		URI uri = EcoreUtil.getURI(sourceElement);
		uri = uri.trimFragment().trimSegments(1);
		uri = uri.appendSegment(((Machine)abstractElement.getContaining(MachinePackage.Literals.MACHINE)).getName());
		uri = uri.appendFileExtension("bum");
		uri = uri.appendFragment(abstractElement.getReference());
		AbstractElementRefiner refiner = ElementRefinerRegistry.getRegistry().getRefiner(abstractElement);
		if (refiner == null) 
			return null;
		Map<EObject,EObject> copy = refiner.refine(uri, abstractElement, null);
		EObject refinedElement = copy.get(abstractElement);
		return refinedElement;
	}

	/**
	 * @param name
	 * @return
	 */
	public static String getMachineName(ScxmlScxmlType scxmlContainer, int refinementLevel) {
		return scxmlContainer.getName()+"_"+refinementLevel;
	}


	/**
	 * gets the type set of an ScxmlData item
	 * @param scxml
	 * @return
	 */
	public static String getType(ScxmlDataType scxml) {
		String type = (String) new IumlbScxmlAdapter(scxml).getAnyAttributeValue("type");
		if (type==null || type.length()==0) {
			//fallback if no iumlb:type attribute provided
			String expr = scxml.getExpr();
			if (expr==null) type = "<null>";
			if ("true".equals(expr) || "false".equals(expr)){
				type = "BOOL";
			}
		}
		return Strings.TYPE_PREDICATE(scxml.getId(),type);
	}

	//FIXME: for debugging only
	public static void printCombi(String string, Set<ScxmlTransitionType> combi, Refinement ref){
		System.out.println("******Combi*******");
		System.out.println("Refinement Level: "+ref.level+" :: code: "+string);
		for (ScxmlTransitionType tr:combi){
			String source = 
					tr.eContainer() instanceof ScxmlStateType? ((ScxmlStateType)tr.eContainer()).getId() :
					(tr.eContainer() instanceof ScxmlInitialType? "Initial" : //((ScxmlInitialType)tr.eContainer()).getId() :
					tr.eContainer().toString());
			String target = 
					tr.getTarget() instanceof ScxmlStateType? ((ScxmlStateType)tr.eContainer()).getId() :
					(tr.getTarget() instanceof ScxmlFinalType? "Final" : //((ScxmlInitialType)tr.eContainer()).getId() :
					tr.getTarget().toString());
			System.out.println("transition:: "
					+ "trigger="+tr.getEvent()+
					", source="+source+
					", target="+target);			
		}
		System.out.println("xxxxxxxxxxxxxxxxx");
	}


}
