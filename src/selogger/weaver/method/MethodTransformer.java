package selogger.weaver.method;

import selogger.EventType;
import selogger.weaver.WeaveLog;
import selogger.weaver.WeaveConfig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;


/**
 * This class is the main implementation of the weaving process for each method. 
 * This class extends LocalVariablesSorter because this class insert 
 * additional local variables to temporarily preserves method parameters. 
 */
public class MethodTransformer extends LocalVariablesSorter {

	public static final String LOGGER_CLASS = "selogger/logging/Logging";

	public static final String METHOD_RECORD_EVENT = "recordEvent";

	private WeaveLog weavingInfo;
	private WeaveConfig config;
	private int currentLine;
	private String className;
	private String sourceFileName;
	private int access;
	private String methodName;
	private String methodDesc;
	
	/**
	 * The index represents the original location in the InsnList object.
	 */
	private int instructionIndex;
	
	private LocalVariables variables;

	private Label startLabel = new Label();
	private Label endLabel = new Label();
	private HashMap<Label, Attr> catchBlockInfo = new HashMap<>();
	private boolean isStartLabelLocated;
	private HashMap<Label, String> labelStringMap = new HashMap<Label, String>();
	private HashMap<Label, Integer> labelLineNumberMap = new HashMap<Label, Integer>();

	/// To check a pair of NEW instruction and its constructor call 
	private Stack<ANewInstruction> newInstructionStack = new Stack<ANewInstruction>();

	// Intentionally set -1 to represent "uninitialized"
	private int lastDataIdVar = -1;

	/**
	 * In a constructor, this flag becomes true after the super() is called.
	 */
	private boolean afterInitialization;

	/**
	 * To skip ARRAY STORE instructions after an array creation
	 */
	private boolean afterNewArray = false;

	/**
	 * Initialize the instance 
	 * @param w is to log the progress
	 * @param config is the configuration of the weaving
	 * @param sourceFileName is a source file name (just for logging the progress)
	 * @param className is a class name
	 * @param outerClassName is outer class name if this class is ineer class 
	 * @param access is modifiers of the method
	 * @param methodName is a method name 
	 * @param methodDesc is a descriptor (parameter types and a return type)
	 * @param signature is a generics signature
	 * @param exceptions represents a throws clause
	 * @param mv is the object for writing bytecode
	 */
	public MethodTransformer(WeaveLog w, WeaveConfig config, String sourceFileName, String className, String outerClassName, int access,
			String methodName, String methodDesc, String signature, String[] exceptions, MethodVisitor mv) {
		super(Opcodes.ASM5, access, methodDesc, mv);
		this.weavingInfo = w;
		this.config = config;
		this.className = className;
		this.sourceFileName = sourceFileName;
		// this.outerClassName = outerClassName; // not used
		this.access = access;
		this.methodName = methodName;
		this.methodDesc = methodDesc;

		this.afterInitialization = !methodName.equals("<init>");
		this.afterNewArray = false;

		this.instructionIndex = 0;

	}

	/**
	 * Receives local variables and instructions in this method.
	 * This method must be called before weaving 
	 * because local variable names and instruction indices are necessary
	 * to generate textual information for DataId.
	 */
	public void setup(List<?> localVariableNodes, InsnList instructions) {
		variables = new LocalVariables(localVariableNodes, instructions);
		
		for (int i = 0; i < instructions.size(); ++i) {
			AbstractInsnNode node = instructions.get(i);
			
			if (node.getType() == AbstractInsnNode.LABEL) {
				// Record label names
				Label label = ((LabelNode) node).getLabel();
				String right = "00000" + Integer.toString(i);
				String labelString = "L" + right.substring(right.length() - 5);
				labelStringMap.put(label, labelString);

			} else if (node.getType() == AbstractInsnNode.LINE) {
				// Record line numbers corresponding to labels (because LINE is always placed AFTER its LABEL) 
				LineNumberNode line = (LineNumberNode)node;
				LabelNode label = line.start;
				labelLineNumberMap.put(label.getLabel(), line.line);
			}
		}
		
		String hash = "";
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			for (int i = 0; i < instructions.size(); ++i) {
				String line = getInstructionString(instructions, i) + "\n";
				digest.update(line.getBytes());
			}
			StringBuilder buf = new StringBuilder();
			for (byte b: digest.digest()) {
				int c = b;
				c &= 0xff; 
				buf.append(Character.forDigit(c / 16, 16));
				buf.append(Character.forDigit(c % 16, 16));
			}
			hash = buf.toString();
		} catch (NoSuchAlgorithmException e) {
		}
		
