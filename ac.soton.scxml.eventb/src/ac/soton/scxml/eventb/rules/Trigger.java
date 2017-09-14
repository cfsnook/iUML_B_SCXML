/**
 * 
 */
package ac.soton.scxml.eventb.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.tests.sample.scxml.ScxmlInitialType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlParallelType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlRaiseType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlStateType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlTransitionType;

import ac.soton.scxml.eventb.utils.Utils;

/**
 * <p>
 * Represents an SCXML trigger.
 * Triggers in SCXML are implicitly defined. They do not have a meta-class in the EMF model and 
 * are only defined by a String property (called event) of a transition or a string property of a Raises.
 * This class can be used to collate and subsequently retrieve information about triggers.
 * 
 * Each instance of Trigger has two modes:
 * Initially it is not finalised and its records can be built up 
 * As soon as any of the getter or query methods are used (except getName) it switches mode and its records can no longer
 * be added to. This ensures that the Trigger is used in a consistent way.
 * 
 * </p>
 * 
 * @author cfs
 * @version
 * @see
 * @since
 */
public class Trigger {

	protected String name;
	protected Integer refinementLevel = null;
	protected Set<ScxmlTransitionType> triggeredTransitions = new HashSet<ScxmlTransitionType>();
	protected Set<ScxmlTransitionType> raisedByTransitions = new HashSet<ScxmlTransitionType>();	
	protected Map<Integer,Set<Set<ScxmlTransitionType>>> combinations = null; //new HashSet<Set<ScxmlTransitionType>>();
	protected boolean finalised = false;
	
	public Trigger (String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	private boolean updateLevel(int newLevel) throws Exception{
		if (finalised == true) throw new Exception("Attempt to update Trigger that has been finalised");
		if (refinementLevel==null || newLevel < refinementLevel){
			refinementLevel = newLevel;
			return true;
		} else {
			return false;
		}
	}
	
	public boolean addTriggeredTransition(ScxmlTransitionType newTransition) throws Exception{
		if (finalised == true) throw new Exception("Attempt to update Trigger that has been finalised");
		updateLevel (Utils.getRefinementLevel(newTransition));
		return triggeredTransitions.add(newTransition);
	}
	
	public boolean addRaisedByTransition(ScxmlRaiseType raise) throws Exception{
		if (finalised == true) throw new Exception("Attempt to update Trigger that has been finalised");
		ScxmlTransitionType newTransition = (ScxmlTransitionType) raise.eContainer();
		updateLevel (Utils.getRefinementLevel(raise));
		return raisedByTransitions.add(newTransition);
	}
	
//	public boolean addTransitionCombination(Set<ScxmlTransitionType> newTransitionCombination) throws Exception{
//		if (finalised == true) throw new Exception("Attempt to update Trigger that has been finalised");
//		return combinations.add(newTransitionCombination);
//	}
	
//	public void finalise() throws Exception{
//		if (finalised == true) throw new Exception("Attempt to update Trigger that has been finalised");
//		finalised = true;
//	}

	public Integer getRefinementLevel() {
		finalised = true;
		return refinementLevel;
	}
	
	public Set<ScxmlTransitionType> getTriggeredTransitions() {
		finalised = true;
		return Collections.unmodifiableSet(triggeredTransitions);
	}
	
	public Set<ScxmlTransitionType> getRaisedByTransitions() {
		finalised = true;
		return Collections.unmodifiableSet(raisedByTransitions);
	}
	
//	public Map<Integer,Set<ScxmlTransitionType>> getTransitionCombinations()	throws Exception{
//		if (finalised == false) throw new Exception("Attempt to read Trigger that has not been finalised");
//		if (combinations==null){
//			
//		}
//		return Collections.unmodifiableMap(combinations);
//	}
	
	public Set<Set<ScxmlTransitionType>> getTransitionCombinations(int level) {
		finalised = true;
		if (combinations==null){
			combinations= new HashMap<Integer,Set<Set<ScxmlTransitionType>>>();
			//calculate all the possible combinations irrespective of refinements
			Set<Set<ScxmlTransitionType>> rawCombinations=
					getCombinations(new ArrayList<ScxmlTransitionType>(getTriggeredTransitions()));
			//store in refinement level mapping
			for (Set<ScxmlTransitionType> combination : rawCombinations){
				int refLevel = -1;
				int depth = -1;
				for (ScxmlTransitionType tr : combination){
					int trRefLevel = Utils.getRefinementLevel(tr);
					if (trRefLevel>refLevel){
						refLevel = trRefLevel;
					}
					if (depth==-1){
						depth = Utils.getRefinementDepth(tr);	//this is done here because we need some element of the model to get access to it
					}
				}
				Set<Set<ScxmlTransitionType>> temp;
				for (int l=refLevel; l<depth+1; l++){
					if (combinations.containsKey(l)){
						temp = combinations.get(l);
					}else{
						temp = new HashSet<Set<ScxmlTransitionType>>();
					}
					temp.add(combination);
					combinations.put(l, temp);
				}
			}			
		}
		return Collections.unmodifiableSet(combinations.get(level));
	}
		
	public boolean isExternal() {
		finalised = true;
		return raisedByTransitions.size()==0;
	}
	
	public boolean isInternal() {
		finalised = true;
		return raisedByTransitions.size()>0;
	}
	
	public boolean equals(Trigger other){
		return other.name == this.name;
	}
	
	//// code to calculate combinations
	
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
	
	
	
}
