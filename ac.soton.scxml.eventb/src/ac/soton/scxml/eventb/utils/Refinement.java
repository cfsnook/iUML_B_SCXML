/**
 * 
 */
package ac.soton.scxml.eventb.utils;

import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.statemachines.AbstractNode;
import ac.soton.eventb.statemachines.Statemachine;

/**
 * <p>
 * record structure for data related to refinement levels in the scxml translation
 * </p>
 * 
 * @author cfs
 * @version
 * @see
 * @since
 */
public class Refinement {

		public int level = 0;
		public Machine machine = null;
		public Statemachine statemachine = null;
		public AbstractNode source = null;
		public AbstractNode target = null;	
	
}
