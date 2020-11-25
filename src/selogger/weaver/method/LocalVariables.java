package selogger.weaver.method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;

/**
 * This object is to manage active local variables for each instruction.
 */
public class LocalVariables {

	private List<?> localVariables;
	private ArrayList<LocalVariableNode> activeVars;
	private InsnList instructions;
	
	/**
	 * Initialize the object
	 * @param localVariableNodes represent the local variables.
	 * @param instructions is the list of instructions of the method
	 */
	public LocalVariables(List<?> localVariableNodes, InsnList instructions) {
		this.localVariables = localVariableNodes;
		this.instructions = instructions;
		this.activeVars = new ArrayList<>();
	}
	
	/**
	 * Update active variables from the given label.
	 */
	public void visitLabel(Label label) {
		for (int i=0; i<localVariables.size(); i++) {
			LocalVariableNode local = (LocalVariableNode)localVariables.get(i);
			if (local.start.getLabel() == label && local.end.getLabel() != label) {
				activeVars.add(local);
			} else if (local.end.getLabel() == label) {
				activeVars.remove(local);
			}
		}
		
		// Check consistency (At most one local variable is active for each local variable index in the table)
		Collections.sort(activeVars, new Comparator<LocalVariableNode>() {
			@Override
			public int compare(LocalVariableNode o1, LocalVariableNode o2) {
				int result = o1.index - o2.index;
				if (result == 0) result = o1.name.compareTo(o2.name);
				return result;
			}
		});
		for (int i=0; i<activeVars.size()-1; i++) {
			assert activeVars.get(i).index != activeVars.get(i+1).index: "Two local variables are active for the same index." + activeVars.get(i).name + ":" + activeVars.get(i).index;
		}
		
	}

	/**
	 * @return a LocalVariableNode of an active variable corresponding to a specified variable index.
	 */
	public LocalVariableNode getLoadVar(int var) {
		for (LocalVariableNode v: activeVars) {
			if (v.index == var) return v;
		}
		return null;
	}
	
	/**
	 * @return a LocalVariableNode for a specified variable.
	 * It requires instruction index, because the specified instruction makes a variable active.
	 * If the instruction is not followed by a label (that is a start of a variable scope), this method searches an active variable.
	 */
	public LocalVariableNode getStoreVar(int instructionIndex, int var) {
		// Get a label after the instruction
		AbstractInsnNode node = instructions.get(instructionIndex);
		node = node.getNext();
		while (node.getType() == AbstractInsnNode.LINE || 
			   node.getType() == AbstractInsnNode.FRAME) {
			node = node.getNext(); 
		}

		// Find a variable that becomes active from the label.
		if (node.getType() == AbstractInsnNode.LABEL) {
			for (int i=0; i<localVariables.size(); i++) {
				LocalVariableNode local = (LocalVariableNode)localVariables.get(i);
				if (local.start == node && local.index == var) {
					return local;
				}
			}
		}
		return getLoadVar(var);
	}
	
}
