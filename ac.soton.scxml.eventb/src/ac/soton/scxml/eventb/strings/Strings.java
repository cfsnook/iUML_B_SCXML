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
package ac.soton.scxml.eventb.strings;

import java.text.MessageFormat;

import org.eclipse.osgi.util.NLS;
import org.eclipse.sirius.tests.sample.scxml.ScxmlAssignType;
import org.eclipse.sirius.tests.sample.scxml.ScxmlDataType;

import ac.soton.scxml.eventb.rules.Trigger;

public class Strings {

	private static final String BUNDLE_NAME = "ac.soton.scxml.eventb.strings.Strings"; //$NON-NLS-1$

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
	
	////// methods for dealing with the SCXML action language
	
	public static  String LOCATION(ScxmlDataType scxml) {
		return convertLocation(scxml.getId());
	}
	
	public static  String INIT_ACTION;
	public static  String INIT_ACTION(ScxmlDataType scxml) {
		return bind(INIT_ACTION,
				convertLocation(scxml.getId()),
				convertBoolVals(scxml.getExpr())
				);
	}

	public static String TYPE_PREDICATE;
	public static String TYPE_PREDICATE(String id, String type) {
		return bind(TYPE_PREDICATE,
				convertLocation(id),
				type
				);
	}

	public static String ASSIGN_ACTION;


	public static String ASSIGN_ACTION(ScxmlAssignType assign) {
		return bind(ASSIGN_ACTION,
				convertLocation(assign.getLocation()),
				convertBoolVals(assign.getExpr())
				);
	}
	
	public static String COND_GUARD(String cond) {
		return removeBrackets(
				convertLocation(
					convertBoolVals(
						convertDoubleEquals(
								cond))));
	}
	
	public static String INV_PREDICATE(String anticedent, ScxmlDataType scxml) {
		String predicate = INV_PREDICATE(scxml.getExpr());
		if (anticedent != null){
			predicate = anticedent + "("+ predicate+ ")";
		}
		return predicate;
	}
	
	public static String INV_PREDICATE(String predicate) {
		String pred = convertLocation(
				convertBoolVals(
				convertDoubleEquals(
				convertIn(		predicate)
				)));
		return pred;
	}
	
	////
	
	/**
	 * @param predicate
	 * @return
	 */
	private static String convertIn(String predicate) {
		return predicate==null?  "<null>": predicate.replaceAll(":", "\u2208");
	}

	private static String convertDoubleEquals(String expr) {
		return expr==null? "<null>": expr.replaceAll("==", "=");
	}

	private static String removeBrackets(String expr) {
		if (expr==null) return "<null>";
		if (expr.startsWith("[") && expr.endsWith("]")) {
			return expr.substring(1, expr.length()-1);
		}else {
			return expr;
		}
	}

	private static String convertBoolVals(String expr) {
		return expr==null? "<null>": expr.replaceAll("\\btrue\\b", "TRUE").replaceAll("\\bfalse\\b", "FALSE");
	}

	private static String convertLocation(String location){
		return location==null? "<null>": location.replaceAll("\\.", "_");
	}

	/**
	 * @param string
	 * @return
	 */
	public static String ACT_ASSIGN(String action) {
		// TODO Auto-generated method stub
		return action==null? "<null>": action.replaceAll(":=", "\u2254"); 
	}
	

	public static  String generatedFromFileComment(String fileName){
		return "(generated from SCXML file: "+fileName+")";
	}
	
	public static final  String basisContextName = "basis";	
	public static final  String triggerSetName = "SCXML_TRIGGER";
	public static final  String externalTriggersName = "SCXML_FutureExternalTrigger";
	public static final  String internalTriggersName = "SCXML_FutureInternalTrigger";
	public static final  String triggerPartitionAxiomName = "axm1";
	public static final  String triggerPartitionAxiomPredicate = "partition("+triggerSetName+","+internalTriggersName+","+externalTriggersName+")";	

	// axioms added to the extended context for each refinement level

	public static String externalTriggersName(int i) {
		return externalTriggersName+i;
	}
	
	public static String internalTriggersName(int i) {
		return internalTriggersName+i;
	}
	
