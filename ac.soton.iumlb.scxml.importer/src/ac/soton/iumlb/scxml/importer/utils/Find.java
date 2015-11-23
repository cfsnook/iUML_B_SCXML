/**
 * Copyright (c) 2015 - University of Southampton.
 * All rights reserved. This program and the accompanying materials  are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 *
 * $Id$
 */
package ac.soton.iumlb.scxml.importer.utils;

import org.eclipse.emf.common.util.EList;
import org.eventb.emf.core.EventBNamed;


/**
 * Convenience methods for finding things needed in translator Rules
 * 
 * @author cfs
 *
 */

public class Find extends ac.soton.eventb.emf.diagrams.importExport.utils.Find {
	
/**
 * Find by name, an element in a list of EventBNamed elements
 * @param collection
 * @param name
 * @return
 */
	public static EventBNamed named(EList<? extends EventBNamed> collection, String name){
		for (EventBNamed element : collection){
			if (name.equals(element.getName())) return element;
		}
		return null;
	}
	

}