		weavingInfo.startMethod(className, methodName, methodDesc, access, sourceFileName, hash);
		weavingInfo.nextDataId(-1, -1, EventType.RESERVED, Descriptor.Void, className + "#" + methodName + "#" + methodDesc + "#size=" + instructions.size());
	}

	/**
	 * End the weaving.
	 */
	@Override
	public void visitEnd() {
		super.visitEnd();
	}

	/**
	 * @return a string representation for a given label.
	 */
	private String getLabelString(Label label) {
		if (label == startLabel)
			return "LSTART";
		else if (label == endLabel)
			return "LEND";

		assert labelStringMap.containsKey(label) : "Unknown label";
		if (labelStringMap.containsKey(label)) {
			return labelStringMap.get(label);
		} else {
			// If an unkwnon label is found, assign a new label.
			String tempLabel = "LT" + Integer.toString(labelStringMap.size());
			labelStringMap.put(label, tempLabel);
			return tempLabel;
		}
	}

	/**
	 * Record current line number for other visit methods
	 */
	@Override
	public void visitLineNumber(int line, Label start) {
		super.visitLineNumber(line, start);
		
		// currentLine should be always updated by visitLabel placed before the LABEL
		assert this.currentLine == line;
		
		// Generate a line number event
		if (config.recordLineNumber()) {
			generateLogging(EventType.LINE_NUMBER, Descriptor.Void, null);
		}
		instructionIndex++;
	}

	/**
	 * @return true if the method has a receiver, i.e., the method is an instance method.
	 */
	private boolean hasReceiver() {
		return (access & Opcodes.ACC_STATIC) == 0;
	}
	
	/**
	 * @return true if the method is a constructor.  
	 */
	private boolean isConstructor() {
		return methodName.equals("<init>");
	}
	
	/**
	 * Visiting a method body. 
	 * Generate try { recordEntryEvent; recordParams; body(); } 
	 * catch (Throwable t) { ... }.
	 */
	@Override
	public void visitCode() {

		super.visitCode();

		if (config.recordExecution() || config.recordCatch()) {
			super.visitTryCatchBlock(startLabel, endLabel, endLabel, "java/lang/Throwable");

			// Create an integer to record a jump/exception 
			if (config.recordCatch()) {
				lastDataIdVar = newLocal(Type.INT_TYPE);
				generateLocationUpdate(0);
			}
		
			if (!methodName.equals("<init>")) { // In a constructor, a try block cannot start before a super() call.
				super.visitLabel(startLabel);
				isStartLabelLocated = true;
			}
		}
		
		if (config.recordExecution()) {
			
			// Generate instructions to record parameters
			MethodParameters params = new MethodParameters(methodDesc);
	
			int varIndex = 0; // Index for local variable table
			int receiverOffset = 0;
	
			// Record an entry event with a receiver object
			if (hasReceiver()) { 
				if (isConstructor()) {
					generateLogging(EventType.METHOD_ENTRY, Descriptor.Void, new Attr("calltype", "constructor"));
				} else { // An instance method
					super.visitVarInsn(Opcodes.ALOAD, 0);
					Attr attr = new Attr("calltype", "instance");
					attr.add("index", 0);
					generateLogging(EventType.METHOD_ENTRY, Descriptor.Object, attr);
				}
				varIndex = 1;
				receiverOffset = 1;
			} else {
				generateLogging(EventType.METHOD_ENTRY, Descriptor.Void, new Attr("calltype", "static"));
			}
	
			if (config.recordParameters()) {
				// Record Remaining parameters
				int paramIndex = 0;
				while (paramIndex < params.size()) {
					super.visitVarInsn(params.getLoadInstruction(paramIndex), varIndex);
					generateLogging(EventType.METHOD_PARAM, params.getRecordDesc(paramIndex), new Attr("index", paramIndex + receiverOffset));
					varIndex += params.getWords(paramIndex);
					paramIndex++;
				}
			}
		}
	}

	/**
	 * Store entry points of catch blocks for later visit* methods. 
	 * The method is called BEFORE visit* methods for other instructions, 
	 * according to the implementation of MethodNode class.
	 */
	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		super.visitTryCatchBlock(start, end, handler, type);

		// Store catch block information 
		String block = type != null ? "CATCH" : "FINALLY";
		Attr attrs = new Attr();
		attrs.add("blockthype", block);
		attrs.add("exception", type);
		attrs.add("start", getLabelString(start));
		attrs.add("end", getLabelString(end));
		attrs.add("handler", getLabelString(handler));
		catchBlockInfo.put(handler, attrs);
	}

	/**
	 * Logging a jump instruction if recordLabel is enabled.
	 * Logging a catch event for a method call.
	 */
	@Override
	public void visitLabel(Label label) {
		variables.visitLabel(label);
		
		// Process the label
		super.visitLabel(label);

		// Update line number if there exists a corresponding LineNumberNode
		Integer l = labelLineNumberMap.get(label);
		if (l != null) {
			currentLine = l.intValue();
		}

		if (config.recordCatch() && catchBlockInfo.containsKey(label)) {
			// If the label is a catch block, record the previous location and an exception.
			generateNewVarInsn(Opcodes.ILOAD, lastDataIdVar);
			generateLogging(EventType.CATCH_LABEL, Descriptor.Integer, new Attr("label", getLabelString(label)));
			generateLoggingPreservingStackTop(EventType.CATCH, Descriptor.Object, catchBlockInfo.get(label));
		} else if (config.recordLabel()) {
			// For a regular label, record a previous location.
			generateNewVarInsn(Opcodes.ILOAD, lastDataIdVar);
			generateLogging(EventType.LABEL, Descriptor.Integer, new Attr("label", getLabelString(label)));
		}

		instructionIndex++;
	}

	/**
	 * No additional actions but count the number of instructions.
	 */
	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		super.visitFrame(type, nLocal, local, nStack, stack);
		instructionIndex++;
	}

	/**
	 * Store a location for LABEL event if recordLabel is enabled.
	 */
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if (config.recordLabel()) {
			Attr attr = new Attr("opcode", OpcodesUtil.getString(opcode));
			attr.add("jumpto", getLabelString(label));
			int dataId = nextDataId(EventType.JUMP, Descriptor.Void, attr);
			generateLocationUpdate(dataId);
		}
		super.visitJumpInsn(opcode, label);
		instructionIndex++;
	}

	
	/**
	 * Finalize the method. 
	 * Generate a finally block for exceptional exit.
	 */
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		assert newInstructionStack.isEmpty();
		assert isStartLabelLocated || !config.recordExecution();

		if (config.recordExecution() || config.recordCatch()) {
			// Since visitMaxs is called at the end of a method, insert an
			// exception handler to record an exception in the method.
			// The conceptual code: 
			//   catch (Throwable t) { 
			//     recordExceptionalExitLabel(pcPositionVar); 
			//     recordExceptionalExit(t); 
			//     throw t; 
			//   }
			
			// Assume an exception object on the stack
			super.visitLabel(endLabel);
			if (config.recordCatch()) {
				generateNewVarInsn(Opcodes.ILOAD, lastDataIdVar);
				generateLogging(EventType.CATCH_LABEL, Descriptor.Integer, new Attr("location", "exceptional-exit"));
				generateLoggingPreservingStackTop(EventType.CATCH, Descriptor.Object, new Attr("location", "exceptional-exit"));
			}
			if (config.recordExecution()) {
				generateLoggingPreservingStackTop(EventType.METHOD_EXCEPTIONAL_EXIT, Descriptor.Object, new Attr("location", "exceptional-exit-rethrow"));
			}
			super.visitInsn(Opcodes.ATHROW);
		}

		// Finalize the method
		try {
			super.visitMaxs(maxStack, maxLocals);
		} catch (RuntimeException e) {
			weavingInfo.log("Error during weaving method " + className + "#" + methodName + "#" + methodDesc);
			throw e;
		}
	}

	/**
	 * Insert logging code for NEW, ANEWARRAY, INSTANCEOF instructions.
	 */
	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (opcode == Opcodes.NEW) {
			super.visitTypeInsn(opcode, type);
			if (config.recordMethodCall() && config.recordParameters()) {
				int dataId = generateLogging(EventType.NEW_OBJECT, Descriptor.Void, new Attr("type", type));
				newInstructionStack.push(new ANewInstruction(dataId, type));
			} else {
				// A tentative item is added to recognize "this()" and "super()" in visitMethodInsn.  
				newInstructionStack.push(new ANewInstruction(-1, type)); 
			}
		} else if (opcode == Opcodes.ANEWARRAY) {
			if (config.recordArrayInstructions()) {
				generateLoggingPreservingStackTop(EventType.NEW_ARRAY, Descriptor.Integer, new Attr("elementtype", type));
				super.visitTypeInsn(opcode, type); // -> stack: [ARRAYREF]
				generateLoggingPreservingStackTop(EventType.NEW_ARRAY_RESULT, Descriptor.Object, null);
			} else {
				super.visitTypeInsn(opcode, type);
			}
			afterNewArray = true;
		} else if (opcode == Opcodes.INSTANCEOF) {
			if (config.recordObject()) {
				generateLoggingPreservingStackTop(EventType.OBJECT_INSTANCEOF, Descriptor.Object, new Attr("type", type));
				super.visitTypeInsn(opcode, type); // -> [ result ]
				generateLoggingPreservingStackTop(EventType.OBJECT_INSTANCEOF_RESULT, Descriptor.Boolean, null);
			} else {
				super.visitTypeInsn(opcode, type); // -> [ result ]
			}
		} else {
			super.visitTypeInsn(opcode, type);
		}
		instructionIndex++;
	}

	/**
	 * Insert logging code for a NEWARRAY instruction. 
	 * It records the array size and a created array.
	 */
	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (opcode == Opcodes.NEWARRAY) {
			if (config.recordArrayInstructions()) {
				// A static operand indicates an element type. 
				// stack: [SIZE]
				generateLoggingPreservingStackTop(EventType.NEW_ARRAY, Descriptor.Integer, new Attr("elementtype", OpcodesUtil.getArrayElementType(operand)));
				super.visitIntInsn(opcode, operand); // -> stack: [ARRAYREF]
				generateLoggingPreservingStackTop(EventType.NEW_ARRAY_RESULT, Descriptor.Object, null);
			} else {
				super.visitIntInsn(opcode, operand);
			}
			afterNewArray = true;
		} else {
			super.visitIntInsn(opcode, operand);
		}
		instructionIndex++;
	}
	

	/**
	 * Insert logging code for INVOKE instructions.
	 */
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

		// This method call is a constructor chain if the call initializes
		// "this" object by this() or suepr().
		boolean isConstructorChain = name.equals("<init>") && methodName.equals("<init>")
				&& newInstructionStack.isEmpty();
		assert !isConstructorChain || opcode == Opcodes.INVOKESPECIAL : "A constructor chain must use INVOKESPECIAL.";

		// Pop a corresponding new instruction if this method call initializes
		// an object.
		ANewInstruction newInstruction = null;
		if (!isConstructorChain && name.equals("<init>")) {
			newInstruction = newInstructionStack.pop();
			assert newInstruction.getTypeName().equals(owner);
		}

		// Generate instructions to record method call and its parameters
		if (config.recordMethodCall()) {

			Attr attrs = new Attr();
			attrs.add("opcode", OpcodesUtil.getString(opcode));
			attrs.add("owner", owner);
			attrs.add("name", name);
			attrs.add("desc", desc);

			if (config.recordParameters()) {
				// Generate code to record parameters
				MethodParameters params = new MethodParameters(desc);

				// Store parameters except for a receiver into additional local
				// variables.
				for (int i = params.size() - 1; i >= 0; i--) {
					int local = super.newLocal(params.getType(i));
					params.setLocalVar(i, local);
					generateNewVarInsn(params.getStoreInstruction(i), local);
				}
				// Here, all parameters (except for a receiver) are stored in
				// local variables.

				boolean hasReceiver = (opcode != Opcodes.INVOKESTATIC);
				boolean receiverNotInitialized = isConstructorChain || (newInstruction != null);

				// Duplicate an object reference to record the created object
				int offset;
				int firstDataId;
				if (receiverNotInitialized) { 
					// For constructor, duplicate the object reference, and record it later.
					// Here, record only the execution of the call.
					super.visitInsn(Opcodes.DUP);
					attrs.add("calltype", "constructor");
					if (newInstruction != null) {
						attrs.add("newinstruction", newInstruction.getInstructionIndex());
					} 
					firstDataId = generateLogging(EventType.CALL, Descriptor.Void, attrs);
					offset = 1;
				} else if (hasReceiver) { // For a regular non-static method,
											// duplicate and record the object
											// reference.
					super.visitInsn(Opcodes.DUP);
					attrs.add("calltype", "instance");
					firstDataId = generateLogging(EventType.CALL, Descriptor.Object, attrs);
					offset = 1;
				} else { // otherwise, no receivers.
					attrs.add("calltype", "static");
					firstDataId = generateLogging(EventType.CALL, Descriptor.Void, attrs);
					offset = 0;
				}

				// Record remaining parameters
				int paramIndex = 0;
				while (paramIndex < params.size()) {
					generateNewVarInsn(params.getLoadInstruction(paramIndex), params.getLocalVar(paramIndex));
					Attr a = new Attr("index", paramIndex + offset);
					a.add("type", params.getType(paramIndex).getDescriptor());
					generateLogging(EventType.CALL_PARAM, params.getRecordDesc(paramIndex), a);
					paramIndex++;
				}

				// Restore parameters from local variables
				for (int i = 0; i < params.size(); i++) {
					generateNewVarInsn(params.getLoadInstruction(i), params.getLocalVar(i));
				}

				// Store the current location for exceptional exit
				generateLocationUpdate(firstDataId);
				// Call the original method
				super.visitMethodInsn(opcode, owner, name, desc, itf);
				// Reset the current location for exceptional exit
				generateLocationUpdate(0);

				// record return value
				String returnDesc = getReturnValueDesc(desc);
				Descriptor d = Descriptor.get(returnDesc);
				generateLoggingPreservingStackTop(EventType.CALL_RETURN, d, new Attr("type", returnDesc));

				if (isConstructorChain) {
					if (config.recordExecution()) {
						// Record an object initialized by this() or super()
						generateLogging(EventType.METHOD_OBJECT_INITIALIZED, Descriptor.Object, null);
					}
					afterInitialization = true;
				} else if (newInstruction != null) {
					// Record an object created by "new X()"
					generateLogging(EventType.NEW_OBJECT_CREATED, Descriptor.Object, new Attr("newinstruction", newInstruction.getInstructionIndex()));
				}


			} else { // !recordParameters
				// Call an occurrence of a call
				if (newInstruction != null) {
					attrs.add("newinstruction", newInstruction.getInstructionIndex());
				} 
				int callId = generateLogging(EventType.CALL, Descriptor.Void, attrs);

				// Store the current location for exceptional exit
				generateLocationUpdate(callId);

				// Call the original method
				super.visitMethodInsn(opcode, owner, name, desc, itf);

				// Reset the current location
				generateLocationUpdate(0);

				// Record return event
				generateLogging(EventType.CALL_RETURN, Descriptor.Void, null);
				
				// Constructor call
				if (isConstructorChain) {
					if (config.recordExecution()) {
						super.visitVarInsn(Opcodes.ALOAD, 0);
						generateLogging(EventType.METHOD_OBJECT_INITIALIZED, Descriptor.Object, null);
					}
					afterInitialization = true;
				}

			}


		} else {  

			super.visitMethodInsn(opcode, owner, name, desc, itf);

			// Constructor call
			if (isConstructorChain) {
				if (config.recordExecution()) {
					super.visitVarInsn(Opcodes.ALOAD, 0);
					generateLogging(EventType.METHOD_OBJECT_INITIALIZED, Descriptor.Object, null);
				}
				afterInitialization = true;
			}
		}

		// If this call is a constructor-chain (super()/this() at the beginning
		// of a constructor), start a try block to record an exception thrown by
		// the remaining code.
		// Because Java Verifier does not allow "try { super(); } catch ... ",
		// this code generate "super(); try { ... }".
		if (isConstructorChain && (config.recordExecution() || config.recordCatch())) {
			super.visitLabel(startLabel);
			isStartLabelLocated = true;
		}

		instructionIndex++;
	}

	/**
	 * Insert an instruction to store the current bytecode location to a local variable to track the control flow.  
	 * @param dataId specifies the instruction location.
	 */
	private void generateLocationUpdate(int dataId) {
		assert lastDataIdVar >= 0: "Uninitialized lastDataId";
		super.visitLdcInsn(dataId);
		generateNewVarInsn(Opcodes.ISTORE, lastDataIdVar);
	}

	/**
	 * Insert logging code for a MultiANewArray instruction.
	 * It records a created array and its elements.
	 */
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if (config.recordArrayInstructions()) {
			Attr attrs = new Attr("type", desc);
			attrs.add("dimensions", dims);
			int dataId = nextDataId(EventType.MULTI_NEW_ARRAY, Descriptor.Object, attrs);
			nextDataId(EventType.MULTI_NEW_ARRAY_OWNER, Descriptor.Object, null);
			nextDataId(EventType.MULTI_NEW_ARRAY_ELEMENT, Descriptor.Object, null);
			super.visitMultiANewArrayInsn(desc, dims);
			super.visitInsn(Opcodes.DUP);
			super.visitLdcInsn(dataId);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordMultiNewArray", "(Ljava/lang/Object;I)V", false);
		} else {
			super.visitMultiANewArrayInsn(desc, dims);
		}
		afterNewArray = true;
		instructionIndex++;
	}

	/**
	 * Insert logging code for an IINC instruction.
	 * It records a value after increment.
	 */
	@Override
	public void visitIincInsn(int var, int increment) {
		super.visitIincInsn(var, increment);
		if (config.recordLocalAccess()) {
			super.visitVarInsn(Opcodes.ILOAD, var);
			LocalVariableNode local = variables.getLoadVar(var);
			Attr attrs = new Attr();
			attrs.add("inc", increment);
			attrs.add("varindex", var);
			attrs.add("name", (local != null) ? local.name : "(unavailable)");
			attrs.add("type", (local != null) ? local.desc : "I");
			generateLogging(EventType.LOCAL_INCREMENT, Descriptor.Integer, attrs); 
		}
		instructionIndex++;
	}
	
	/**
	 * Extract a descriptor representing the return type of a given method descriptor.
	 * TODO This should be moved to MethodParameters.
	 */
	private String getReturnValueDesc(String methodDesc) {
		int index = methodDesc.indexOf(')');
		String returnTypeName = methodDesc.substring(index + 1);
		return returnTypeName;
	}
	
	/**
	 * @return a descriptor representing the return type of this method.
	 */
	private Descriptor getDescForReturn() {
		int index = methodDesc.lastIndexOf(')');
		assert index >= 0: "Invalid method descriptor " + methodDesc;
		String returnValueType = methodDesc.substring(index+1);
		return Descriptor.get(returnValueType);
	}

	/**
	 * Insert logging code for various instructions.
	 */
	@Override
	public void visitInsn(int opcode) {

		if (OpcodesUtil.isReturn(opcode)) {
			if (config.recordExecution()) {
				generateLoggingPreservingStackTop(EventType.METHOD_NORMAL_EXIT, getDescForReturn(), null);
			}
			super.visitInsn(opcode);
		} else if (opcode == Opcodes.ATHROW) {
			if (config.recordExecution()) {
				int dataId = generateLoggingPreservingStackTop(EventType.METHOD_THROW, Descriptor.Object, null);
				if (config.recordCatch()) {
					generateLocationUpdate(dataId);
				}
			} else if (config.recordCatch()) {
				// Assign a thread ID just for location
				int dataId = nextDataId(EventType.METHOD_THROW, Descriptor.Void, null);
				generateLocationUpdate(dataId);
			}

			super.visitInsn(opcode);
		} else if (OpcodesUtil.isArrayLoad(opcode)) {
			if (config.recordArrayInstructions()) {
				generateRecordArrayLoad(opcode);
			} else {
				super.visitInsn(opcode);
			}
		} else if (OpcodesUtil.isArrayStore(opcode)) {
			if (config.recordArrayInstructions() && !(config.ignoreArrayInitializer() && afterNewArray)) {
				generateRecordArrayStore(opcode);
			} else {
				super.visitInsn(opcode);
			}
		} else if (opcode == Opcodes.ARRAYLENGTH) {
			if (config.recordArrayInstructions()) {
				generateLoggingPreservingStackTop(EventType.ARRAY_LENGTH, Descriptor.Object, null);
				super.visitInsn(opcode); // -> [ arraylength ]
				generateLoggingPreservingStackTop(EventType.ARRAY_LENGTH_RESULT, Descriptor.Integer, null);
			} else {
				super.visitInsn(opcode);
			}
		} else if (opcode == Opcodes.MONITORENTER) {
			if (config.recordSynchronization()) {
				super.visitInsn(Opcodes.DUP);
				super.visitInsn(Opcodes.DUP);
				// Monitor enter fails if the argument is null.
				int dataid = generateLogging(EventType.MONITOR_ENTER, Descriptor.Object, null);
				generateLocationUpdate(dataid);
				super.visitInsn(opcode); // Enter the monitor
				generateLocationUpdate(0);
				generateLogging(EventType.MONITOR_ENTER_RESULT, Descriptor.Object, null);
			} else {
				super.visitInsn(opcode);
			}
		} else if (opcode == Opcodes.MONITOREXIT) {
			if (config.recordSynchronization()) {
				super.visitInsn(Opcodes.DUP); // -> [objectref, objectref]
				generateLogging(EventType.MONITOR_EXIT, Descriptor.Object, null);
				super.visitInsn(opcode);
			} else {
				super.visitInsn(opcode);
			}
		} else if (opcode == Opcodes.DDIV || 
				opcode == Opcodes.FDIV ||
				opcode == Opcodes.IDIV ||
				opcode == Opcodes.LDIV) {
			if (config.recordCatch()) {
				int dataId = nextDataId(EventType.DIVIDE, Descriptor.Void, null);
				generateLocationUpdate(dataId);
				super.visitInsn(opcode);
				generateLocationUpdate(0);
			} else {
				super.visitInsn(opcode);
			}
		} else {
			super.visitInsn(opcode);
		}
		instructionIndex++;
	}

	/**
	 * Insert logging code for INVOKEDYNAMIC instruction.
	 */
	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		if (config.recordMethodCall()) {
			// Duplicate an object reference to record the created object
			Attr attrs = new Attr();
			attrs.add("opcode", "INVOKEDYNAMIC");
			attrs.add("name", name);
			attrs.add("desc", desc);
			attrs.add("bootstrap_owner", bsm.getOwner());
			attrs.add("bootstrap_method", bsm.getName());
			attrs.add("bootstrap_desc", bsm.getDesc());
			for (int i=0; i<bsmArgs.length; i++) {
				attrs.add("bootstrap_args" + i, bsmArgs[i].getClass().getName());
			}
			
			generateLogging(EventType.INVOKE_DYNAMIC, Descriptor.Void, attrs);

			if (config.recordParameters()) {
				// Generate code to record parameters
				MethodParameters params = new MethodParameters(desc);

				// Store parameters except for a receiver into additional local variables.
				for (int i = params.size() - 1; i >= 0; i--) {
					int local = super.newLocal(params.getType(i));
					params.setLocalVar(i, local);
					generateNewVarInsn(params.getStoreInstruction(i), local);
				}

				// Record remaining parameters
				int paramIndex = 0;
				while (paramIndex < params.size()) {
					generateNewVarInsn(params.getLoadInstruction(paramIndex), params.getLocalVar(paramIndex));
					Attr a = new Attr("index", paramIndex);
					a.add("type", params.getType(paramIndex).getDescriptor());
					generateLogging(EventType.INVOKE_DYNAMIC_PARAM, params.getRecordDesc(paramIndex), a);
					paramIndex++;
				}

				// Restore parameters from local variables
				for (int i = 0; i < params.size(); i++) {
					generateNewVarInsn(params.getLoadInstruction(i), params.getLocalVar(i));
				}

				// Call the original method
				super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);

			} else {
				// Call the original method
				super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
			}
			
			// record return value
			generateLoggingPreservingStackTop(EventType.INVOKE_DYNAMIC_RESULT, Descriptor.Object, attrs);
			
		} else {
			super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
		}
		instructionIndex++;
	}

	/**
	 * No additional actions but count the number of instructions.
	 */
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		super.visitLookupSwitchInsn(dflt, keys, labels);
		instructionIndex++;
	}

	/**
	 * No additional actions but count the number of instructions.
	 */
	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		super.visitTableSwitchInsn(min, max, dflt, labels);
		instructionIndex++;
	}

	/**
	 * Insert logging code for a Load Constant instruction 
	 * in order to record the constant object. 
	 */
	@Override
	public void visitLdcInsn(Object cst) {
		super.visitLdcInsn(cst); // -> [object]
		if (config.recordObject() && 
			!(cst instanceof Integer) && !(cst instanceof Long) && 
			!(cst instanceof Double) && !(cst instanceof Float)) {
			generateLoggingPreservingStackTop(EventType.OBJECT_CONSTANT_LOAD, Descriptor.Object, new Attr("type", cst.getClass().getName()));
		}
		instructionIndex++;
	}

	/**
	 * Insert logging code for ARRAY LOAD instruction.
	 */
	private void generateRecordArrayLoad(int opcode) {
		Descriptor elementDesc = OpcodesUtil.getDescForArrayLoad(opcode);

		// Create dataId used in Logging class
		int dataId = nextDataId(EventType.ARRAY_LOAD, Descriptor.Object, null);
		nextDataId(EventType.ARRAY_LOAD_INDEX, Descriptor.Integer, null); 
		int resultId = nextDataId(EventType.ARRAY_LOAD_RESULT, elementDesc, null);

		super.visitInsn(Opcodes.DUP2); // stack: [array, index, array, index]
		super.visitLdcInsn(dataId); // [array, index, array, index, id]
		super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordArrayLoad", "(Ljava/lang/Object;II)V", false);

		generateLocationUpdate(dataId);

		// the original instruction [array, index] -> [value]
		super.visitInsn(opcode);
		
		if (elementDesc == Descriptor.Long || elementDesc == Descriptor.Double) {
			super.visitInsn(Opcodes.DUP2); 
		} else {
			super.visitInsn(Opcodes.DUP);
		}
		super.visitLdcInsn(resultId);
		super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordEvent", "(" + elementDesc.getString() + "I)V", false);

		generateLocationUpdate(0);
	}

	/**
	 * Insert logging code for ARRAY STORE instruction.
	 */
	private void generateRecordArrayStore(int opcode) {
		String elementDesc = OpcodesUtil.getDescForArrayStore(opcode);
		String methodDesc = "(Ljava/lang/Object;I" + elementDesc + "I)V";

		int arrayDataId = nextDataId(EventType.ARRAY_STORE, Descriptor.Object, null);
		nextDataId(EventType.ARRAY_STORE_INDEX, Descriptor.Integer, null);
		nextDataId(EventType.ARRAY_STORE_VALUE, Descriptor.get(elementDesc), null);

		int valueStoreVar = super.newLocal(OpcodesUtil.getAsmType(elementDesc));
		// Stack: [ array, index, value ]
		generateNewVarInsn(OpcodesUtil.getStoreInstruction(elementDesc), valueStoreVar); // -> Local: [value],  Stack: [array, index].
		super.visitInsn(Opcodes.DUP2); // -> Local: [value], Stack: [array, index, array, index]
		generateNewVarInsn(OpcodesUtil.getLoadInstruction(elementDesc), valueStoreVar);

		super.visitLdcInsn(arrayDataId);
		super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordArrayStore", methodDesc, false);

		generateNewVarInsn(OpcodesUtil.getLoadInstruction(elementDesc), valueStoreVar); // -> [array, index, value]

		generateLocationUpdate(arrayDataId);
		super.visitInsn(opcode); // original store instruction
		generateLocationUpdate(0);

	}


	/**
	 * Insert logging code for field access instruction.
	 */
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (!config.recordFieldAccess()) {
			super.visitFieldInsn(opcode, owner, name, desc);
			instructionIndex++;
			return;
		}

		Attr attrs = new Attr();
		attrs.add("owner", owner);
		attrs.add("name", name);
		attrs.add("type", desc);

		if (opcode == Opcodes.GETSTATIC) {
			// Record a resultant value
			super.visitFieldInsn(opcode, owner, name, desc); // [] -> [ value ]
			generateLoggingPreservingStackTop(EventType.GET_STATIC_FIELD, Descriptor.get(desc), attrs);
			
		} else if (opcode == Opcodes.PUTSTATIC) {
			// Record a new value
			generateLoggingPreservingStackTop(EventType.PUT_STATIC_FIELD, Descriptor.get(desc), attrs);
			super.visitFieldInsn(opcode, owner, name, desc);

		} else if (opcode == Opcodes.GETFIELD) {
			int fieldDataId = generateLoggingPreservingStackTop(EventType.GET_INSTANCE_FIELD, Descriptor.Object, attrs);

			generateLocationUpdate(fieldDataId);

			// Execute GETFIELD
			super.visitFieldInsn(opcode, owner, name, desc); // -> [value]

			generateLocationUpdate(0);

			// Record the result
			generateLoggingPreservingStackTop(EventType.GET_INSTANCE_FIELD_RESULT, Descriptor.get(desc), attrs);

		} else {
			assert opcode == Opcodes.PUTFIELD;
			if (afterInitialization) {
				// stack: [object, value]
				if (desc.equals("D") || desc.equals("J")) {
					int local = newLocal(OpcodesUtil.getAsmType(desc));
					// Store a value to a local variable, record an object, and then load the value.
					generateNewVarInsn(OpcodesUtil.getStoreInstruction(desc), local); 
					int fieldDataId = generateLoggingPreservingStackTop(EventType.PUT_INSTANCE_FIELD, Descriptor.Object, attrs);
					generateNewVarInsn(OpcodesUtil.getLoadInstruction(desc), local); 

					// Record a value.
					generateLoggingPreservingStackTop(EventType.PUT_INSTANCE_FIELD_VALUE, Descriptor.get(desc), attrs);

					generateLocationUpdate(fieldDataId);
					
					// Original Instruction
					super.visitFieldInsn(opcode, owner, name, desc);

					generateLocationUpdate(0);

				} else {
					super.visitInsn(Opcodes.DUP2);
					super.visitInsn(Opcodes.SWAP); // -> [object, value, value, object]
					int fieldDataId = generateLogging(EventType.PUT_INSTANCE_FIELD, Descriptor.Object, attrs);
					generateLogging(EventType.PUT_INSTANCE_FIELD_VALUE, Descriptor.get(desc), attrs);

					generateLocationUpdate(fieldDataId);
					super.visitFieldInsn(opcode, owner, name, desc);
					generateLocationUpdate(0);
				}
			} else {
				// Before the target object is initialized, we cannot record the object.
				generateLoggingPreservingStackTop(EventType.PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION, Descriptor.get(desc), attrs);
				super.visitFieldInsn(opcode, owner, name, desc);
			}
		}
		instructionIndex++;
	}

	
	/**
	 * Create a new Data ID.
	 */
	private int nextDataId(EventType eventType, Descriptor desc, Attr label) {
//		assert !label.contains(WeavingInfo.SEPARATOR) : "Location ID cannot includes WeavingInfo.SEPARATOR(" + WeavingInfo.SEPARATOR + ").";
		if (label != null) {
			return weavingInfo.nextDataId(currentLine, instructionIndex, eventType, desc, label.toString());
		} else {
			return weavingInfo.nextDataId(currentLine, instructionIndex, eventType, desc, "");
		}
	}


	/**
	 * Insert logging code for local variable and RET instructions.
	 */
	@Override
	public void visitVarInsn(int opcode, int var) {
		if (config.recordLocalAccess()) {
			Descriptor d = OpcodesUtil.getDescForStore(opcode);
			if (d != null) { // isStore
				LocalVariableNode local = variables.getStoreVar(instructionIndex, var);
				Attr attr = new Attr();
				attr.add("varindex", var);
				attr.add("name", (local != null) ? local.name : "(unavailable)");
				attr.add("desc", (local != null) ? local.desc : d.getString());
				if (local != null) d = Descriptor.get(local.desc);
				generateLoggingPreservingStackTop(EventType.LOCAL_STORE,  d, attr); 
			} else if (opcode == Opcodes.RET) {
				d = Descriptor.Integer;
				super.visitVarInsn(Opcodes.ILOAD, var);
				generateLogging(EventType.RET,  d, new Attr("varindex", var)); 
			}
		}

		super.visitVarInsn(opcode, var);
		
		if (config.recordLocalAccess()) {
			Descriptor d = OpcodesUtil.getDescForLoad(opcode);
			if (d != null) { // isLoad
				if (!(hasReceiver() && var == 0)) {  // Record variables except for "this"
					LocalVariableNode local = variables.getLoadVar(var);
					Attr attr = new Attr();
					attr.add("varindex", var);
					attr.add("name", (local != null) ? local.name : "(unavailable)");
					attr.add("desc", (local != null) ? local.desc : d.getString());
					if (local != null) {
						d = Descriptor.get(local.desc);
					}
					generateLoggingPreservingStackTop(EventType.LOCAL_LOAD,  d, attr); 
				}
			}
		}
		instructionIndex++;
	}

	/**
	 * Create a variable instruction using new local variables created by newLocal. 
	 * This method does not use super.visitVarInsn because 
	 * visitVarInsn will renumber variable index. (A
	 * return value of newLocal is a renumbered index.)
	 * 
	 * @param opcode
	 * @param local
	 */
	private void generateNewVarInsn(int opcode, int local) {
		if (mv != null)
			mv.visitVarInsn(opcode, local);
	}

	/**
	 * Generate logging instructions.
	 * 
	 * @param paramName specifies a data name.
	 * @param valueDesc specifies a data type.  If it has no data, use Descriptor.Void.  
	 * @param label
	 * @return dataId.
	 */
	private int generateLogging(EventType eventType, Descriptor valueDesc, Attr label) {
		int dataId = nextDataId(eventType, valueDesc, label);
		super.visitLdcInsn(dataId);
		if (valueDesc == Descriptor.Void) {
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, METHOD_RECORD_EVENT, "(I)V", false);
		} else {
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, METHOD_RECORD_EVENT, 
					"(" + valueDesc.getString() + "I)V", false);
		}
		return dataId;
	}

	/**
	 * Generate logging instructions to record a copy value on the stack top.
	 * This call does not change a stack.
	 * 
	 * @param paramName
	 * @param valueDesc
	 * @param label
	 */
	private int generateLoggingPreservingStackTop(EventType eventType, Descriptor valueDesc, Attr label) {
		int dataId = nextDataId(eventType, valueDesc, label);
		if (valueDesc == Descriptor.Void) {
			super.visitLdcInsn(dataId);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, METHOD_RECORD_EVENT, "(I)V", false);
		} else {
			if (valueDesc == Descriptor.Long || valueDesc == Descriptor.Double) {
				super.visitInsn(Opcodes.DUP2);
			} else {
				super.visitInsn(Opcodes.DUP);
			}
			super.visitLdcInsn(dataId);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, METHOD_RECORD_EVENT,
					"(" + valueDesc.getString() + "I)V", false);
		}
		return dataId;
	}
	
	/**
	 * @param method specifies a method containing an instruction.
	 * @param index specifies the position of an instruction in the list of instructions.
	 * @return a string representation of an instruction.
	 */
	private String getInstructionString(InsnList instructions, int index) {
		if (index == -1) return "ARG";
		
		AbstractInsnNode node = instructions.get(index);
		int opcode = node.getOpcode();
		String op = Integer.toString(index) + ": " + OpcodesUtil.getString(opcode);

		switch (node.getType()) {
		case AbstractInsnNode.VAR_INSN:
			
			int var = ((VarInsnNode)node).var; // variable index
			Descriptor d = OpcodesUtil.getDescForStore(opcode);
			if (d != null) { // isStore
				LocalVariableNode local = variables.getStoreVar(index, var);
				if (local != null) {
					return op + " " + Integer.toString(var) + " (" + local.name + ")";
				} else {
					return op + " " + Integer.toString(var);
				}
				
			} else if (opcode == Opcodes.RET) {
				return op + " " + var;
			} else {
				if (hasReceiver() && var == 0) {
					return op + " (this)";
				} else {
					LocalVariableNode local = variables.getLoadVar(var);
					if (local != null) {
						return op + " " + Integer.toString(var) + " (" + local.name + ")";
					} else {
						return op + " " + Integer.toString(var);
					}
				}
			}
			
		case AbstractInsnNode.IINC_INSN:
			IincInsnNode iinc = (IincInsnNode)node;
			LocalVariableNode local = variables.getLoadVar(iinc.var);
			if (local != null) {
				return op + " " + Integer.toString(iinc.incr) + ", " + Integer.toString(iinc.var) + " (" + local.name + ")";
			} else {
				return op + " " + Integer.toString(iinc.var) + ", " + Integer.toString(iinc.var);
			}

		case AbstractInsnNode.FIELD_INSN:
			FieldInsnNode fieldNode = (FieldInsnNode)node;
			return op + " " + fieldNode.owner + "#" + fieldNode.name + ": " + fieldNode.desc;
			
		case AbstractInsnNode.METHOD_INSN:
			MethodInsnNode methodInsnNode = (MethodInsnNode)node;
			return op + " " + methodInsnNode.owner + "#" + methodInsnNode.name + methodInsnNode.desc;
		
		case AbstractInsnNode.LINE:
			return Integer.toString(index) + ": " + "(line)";
			
		case AbstractInsnNode.LABEL:
			Label label = ((LabelNode) node).getLabel();
			return Integer.toString(index) + ": " + "(" + labelStringMap.get(label) + ")";
			
		case AbstractInsnNode.JUMP_INSN:
			JumpInsnNode jumpNode = (JumpInsnNode)node;
			return op + " " + labelStringMap.get(jumpNode.label.getLabel());
			
		case AbstractInsnNode.FRAME:
			FrameNode frameNode = (FrameNode)node;
			return Integer.toString(index) + ": FRAME-OP(" + frameNode.type + ")";
			
		case AbstractInsnNode.LDC_INSN:
			LdcInsnNode ldc = (LdcInsnNode)node;
			return op + " " + ldc.cst.toString();
		
		default: 
			return op; 
		}
	}
	
	public class Attr {

		private ArrayList<String> values;
		
		public Attr() {
			values = new ArrayList<>();
		}
		public Attr(String key, String value) {
			this();
			values.add(key + "=" + value);
		}

		public Attr(String key, int value) {
			this();
			values.add(key + "=" + value);
		}

		public void add(String key, String value) {
			values.add(key + "=" + value);
		}

		public void add(String key, int value) {
			values.add(key + "=" + value);
		}
		
		public String toString() {
			StringBuilder b = new StringBuilder();
			boolean isFirst = true;
			for (String v: values) {
				if (isFirst) {
					isFirst = false;
				} else {
					b.append(";");
				}
				b.append(v);
			}
			return b.toString();
		}
	}
	
}