	public static String externalTriggerAxiomName(int i) {
		return externalTriggersName+i;
	}
	public static String internalTriggerAxiomName(int i) {
		return internalTriggersName+i;
	}
	public static String externalTriggerDefinitionAxiomPredicate(int i, String externals) {
		String previous = i==0? externalTriggersName : externalTriggersName+(i-1);
		String newTriggers = externals == null? "" : ",{"+externals+"}";
		return "partition("+previous+","+externalTriggersName+i+newTriggers+")";
	}
	public static String internalTriggerDefinitionAxiomPredicate(int i, String internals) {
		String previous = i==0? internalTriggersName : internalTriggersName+(i-1);
		String newTriggers = internals == null? "" : ",{"+internals+"}";
		return "partition("+previous+","+internalTriggersName+i+newTriggers+")";
	}
	
	
	public static final  String basisMachineName = "basis";
	public static final  String externalQueueName = "SCXML_eq";
	public static final  String internalQueueName = "SCXML_iq";
	public static final  String completionFlagName = "SCXML_uc";
	public static final  String externalQueueTypeName = "typeof_"+externalQueueName;
	public static final  String externalQueueTypePredicate = externalQueueName+" \u2286 "+externalTriggersName;
	public static final  String internalQueueTypeName = "typeof_"+internalQueueName;
	public static final  String internalQueueTypePredicate = internalQueueName+" \u2286 "+internalTriggersName;
	public static final  String queueDisjunctionName = "disjointQueues";
	public static final  String queueDisjunctionPredicate = internalQueueName+" \u2229 "+externalQueueName+"= \u2205";
	public static final  String completionFlagTypeName = "typeof_"+completionFlagName;
	public static final  String completionFlagTypePredicate = completionFlagName+" \u2208 BOOL";
	
	public static final  String initExternalQName = "init_"+externalQueueName;
	public static final  String initExternalQAction = externalQueueName+" \u2254 \u2205";	
	public static final  String initInternalQName = "init_"+internalQueueName;
	public static final  String initInternalQAction = internalQueueName+" \u2254 \u2205";
	public static final  String initCompletionFlagName = "init_"+completionFlagName;
	public static final  String initCompletionFlagAction = completionFlagName+" \u2254 FALSE";
	//e1
	public static final  String futureExternalTriggersEventName = "SCXML_futureExternalTrigger";
	public static final  String raisedExternalTriggersParameterName = "SCXML_raisedTriggers";
	public static final  String raisedExternalTriggersParameterComment = "";
	public static final  String e1_g1_Name = "typeof_"+raisedExternalTriggersParameterName;
	public static final  String e1_g1_Predicate = raisedExternalTriggersParameterName+" \u2286 "+externalTriggersName;
	public static final  String e1_g1_Comment = "";
	public static final  String e1_a1_Name = "SCXML_raiseExternalTriggers";
	public static final  String e1_a1_Action = externalQueueName+" ≔ "+externalQueueName+" \u222a "+raisedExternalTriggersParameterName;
	public static final  String e1_a1_Comment = "";
	
	//Strings for making a guard to go in a refinement of the future external trigger event
	// so that it raises a specific trigger 
	public static final String specificRaisedExternalTriggerGuardName = "raisedExternalTrigger";
	public static String specificRaisedExternalTriggerGuardPredicate(String raised) {
		return raisedExternalTriggersParameterName+" = {"+raised+"}";
	}
	public static final String specificRaisedExternalTriggerGuardComment = "";

	
	//Strings
	//public static final  String futureInternalTriggersEventName = "SCXML_futureInternalTrigger";	
	public static final  String raisedInternalTriggersParameterName = "SCXML_raisedTriggers";
	public static final  String raisedInternalTriggersParameterComment = "";
	public static final  String raisedInternalTriggersGuardName = "typeof_"+raisedInternalTriggersParameterName;
	public static final  String raisedInternalTriggersGuardPredicate = raisedInternalTriggersParameterName+" \u2286 "+internalTriggersName;
	public static final  String raisedInternalTriggersGuardComment = "";
	public static final  String raisedInternalTriggersActionName = "SCXML_raiseInternalTriggers";
	public static final  String raisedInternalTriggersActionAction = internalQueueName+" ≔ "+internalQueueName+" \u222a "+raisedInternalTriggersParameterName;
	public static final  String raisedInternalTriggersActionComment = "";
	
	//Strings for making a guard to go in a refinement of the future external trigger event
	// so that it raises a specific trigger 
	public static final String specificRaisedInternalTriggersGuardName = "raisedInternalTriggers";
	
