/**
 * 
 */
package ac.soton.scxml.eventb;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.sirius.tests.sample.scxml.DocumentRoot;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.persistence.EMFRodinDB;

import ac.soton.emf.translator.eventb.handler.EventBTranslateHandler;
import ac.soton.eventb.emf.diagrams.generator.commands.TranslateAllCommand;

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
public class ScxmlTranslateHandler extends EventBTranslateHandler {	
	
	/**
	 * This is overridden to invoke the iUML-B translators after SCXML translation has finished
	 * 
	 * @param sourceElement
	 * @param commandId
	 * @param monitor
	 * @throws CoreException 
	 */
	protected IStatus postProcessing(EObject sourceElement, String commandId, IProgressMonitor monitor) throws Exception {
		IStatus status = Status.OK_STATUS;
		if (sourceElement instanceof DocumentRoot){
			IProject project = WorkspaceSynchronizer.getFile(sourceElement.eResource()).getProject();
			EMFRodinDB emfRodinDB = new EMFRodinDB(getEditingDomain());
			List<EventBNamedCommentedComponentElement> components = emfRodinDB.loadAllComponents(project.getName());
			for (EventBNamedCommentedComponentElement cp : components){
				TranslateAllCommand translateAllCmd = new TranslateAllCommand(emfRodinDB.getEditingDomain(),cp);
				if (translateAllCmd.canExecute()){
					status = translateAllCmd.execute(null, null);
				}
			
			}
		}		
		monitor.done();
		return status;
	}
	
}
