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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.sirius.tests.sample.scxml.DocumentRoot;
import org.eventb.emf.core.AbstractExtension;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.persistence.EMFRodinDB;

import ac.soton.emf.translator.eventb.handler.EventBTranslateHandler;
import ac.soton.eventb.emf.diagrams.generator.commands.TranslateAllCommand;
import ac.soton.eventb.statemachines.AbstractNode;
import ac.soton.eventb.statemachines.Final;
import ac.soton.eventb.statemachines.Initial;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.Transition;

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
public class ScxmlTranslateHandler extends EventBTranslateHandler {	
	
	/**
	 * This is overridden to invoke the iUML-B translators after SCXML translation has finished
	 * 
	 * @param sourceElement
	 * @param commandId
	 * @param monitor
	 * @throws CoreException 
	 */
	protected void postProcessing(EObject sourceElement, String commandId, IProgressMonitor monitor) throws Exception {
		//TODO: add a return status to pre/post processing
		IStatus status = Status.OK_STATUS;
		if (sourceElement instanceof DocumentRoot){
			IProject project = WorkspaceSynchronizer.getFile(sourceElement.eResource()).getProject();
			EMFRodinDB emfRodinDB = new EMFRodinDB();
			List<EventBNamedCommentedComponentElement> components = emfRodinDB.loadAllComponents(project.getName());
			for (EventBNamedCommentedComponentElement cp : components){
				
				//first fix the initial transition elaboration which is not done in the scxml translator
				for (AbstractExtension ext : cp.getExtensions()){
					if (ext instanceof Statemachine){
						processStatemachineInitialTransitions((Statemachine)ext);
					}
				}
				
				TranslateAllCommand translateAllCmd = new TranslateAllCommand(emfRodinDB.getEditingDomain(),cp);
				if (translateAllCmd.canExecute()){
					status = translateAllCmd.execute(null, null);
				}
			
			}
		}		
		monitor.done();
	}

	/**
	 * for any initial transitions of sub-state-machines, adds elaboration of all the events
	 *  elaborated by incoming transitions to the parent state.
	 * 
	 * @param sm
	 * @return flag indicates whether anything was changed
	 */
	private boolean processStatemachineInitialTransitions(Statemachine sm) {
		boolean dirty = false;
		 EList<Transition> transitions = sm.getTransitions(); //ext.getAllContained(StatemachinesPackage.Literals.TRANSITION, true);
		 for (Transition tr : transitions){
			 if (tr.eContainer().eContainer() instanceof State){
				 State parent = (State) tr.eContainer().eContainer();
				 if (tr.getSource() instanceof Initial){
					 for (Transition in : parent.getIncoming()){
						tr.getElaborates().addAll(in.getElaborates());
						dirty = true;
					 }
				 }else if (tr.getTarget() instanceof Final){
					 for (Transition out : parent.getOutgoing()){
					 	tr.getElaborates().addAll(out.getElaborates());
					 	dirty = true;
					 }
				 }
			 }
		 }
		 for (AbstractNode an : sm.getNodes()){
			 if (an instanceof State){
				 EList<Statemachine> sms = ((State)an).getStatemachines();
				 for (Statemachine ssm : sms){
					 dirty = processStatemachineInitialTransitions(ssm) | dirty;
				 }
			 }
			 
		 }
		 return dirty;
	}
	
}
