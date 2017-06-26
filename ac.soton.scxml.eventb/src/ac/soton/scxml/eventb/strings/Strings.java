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
	




}