	/**
	 * @param raiseList
	 * @param finalised
	 * @return
	 */
	public static String specificRaisedInternalTriggersGuardPredicate(String raiseList, boolean finalised) {
		//String test = (finalised? raiseList + " = " : raiseList + " \u2286 ") + raisedInternalTriggersParameterName;
		return (finalised? raiseList + " = " : raiseList + " \u2286 ") + raisedInternalTriggersParameterName;
	}
	
	public static final String specificRaisedInternalTriggersGuardComment = "";
	
	//e3
	public static final  String consumeExternalTriggerEventName = "SCXML_futureExternalTransitionSet";
	public static final  String consumedExternalTriggerParameterName = "SCXML_et";
	public static final  String consumedExternalTriggerParameterComment = "";
	public static final  String e3_g1_Name = "typeof_"+consumedExternalTriggerParameterName;
	public static final  String e3_g1_Predicate = consumedExternalTriggerParameterName+" ∈ "+externalQueueName;
	public static final  String e3_g1_Comment = "";
	public static final  String e3_g2_Name = "SCXML_internalQEmpty";
	public static final  String e3_g2_Predicate = internalQueueName+" = \u2205";
	public static final  String e3_g2_Comment = "";
	public static final  String e3_g3_Name = "SCXML_isComplete";
	public static final  String e3_g3_Predicate = completionFlagName+" = TRUE";
	public static final  String e3_g3_Comment = "";
	public static final  String e3_a1_Name = "SCXML_notComplete";
	public static final  String e3_a1_Action = completionFlagName+" \u2254 FALSE";
	public static final  String e3_a1_Comment = "";
	public static final  String e3_a2_Name = "SCXML_consumeExternalTrigger";
	public static final  String e3_a2_Action = externalQueueName+" \u2254 "+externalQueueName+" \u2216 {"+consumedExternalTriggerParameterName+"}";
	public static final  String e3_a2_Comment = "";
	//e4
	public static final  String consumeInternalTriggerEventName = "SCXML_futureInternalTransitionSet";
	public static final  String consumedInternalTriggerParameterName = "SCXML_it";
	public static final  String consumedInternalTriggerParameterComment = "";
	public static final  String e4_g1_Name = "typeof_"+consumedInternalTriggerParameterName;
	public static final  String e4_g1_Predicate = consumedInternalTriggerParameterName+"  ∈ "+internalQueueName;
	public static final  String e4_g1_Comment = "";
	public static final  String e4_g2_Name = "SCXML_isComplete";
	public static final  String e4_g2_Predicate = completionFlagName+" = TRUE";
	public static final  String e4_g2_Comment = "";
	public static final  String e4_a1_Name = "SCXML_notComplete";
	public static final  String e4_a1_Action = completionFlagName+" \u2254 FALSE";
	public static final  String e4_a1_Comment = "";
	public static final  String e4_a2_Name = "SCXML_consumeInternalTrigger";
	public static final  String e4_a2_Action = internalQueueName+" \u2254 ("+internalQueueName+" \u222a "+raisedInternalTriggersParameterName+") \u2216 {"+consumedInternalTriggerParameterName+"}";
	public static final  String e4_a2_Comment = "";

	//for use in events that define a specific trigger:
	public static final String trigGd_Name = "SCXML_trigger";
	public static String trigGd_Predicate(Trigger t) {
		String pred = "";
		pred = t.isInternal()? consumedInternalTriggerParameterName : consumedExternalTriggerParameterName ;
		pred = pred + " = " + t.getName();
		return pred;
	}
	public static final String trigGd_Comment = "triggered transition";
	
	//e5
	public static final  String untriggeredEventName = "SCXML_futureUntriggeredTransitionSet";
	public static final  String e5_g1_Name = "SCXML_isNotComplete";
	public static final  String e5_g1_Predicate = completionFlagName+" = FALSE";
	public static final  String e5_g1_Comment = "";
	public static final  String e5_a1_Name = "SCXML_NotComplete";
	public static final  String e5_a1_Action = completionFlagName+" \u2254 FALSE";
	public static final  String e5_a1_Comment = "";
	//e6
	public static final  String completionEventName = "SCXML_Completion";
	public static final  String e6_g1_Name = "SCXML_isNotComplete";
	public static final  String e6_g1_Predicate = completionFlagName+" = FALSE";
	public static final  String e6_g1_Comment = "";
	public static final  String e6_a1_Name = "SCXML_Complete";
	public static final  String e6_a1_Action = completionFlagName+" \u2254 TRUE";
	public static final  String e6_a1_Comment = "";
	
}
