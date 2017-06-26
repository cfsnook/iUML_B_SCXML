/**
 * 
 */
package ac.soton.scxml.eventb.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <p>
 * 
 * </p>
 * 
 * @author cfs
 * @version
 * @see
 * @since
 */
public class IumlbScxmlAdapter {
	
	  /**
	   * The adapted SCXML element.
	   */
	  protected EObject target = null;

		 /**
	   * Creates an instance.
	   */
	  public IumlbScxmlAdapter()
	  {
	    //adapt(target);
	  }
	  
	 /**
	   * Creates an instance that adapts target eObject
	   */
	  public IumlbScxmlAdapter(EObject target)
	  {
	    adapt(target);
	  }

	  /**
	   * makes this instance adapt the given target
	   * @param target
	   */
	  public IumlbScxmlAdapter adapt(EObject target){
		    this.target = target;
		    return this;
	  }
	  
	 /**
	  * returns the value of the attribute with the given name contained in
	  * the anyAttribute feature map...
	  * ... or null if no such attribute is found.
	  * @param attributeName
	  * @return attribute value object
	  */
	  
	  public Object getAnyAttributeValue(String attributeName){
			EStructuralFeature anyAttributeFeature  = target.eClass().getEStructuralFeature("anyAttribute");
			if (anyAttributeFeature==null) return null;
			FeatureMap fm = (FeatureMap) target.eGet(anyAttributeFeature);
			for (int i=0; i< fm.size(); i++){
				EStructuralFeature sf = fm.getEStructuralFeature(i);
				if (attributeName.equals(sf.getName())){
					return fm.getValue(i);
				}
			}
			return null;	  
	  }
	  
	 /**
	  * returns a list of adapters wrapping EObjects that are the value of the features with the given name contained in
	  * the any feature map...
	  * ... or an empty list if none are found.
	  * @param attributeName
	  * @return attribute value object
	  */
	  
	  public List<IumlbScxmlAdapter> getAnyChildren(String featureName){
			List<IumlbScxmlAdapter> ret = new ArrayList<IumlbScxmlAdapter>();
			EStructuralFeature anyFeature  = target.eClass().getEStructuralFeature("any");
			if (anyFeature==null) return ret;
			FeatureMap fm = (FeatureMap) target.eGet(anyFeature);
			for (int i=0; i< fm.size(); i++){
				EStructuralFeature sf = fm.getEStructuralFeature(i);
				if (featureName.equals(sf.getName()) && fm.getValue(i) instanceof EObject){
					ret.add(new IumlbScxmlAdapter((EObject)fm.getValue(i)));
				}
			}
			return ret;	  
	  }
	  
	  
	/**
	 * Returns the starting refinement level for this SCXML element
	 * This is given in a 'refinement' iumlb:attribute attached to the element,
	 * or, if none, the refinement level of its parent,
	 * or, if none, 0
	 * 
	 * @param scxmlElement
	 * @return
	 */
	public int getRefinementLevel(){
		Object level = getAnyAttributeValue("refinement");
		if (level instanceof String) {
			return Integer.parseInt((String)level);
		}else{
			if (target.eContainer()==null){
				return 0;
			}else{
				EObject oldTarget = target;
				int refLevel = this.adapt(target.eContainer()).getRefinementLevel();
				this.adapt(oldTarget);
				return refLevel; //new IumlbScxmlAdapter(target.eContainer()).getRefinementLevel();
			}
		}
	}
	
	/**
	 * Returns the starting refinement level for this SCXML element
	 * This is given in a 'refinement' iumlb:attribute attached to the element,
	 * if none, or the attribute string doesn't parse as an int, returns -1.
	 * 
	 * @param scxmlElement
	 * @return
	 */
	public int getBasicRefinementLevel(){		
		Object level = getAnyAttributeValue("refinement");
		if (level instanceof String) {
			try {
				return Integer.parseInt((String)level);
			}catch (Exception e){
				return -1;
			}
		}else{
			return -1;
		}
	}

	
	public List<IumlbScxmlAdapter> getinvariants() {		
		return getAnyChildren("invariant");
	}

	/**
	 * @return
	 */
	public List<IumlbScxmlAdapter> getGuards() {
		return getAnyChildren("guard");	
	}
		
}
