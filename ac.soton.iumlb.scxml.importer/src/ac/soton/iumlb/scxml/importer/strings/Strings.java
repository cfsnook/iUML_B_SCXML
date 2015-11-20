package ac.soton.iumlb.scxml.importer.strings;

import java.text.MessageFormat;

import org.eclipse.osgi.util.NLS;
import org.eclipse.sirius.tests.sample.scxml.ScxmlAssignType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlDataType;

public class Strings {

	private static final String BUNDLE_NAME = "ac.soton.iumlb.scxml.importer.strings.Strings"; //$NON-NLS-1$

	private Strings() {
		// Do not instantiate
	}
	
	// //////////////////////////////////////////////////////////
	// ADMIN and HELPER methods
	// /////////////////////////////////////////////////////////
	static {
		NLS.initializeMessages(BUNDLE_NAME, Strings.class);
	}

	/**
	 * Bind the given message's substitution locations with the given string
	 * values.
	 * 
	 * @param message
	 *            the message to be manipulated
	 * @param bindings
	 *            An array of objects to be inserted into the message
	 * @return the manipulated String
	 */
	private static String bind(String message, Object... bindings) {
		if (message == null) {
			return "NULL STRING!!!";
		}
		return MessageFormat.format(message, bindings);
	}

//	public static String CLASS_SUPERTYPE_PRED;
//	public static String CLASS_SUPERTYPE_PRED(Class cp, Class sc) {
//		return bind(CLASS_SUPERTYPE_PRED, cp.getName(), sc.getName());
//	}
//
//	public static String CLASS_SUPERTYPE_NAME;
//	public static String CLASS_SUPERTYPE_NAME(Class cp, Class sc) {
//		return bind(CLASS_SUPERTYPE_NAME, cp.getName(), sc.getName());
//	}
//	
//	public static String ASSOCIATION_PRED;
//
//	public static String ASSOCIATION_PRED(Association element) {
//		return bind(ASSOCIATION_PRED, 
//				element.getName(), 
//				element.getSource().getName(), 
//				getRelationType(element), 
//				element.getTarget().getName());
//	}
//
//	public static String REL_TOTAL_BIJECTION;				
//	public static String REL_TOTAL_SURJECTION;									
//	public static String REL_TOTAL_INJECTION;
//	public static String REL_TOTAL_FUNCTION;
//	public static String REL_PARTIAL_SURJECTION;
//	public static String REL_PARTIAL_INJECTION;
//	public static String REL_PARTIAL_FUNCTION;
//	public static String REL_TOTAL_SURJECTIVE_REL;
//	public static String REL_TOTAL_RELATION;
//	public static String REL_PARTIAL_SURJECTIVE_RELATION;
//	public static String REL_PARTIAL_RELATION;
//	
//	private static String getRelationType(EventBRelationKind element) {
//		if (element.isFunctional()){
//			if (element.isTotal()) {
//				if (element.isSurjective()){
//					if (element.isInjective()){
//						return REL_TOTAL_BIJECTION;				//total bijection
//					}else{
//						return REL_TOTAL_SURJECTION;				//total surjection						
//					}
//				}else{
//					if (element.isInjective()){
//						return REL_TOTAL_INJECTION;				//total injection
//					}else{
//						return REL_TOTAL_FUNCTION;				//total function
//					}				
//				}
//			}else{
//				if (element.isSurjective()){
//					if (element.isInjective()){
//						return REL_PARTIAL_SURJECTION;				//partial bijection - but not supported by Rodin so we have to add an extra predicate elsewhere	
//					}else{
//						return REL_PARTIAL_SURJECTION;				//partial surjection
//					}
//				}else{
//					if (element.isInjective()){
//						return REL_PARTIAL_INJECTION;				//partial injection	
//					}else{
//						return REL_PARTIAL_FUNCTION;				//partial function		
//					}					
//				}				
//			}
//		}else{
//			if (element.isTotal()) {
//				if (element.isSurjective()){
//					return REL_TOTAL_SURJECTIVE_REL;			//total surjective relation
//				}else{
//					return REL_TOTAL_RELATION;				//total relation		
//				}					
//			}else{
//				if (element.isSurjective()){
//					return REL_PARTIAL_SURJECTIVE_RELATION;				//partial surjective relation
//				}else{
//					return REL_PARTIAL_RELATION;				//partial relation		
//				}					
//			}
//		}
//	}
//	
//	public static String ASSOCIATION_INJECTIVE_PRED;
//	public static String ASSOCIATION_INJECTIVE_PRED(Association element) {
//		return bind(ASSOCIATION_INJECTIVE_PRED, 
//				element.getName(), 
//				element.getTarget().getName(),
//				element.getSource().getName());
//	}
//	
//	public static String ASSOCIATION_PRED_NAME;
//	public static String ASSOCIATION_PRED_NAME(String name) {
//		return bind(ASSOCIATION_PRED_NAME, name);
//	}
//
//	public static String ASSOCIATION_INJECTIVE_PRED_NAME;
//	public static String ASSOCIATION_INJECTIVE_PRED_NAME(String name) {
//		return bind(ASSOCIATION_INJECTIVE_PRED_NAME, name);
//	}
//	
//	
//	//CLASS ATTRIBUTE
//	public static String CLASS_ATTRIBUTE_PRED;
//	public static String CLASS_ATTRIBUTE_PRED(ClassAttribute element) {
//		return bind(CLASS_ATTRIBUTE_PRED, 
//				element.getName(), 
//				((Class)element.eContainer()).getName(), 
//				getRelationType(element), 
//				element.getTarget());
//	}
//	
//	public static String CLASS_ATTRIBUTE_INJECTIVE_PRED;
//	public static String CLASS_ATTRIBUTE_INJECTIVE_PRED(ClassAttribute element) {
//		return bind(CLASS_ATTRIBUTE_INJECTIVE_PRED, 
//				element.getName(), 
//				element.getTarget(),
//				((Class)element.eContainer()).getName());
//	}
//	
//	public static String CLASS_ATTRIBUTE_PRED_NAME;
//	public static String CLASS_ATTRIBUTE_PRED_NAME(String name) {
//		return bind(CLASS_ATTRIBUTE_PRED_NAME, name);
//	}
//
//	public static String CLASS_ATTRIBUTE_INJECTIVE_PRED_NAME;
//	public static String CLASS_ATTRIBUTE_INJECTIVE_PRED_NAME(String name) {
//		return bind(CLASS_ATTRIBUTE_INJECTIVE_PRED_NAME, name);
//	}
//	
//	public static String CLASS_CONSTRAINT_NAME;
//	public static String CLASS_CONSTRAINT_NAME(String name) {
//		return bind(CLASS_CONSTRAINT_NAME, name);
//	}
//	
//	public static String CLASS_CONSTRAINT_PRED;
//	public static String CLASS_CONSTRAINT_PRED(String selfName, String instances, String predicate) {
//		return bind(CLASS_CONSTRAINT_PRED, 
//				selfName, 
//				instances,
//				predicate);
//	}
//	
//	public static String CLASS_PARAMETER_NAME;
//	public static String CLASS_PARAMETER_NAME(String name) {
//		return bind(CLASS_PARAMETER_NAME, name);
//	}
//	
//	////guard for selecting an instance parameter
//	
//	public static String CLASS_PARAMETER_GUARD_NAME;
//	public static String CLASS_PARAMETER_GUARD_NAME(String name) {
//		return bind(CLASS_PARAMETER_GUARD_NAME, name);
//	}
//	
//	public static String CLASS_PARAMETER_CONSTRUCTOR_GUARD_PRED;
//	public static String CLASS_PARAMETER_CONSTRUCTOR_GUARD_PRED(String selfName, String instances) {
//		return bind(CLASS_PARAMETER_CONSTRUCTOR_GUARD_PRED, 
//				selfName,
//				instances);
//	}
//	
//	public static String CLASS_PARAMETER_GUARD_PRED;
//	public static String CLASS_PARAMETER_GUARD_PRED(String selfName, String instances) {
//		return bind(CLASS_PARAMETER_GUARD_PRED, 
//				selfName,
//				instances);
//	}
//	
//	///action for constructing/destructing instances
//	
//	public static String CLASS_PARAMETER_ACTION_NAME;
//	public static String CLASS_PARAMETER_ACTION_NAME(String name) {
//		return bind(CLASS_PARAMETER_ACTION_NAME, name);
//	}
//	
//	public static String CLASS_PARAMETER_CONSTRUCTOR_ACTION_EXPR;
//	public static String CLASS_PARAMETER_CONSTRUCTOR_ACTION_EXPR(String selfName, String instances) {
//		return bind(CLASS_PARAMETER_CONSTRUCTOR_ACTION_EXPR, 
//				selfName,
//				instances);
//	}
//	
//	public static String CLASS_PARAMETER_DESTRUCTOR_ACTION_EXPR;
//	public static String CLASS_PARAMETER_DESTRUCTOR_ACTION_EXPR(String selfName, String instances) {
//		return bind(CLASS_PARAMETER_DESTRUCTOR_ACTION_EXPR, 
//				selfName,
//				instances);
//	}
//	
//	///// extra user parameters in methods
//	
//	public static String USER_PARAMETER_TYPE_GUARD_NAME;
//	public static String USER_PARAMETER_TYPE_GUARD_NAME(TypedParameter p) {
//		return bind(USER_PARAMETER_TYPE_GUARD_NAME, p.getName());
//	}
//	
//	public static String USER_PARAMETER_TYPE_GUARD_PRED;
//	public static String USER_PARAMETER_TYPE_GUARD_PRED(TypedParameter p) {
//		return bind(USER_PARAMETER_TYPE_GUARD_PRED, 
//				p.getName(), 
//				p.getType());
//	}
//	
//	/// intialisation to intialValue of associations/attributes
//	/// ....in fixed classes
//	public static String INITIALISATION_NAME;
//	public static String INITIALISATION_NAME(EventBNamed e) {
//		return bind(INITIALISATION_NAME, e.getName());
//	}
//
//	public static String ATTR_VALUE_INITIALISATION_ACTION_EXPR;
//	public static String ATTR_VALUE_INITIALISATION_ACTION_EXPR(EventBNamed e) {
//		return bind(ATTR_VALUE_INITIALISATION_ACTION_EXPR, 
//				e.getName(), 
//				((EventBNamed) e.eContainer()).getName(), 
//				((EventBInitialisable)e).getInitialValue());
//	}
//	//initialisation to empty set of associations/attributes etc.
//	//... in variable classes
//	public static String EMPTY_INITIALISATION_ACTION_EXPR;
//	public static String EMPTY_INITIALISATION_ACTION_EXPR(EventBNamed e) {
//		return bind(EMPTY_INITIALISATION_ACTION_EXPR, e.getName());
//	}
//	
//	/// ....in constructors of variable classes	
//	public static String ATTRIBUTE_INITIALISATION_ACTION_NAME;
//	public static String ATTRIBUTE_INITIALISATION_ACTION_NAME(String name) {
//		return bind(ATTRIBUTE_INITIALISATION_ACTION_NAME, name);
//	}
//
//	public static String ATTRIBUTE_INITIALISATION_ACTION_EXPR;
//	public static String ATTRIBUTE_INITIALISATION_ACTION_EXPR(String name, String selfName, String intialValue) {
//		return bind(ATTRIBUTE_INITIALISATION_ACTION_EXPR, 
//				name, 
//				selfName, 
//				intialValue);
//	}
//	public static String ATTRIBUTE_DESTRUCTION_ACTION_NAME;
//	public static String ATTRIBUTE_DESTRUCTION_ACTION_NAME(String name) {
//		return bind(ATTRIBUTE_DESTRUCTION_ACTION_NAME, name);
//	}
//
//	public static String ATTRIBUTE_DESTRUCTION_ACTION_EXPR;
//	public static String ATTRIBUTE_DESTRUCTION_ACTION_EXPR(String name, String selfName) {
//		return bind(ATTRIBUTE_DESTRUCTION_ACTION_EXPR, 
//				name, 
//				selfName);
//	}
	
	
	////// methods for dealing with the SCXML action language
	
