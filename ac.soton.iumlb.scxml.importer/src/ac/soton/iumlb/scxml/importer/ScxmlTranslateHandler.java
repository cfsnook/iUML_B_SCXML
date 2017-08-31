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

package ac.soton.iumlb.scxml.importer;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.sirius.tests.sample.scxml.DocumentRoot;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.persistence.EMFRodinDB;

import ac.soton.emf.translator.handler.TranslateHandler;
import ac.soton.eventb.emf.diagrams.generator.actions.GenerateAllHandler;

/**
 * <p>
 * TranslateHandler is overridden to add post processing that generates all iUML-B diagrams
 *  into Event-B automatically
 * </p>
 * 
 * @author cfs
 * @version
 * @see
 * @since
 */
public class ScxmlTranslateHandler extends TranslateHandler {	
	
	/**
	 * This is overridden to invoke the iUML-B translators after SCXML translation has finished
	 * 
	 * @param sourceElement
	 * @param commandId
	 * @param monitor
	 * @throws CoreException 
	 */
	protected void postProcessing(EObject sourceElement, String commandId, IProgressMonitor monitor) throws Exception {
		if (sourceElement instanceof DocumentRoot){
			IProject project = WorkspaceSynchronizer.getFile(sourceElement.eResource()).getProject();
			EMFRodinDB emfRodinDB = new EMFRodinDB();
			List<EventBNamedCommentedComponentElement> components = emfRodinDB.loadAllComponents(project.getName());
			for (EventBNamedCommentedComponentElement cp : components){				
				//Generate all diagrams in this component
				GenerateAllHandler genAll = new GenerateAllHandler();
				genAll.generateAllDiagrams(cp, emfRodinDB.getEditingDomain(), null);
			}
		}		
		monitor.done();
	}
	
}
