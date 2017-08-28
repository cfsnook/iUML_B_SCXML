/**
 * 
 */
package ac.soton.scxml.eventb;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.sirius.tests.sample.scxml.DocumentRoot;
import org.eclipse.ui.progress.UIJob;
import org.eventb.emf.core.AbstractExtension;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.persistence.EMFRodinDB;

import ac.soton.emf.translator.handler.TranslateHandler;
import ac.soton.eventb.emf.diagrams.generator.actions.GenerateAllHandler;
import ac.soton.eventb.statemachines.AbstractNode;
import ac.soton.eventb.statemachines.Final;
import ac.soton.eventb.statemachines.Initial;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;

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
public class ScxmlTranslateHandler extends TranslateHandler {
	
	private static final String QUALIFIER = "ac.soton.scxml.eventb";
	private static final QualifiedName COMPONENT = new QualifiedName(QUALIFIER, "COMPONENT");
	
	
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
//				boolean dirty = false;
//				if (cp.eIsProxy()){
//					EcoreUtil.resolve(cp, emfRodinDB.getResourceSet());
//				}

//				for (AbstractExtension ext : cp.getExtensions()){
//					if (ext instanceof Statemachine){
//						processStatemachine((Statemachine)ext);
//					}
//				}
//				if (dirty){
//					IFile file = WorkspaceSynchronizer.getFile(cp.eResource());
//					if (file!=null){
//						URI fileURI = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
//						emfRodinDB.saveResource(fileURI, cp);
//					}
//				}
				//schedule a job to generate all diagrams in the component
				//need to run this in the UI thread due to the messages
				UIJob generateAllJob = new UIJob("Generating all iUML-B diagrams in component: "+cp.getName()) {
				      public IStatus runInUIThread(IProgressMonitor monitor) {				    	  
						EMFRodinDB emfRodinDB = new EMFRodinDB();
						EventBNamedCommentedComponentElement component = (EventBNamedCommentedComponentElement)getProperty(COMPONENT);
//						if (component.eIsProxy()){
//							EcoreUtil.resolve(component, emfRodinDB.getResourceSet());
//						}
						
						//first fix the initial transition elaboration which is not done in the scxml translator
						for (AbstractExtension ext : component.getExtensions()){
							if (ext instanceof Statemachine){
								processStatemachine((Statemachine)ext);
							}
						}
					
//						IFile file = WorkspaceSynchronizer.getFile(component.eResource());
//						if (file!=null){
//							URI fileURI = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
//							emfRodinDB.saveResource(fileURI, component);
//						}
						GenerateAllHandler genAll = new GenerateAllHandler();
						genAll.generateAllDiagrams(component, shell, emfRodinDB.getEditingDomain(), null);	
				        return Status.OK_STATUS;
				      }
				   };
				   generateAllJob.setRule(project);  // the job will need to lock the project in order to save the resource
				   generateAllJob.setPriority(Job.LONG);  // low priority
				   generateAllJob.setProperty(COMPONENT, cp);
				   generateAllJob.schedule();
			}
		}		
		monitor.done();
	}


	/**
	 * @param sm
	 * @return 
	 */
	private boolean processStatemachine(Statemachine sm) {
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
					 dirty = processStatemachine(ssm) || dirty;
				 }
			 }
			 
		 }
		 return dirty;
	}

}