	public static  String LOCATION(ScxmlDataType scxml) {
		String location = convertLocation(scxml.getId());
		return location;
	}
	
	public static  String INIT_ACTION(ScxmlDataType scxml) {
		String location = convertLocation(scxml.getId());
		String expression = convertBoolVals(scxml.getExpr());
		return location+" \u2254 "+expression;
	}

	public static String TYPE_PREDICATE(ScxmlDataType scxml) {
		String location = convertLocation(scxml.getId());
		String type = convertType(scxml.getExpr());
		return location+" \u2208 "+type;
	}

	public static String ASSIGN_ACTION(ScxmlAssignType assign) {
		String location = convertLocation(assign.getLocation());
		String expression = convertBoolVals(assign.getExpr());
		return (location+" \u2254 "+expression);
		
	}
	
	public static String COND_GUARD(String cond) {
		return removeBrackets(
				convertLocation(
					convertBoolVals(
						convertDoubleEquals(
								cond))));
	}
	
	////
	
	private static String convertDoubleEquals(String expr) {
		return expr.replaceAll("==", "=");
	}

	private static String removeBrackets(String expr) {
		if (expr.startsWith("[") && expr.endsWith("]")) {
			return expr.substring(1, expr.length()-1);
		}else {
			return expr;
		}
	}

	private static String convertBoolVals(String expr) {
		return expr.replaceAll("\\btrue\\b", "TRUE").replaceAll("\\bfalse\\b", "FALSE");
	}

	private static String convertLocation(String location){
		return location.replaceAll("\\.", "_");
	}
	
	private static String convertType(String expr) {
		if ("true".equals(expr) || "false".equals(expr)){
			return "BOOL";
		}
		return expr;
	}

	public static String INV_PREDICATE(String anticedent, ScxmlDataType scxml) {
		String predicate = convertLocation(convertBoolVals(convertDoubleEquals(scxml.getExpr())));
		if (anticedent != null){
			predicate = anticedent + "("+ predicate+ ")";
		}
		return predicate;
	}

}
