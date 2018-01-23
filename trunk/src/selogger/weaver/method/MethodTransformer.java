package selogger.weaver.method;

import selogger.EventType;
import selogger.weaver.WeaveLog;
import selogger.weaver.WeaveConfig;

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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;

public class MethodTransformer extends LocalVariablesSorter {

	public static final String LOGGER_CLASS = "selogger/logging/Logging";

	public static final String METHOD_RECORD_EVENT = "recordEvent";

	private WeaveLog weavingInfo;
	private WeaveConfig config;
	private int currentLine;
	private String className;
	private int access;
	private String methodName;
	private String methodDesc;
	private int instructionIndex;
	
	private LocalVariables variables;

	private Label startLabel = new Label();
	private Label endLabel = new Label();
	private HashMap<Label, String> catchBlockInfo = new HashMap<>();
	private boolean isStartLabelLocated;
	private HashMap<Label, String> labelStringMap = new HashMap<Label, String>();

	private Stack<ANewInstruction> newInstructionStack = new Stack<ANewInstruction>();

	private int lastDataIdVar;

	/**
	 * In a constructor, this flag becomes true after the super() is called.
	 */
	private boolean afterInitialization;

	private boolean afterNewArray = false;

	public MethodTransformer(WeaveLog w, WeaveConfig config, String sourceFileName, String className, String outerClassName, int access,
			String methodName, String methodDesc, String signature, String[] exceptions, MethodVisitor mv) {
		super(Opcodes.ASM5, access, methodDesc, mv);
		this.weavingInfo = w;
		this.config = config;
		this.className = className;
		// this.outerClassName = outerClassName; // not used
		this.access = access;
		this.methodName = methodName;
		this.methodDesc = methodDesc;

		this.afterInitialization = !methodName.equals("<init>");
		this.afterNewArray = false;

		this.instructionIndex = 0;

		weavingInfo.startMethod(className, methodName, methodDesc, access, sourceFileName);
		weavingInfo.nextDataId(-1, -1, EventType.RESERVED, Descriptor.Void, className + "#" + methodName + "#" + methodDesc);
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
				Label label = ((LabelNode) node).getLabel();
				String right = "00000" + Integer.toString(i);
				String labelString = "L" + right.substring(right.length() - 5);
				labelStringMap.put(label, labelString);
			}
		}
	}


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
		this.currentLine = line;
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
		}
		
		// Create an integer to record a jump/exception 
		if (config.recordCatch()) {
			lastDataIdVar = newLocal(Type.INT_TYPE);
			super.visitLdcInsn(0);
			generateNewVarInsn(Opcodes.ISTORE, lastDataIdVar);
		}
		
		if (!methodName.equals("<init>")) { // In a constructor, a try block cannot start before a super() call.
			super.visitLabel(startLabel);
			isStartLabelLocated = true;
		}

		// Generate instructions to record parameters
		MethodParameters params = new MethodParameters(methodDesc);

		int varIndex = 0; // Index for local variable table
		int receiverOffset = 0;

		// Record an entry event with a receiver object
		if (hasReceiver()) { 
			if (isConstructor()) {
				generateLogging(EventType.METHOD_ENTRY, Descriptor.Void, "Receiver=uninitialized");
			} else { // An instance method
				super.visitVarInsn(Opcodes.ALOAD, 0);
				generateLogging(EventType.METHOD_ENTRY, Descriptor.Object, "Index=0,Receiver=true");
			}
			varIndex = 1;
			receiverOffset = 1;
		} else {
			generateLogging(EventType.METHOD_ENTRY, Descriptor.Void, "Receiver=false");
		}

		if (config.recordParameters()) {
			// Record Remaining parameters
			int paramIndex = 0;
			while (paramIndex < params.size()) {
				super.visitVarInsn(params.getLoadInstruction(paramIndex), varIndex);
				generateLogging(EventType.METHOD_PARAM, params.getRecordDesc(paramIndex), "Index=" + Integer.toString(paramIndex + receiverOffset));
				varIndex += params.getWords(paramIndex);
				paramIndex++;
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
		catchBlockInfo.put(handler, "BlockType=" + block + ",ExceptionType=" + type + ",Start=" + getLabelString(start) + ",End=" + getLabelString(end) + ",Handler=" + getLabelString(handler));
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

		if (config.recordCatch() && catchBlockInfo.containsKey(label)) {
			// If the label is a catch block, record the previous location and an exception.
			generateNewVarInsn(Opcodes.ILOAD, lastDataIdVar);
			generateLogging(EventType.CATCH_LABEL, Descriptor.Integer, "Label=" + getLabelString(label));
			generateLoggingPreservingStackTop(EventType.CATCH, Descriptor.Object, catchBlockInfo.get(label));
		} else if (config.recordLabel()) {
			// For a regular label, record a previous location.
			generateNewVarInsn(Opcodes.ILOAD, lastDataIdVar);
			generateLogging(EventType.LABEL, Descriptor.Integer, "Label=" + getLabelString(label));
		}

		instructionIndex++;
	}

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
			int dataId = nextDataId(EventType.JUMP, Descriptor.Void, "Instruction=" + OpcodesUtil.getString(opcode) + "JumpTo=" + getLabelString(label));
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
				generateLogging(EventType.METHOD_EXCEPTIONAL_EXIT_LABEL, Descriptor.Integer, "ExceptionalExit");
			}
			generateLoggingPreservingStackTop(EventType.METHOD_EXCEPTIONAL_EXIT, Descriptor.Object, "ExceptionalExit-Throw");
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
	 * NEW, ANEWARRAY, INSTANCEOF instructions.
	 */
	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (opcode == Opcodes.NEW) {
			super.visitTypeInsn(opcode, type);
			if (config.recordMethodCall() && config.recordParameters()) {
				int dataId = generateLogging(EventType.NEW_OBJECT, Descriptor.Void, "Type=" + type);
				newInstructionStack.push(new ANewInstruction(dataId, type));
			} else {
				// A tentative item is added to recognize "this()" and "super()" in visitMethodInsn.  
				newInstructionStack.push(new ANewInstruction(-1, type)); 
			}
		} else if (opcode == Opcodes.ANEWARRAY) {
			if (config.recordArrayInstructions()) {
				int dataId = generateLoggingPreservingStackTop(EventType.NEW_ARRAY, Descriptor.Integer, "ElementType=" + type);
				super.visitTypeInsn(opcode, type); // -> stack: [ARRAYREF]
				generateLoggingPreservingStackTop(EventType.NEW_ARRAY_RESULT, Descriptor.Object, "Parent=" + dataId);
			} else {
				super.visitTypeInsn(opcode, type);
			}
			afterNewArray = true;
		} else if (opcode == Opcodes.INSTANCEOF) {
			if (config.recordObject()) {
				int dataId = generateLoggingPreservingStackTop(EventType.OBJECT_INSTANCEOF, Descriptor.Object, "INSTANCEOF " + type);
				super.visitTypeInsn(opcode, type); // -> [ result ]
				generateLoggingPreservingStackTop(EventType.OBJECT_INSTANCEOF_RESULT, Descriptor.Boolean, "Parent=" + dataId);
			} else {
				super.visitTypeInsn(opcode, type); // -> [ result ]
			}
		} else {
			super.visitTypeInsn(opcode, type);
		}
		instructionIndex++;
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (opcode == Opcodes.NEWARRAY) {
			if (config.recordArrayInstructions()) {
				// operand indicates an element type. 
				// Record [SIZE] and [ARRAYREF]
				int dataId = generateLoggingPreservingStackTop(EventType.NEW_ARRAY, Descriptor.Integer, "ElementType=" + OpcodesUtil.getArrayElementType(operand));
				super.visitIntInsn(opcode, operand); // -> stack: [ARRAYREF]
				generateLoggingPreservingStackTop(EventType.NEW_ARRAY_RESULT, Descriptor.Object, "Parent=" + dataId);
			} else {
				super.visitIntInsn(opcode, operand);
			}
			afterNewArray = true;
		} else {
			super.visitIntInsn(opcode, operand);
		}
		instructionIndex++;
	}
	

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

			String callSig = "Instruction=" + OpcodesUtil.getString(opcode) + ",Owner=" + owner + ",Name=" + name + ",Desc=" + desc;

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
					String label = "CallType=ReceiverNotInitialized,";
					if (newInstruction != null) {
						label = label + "NewParent=" + newInstruction.getDataId() + ",";
					}
					firstDataId = generateLogging(EventType.CALL, Descriptor.Void, label + callSig);
					offset = 1;
				} else if (hasReceiver) { // For a regular non-static method,
											// duplicate and record the object
											// reference.
					super.visitInsn(Opcodes.DUP);
					firstDataId = generateLogging(EventType.CALL, Descriptor.Object, "CallType=Regular," + callSig);
					offset = 1;
				} else { // otherwise, no receivers.
					firstDataId = generateLogging(EventType.CALL, Descriptor.Void, "CallType=Static," + callSig);
					offset = 0;
				}

				// Record remaining parameters
				int paramIndex = 0;
				while (paramIndex < params.size()) {
					generateNewVarInsn(params.getLoadInstruction(paramIndex), params.getLocalVar(paramIndex));
					generateLogging(EventType.CALL_PARAM, params.getRecordDesc(paramIndex), "CallParent=" + firstDataId + ",Index=" + Integer.toString(paramIndex + offset) + ",Type=" + params.getType(paramIndex).getDescriptor());
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
				generateLoggingPreservingStackTop(EventType.CALL_RETURN, d, "CallParent=" + firstDataId + ",Type=" + returnDesc);

				if (isConstructorChain) {
					if (config.recordExecution()) {
						// Record an object initialized by this() or super()
						generateLogging(EventType.METHOD_OBJECT_INITIALIZED, Descriptor.Object, "");
					}
					afterInitialization = true;
				} else if (newInstruction != null) {
					// Record an object created by "new X()"
					generateLogging(EventType.NEW_OBJECT_CREATED, Descriptor.Object, "CallParent=" + firstDataId + ",NewParent=" + newInstruction.getDataId());
				}

			} else { // !recordParameters
				// Call an occurrence of a call
				String label = (newInstruction != null) ? "NewParent=" + newInstruction.getDataId() + "," + callSig : callSig;
				int callId = generateLogging(EventType.CALL, Descriptor.Void, label);

				// Store the current location for exceptional exit
				generateLocationUpdate(callId);

				// Call the original method
				super.visitMethodInsn(opcode, owner, name, desc, itf);

				// Reset the current location
				generateLocationUpdate(0);

				// Record return event
				generateLogging(EventType.CALL_RETURN, Descriptor.Void, "Parent=" + callId);
				
			}

		} else {  

			super.visitMethodInsn(opcode, owner, name, desc, itf);

			// Constructor call
			if (isConstructorChain) {
				if (config.recordExecution()) {
					super.visitVarInsn(Opcodes.ALOAD, 0);
					generateLogging(EventType.METHOD_OBJECT_INITIALIZED, Descriptor.Object, "");
				}
				afterInitialization = true;
			}
		}

		// If this call is a constructor-chain (super()/this() at the beginning
		// of a constructor), start a try block to record an exception thrown by
		// the remaining code.
		// Because Java Verifier does not allow "try { super(); } catch ... ",
		// this code generate "super(); try { ... }".
		if (isConstructorChain && config.recordExecution()) {
			super.visitLabel(startLabel);
			isStartLabelLocated = true;
		}

		instructionIndex++;
	}

	private void generateLocationUpdate(int dataId) {
		super.visitLdcInsn(dataId);
		generateNewVarInsn(Opcodes.ISTORE, lastDataIdVar);
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if (config.recordArrayInstructions()) {
			int dataId = nextDataId(EventType.MULTI_NEW_ARRAY, Descriptor.Object, "Type=" + desc + ",Dimensions="  + dims);
			nextDataId(EventType.MULTI_NEW_ARRAY_OWNER, Descriptor.Object, "Parent=" + dataId);
			nextDataId(EventType.MULTI_NEW_ARRAY_ELEMENT, Descriptor.Object, "Parent=" + dataId);
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

	@Override
	public void visitIincInsn(int var, int increment) {
		super.visitIincInsn(var, increment);
		if (config.recordLocalAccess()) {
			super.visitVarInsn(Opcodes.ILOAD, var);
			LocalVariableNode local = variables.getLoadVar(var);
			if (local != null) {
				generateLogging(EventType.LOCAL_INCREMENT, Descriptor.Integer, "Increment=" + increment + ",Var=" + var + ",Name=" + local.name + ",Type=" + local.desc); 
			} else {
				generateLogging(EventType.LOCAL_INCREMENT, Descriptor.Integer, "Increment=" + increment + ",Var=" + var + ",Name=(Unavailable),Type=I"); 
			}
		}
		instructionIndex++;
	}
	
	/**
	 * Extract a descriptor representing the return type of this method.
	 */
	private String getReturnValueDesc(String methodDesc) {
		int index = methodDesc.indexOf(')');
		String returnTypeName = methodDesc.substring(index + 1);
		return returnTypeName;
	}
	
	private Descriptor getDescForReturn() {
		int index = methodDesc.lastIndexOf(')');
		assert index >= 0: "Invalid method descriptor " + methodDesc;
		String returnValueType = methodDesc.substring(index+1);
		return Descriptor.get(returnValueType);
	}

	@Override
	public void visitInsn(int opcode) {

		if (OpcodesUtil.isReturn(opcode)) {
			if (config.recordExecution()) {
				generateLoggingPreservingStackTop(EventType.METHOD_NORMAL_EXIT, getDescForReturn(), "");
			}
			super.visitInsn(opcode);
		} else if (opcode == Opcodes.ATHROW) {
			if (config.recordExecution() || config.recordLabel()) {
				int dataId = generateLoggingPreservingStackTop(EventType.METHOD_THROW, Descriptor.Object, "");
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
				int arrayLengthId = generateLoggingPreservingStackTop(EventType.ARRAY_LENGTH, Descriptor.Object, "");
				super.visitInsn(opcode); // -> [ arraylength ]
				generateLoggingPreservingStackTop(EventType.ARRAY_LENGTH_RESULT, Descriptor.Integer, "Parent=" + arrayLengthId);
			} else {
				super.visitInsn(opcode);
			}
		} else if (opcode == Opcodes.MONITORENTER) {
			if (config.recordSynchronization()) {
				super.visitInsn(Opcodes.DUP);
				super.visitInsn(Opcodes.DUP);
				// Monitor enter fails if the argument is null.
				int dataid = generateLogging(EventType.MONITOR_ENTER, Descriptor.Object, "");
				generateLocationUpdate(dataid);
				super.visitInsn(opcode); // Enter the monitor
				generateLocationUpdate(0);
				generateLogging(EventType.MONITOR_ENTER_RESULT, Descriptor.Object, "");
			} else {
				super.visitInsn(opcode);
			}
		} else if (opcode == Opcodes.MONITOREXIT) {
			if (config.recordSynchronization()) {
				super.visitInsn(Opcodes.DUP); // -> [objectref, objectref]
				generateLogging(EventType.MONITOR_EXIT, Descriptor.Object, "");
				super.visitInsn(opcode);
			} else {
				super.visitInsn(opcode);
			}
		} else if (opcode == Opcodes.DDIV || 
				opcode == Opcodes.FDIV ||
				opcode == Opcodes.IDIV ||
				opcode == Opcodes.LDIV) {
			int dataId = nextDataId(EventType.DIVIDE, Descriptor.Void, "DIV");
			generateLocationUpdate(dataId);
			super.visitInsn(opcode);
			generateLocationUpdate(0);
		} else {
			super.visitInsn(opcode);
		}
		instructionIndex++;
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		if (config.recordMethodCall()) {
			// Duplicate an object reference to record the created object
			StringBuilder sig = new StringBuilder();
			sig.append("INVOKEDYNAMIC,Name=" + name + ",Desc=");
			sig.append(",Bootstrap=" + bsm.getOwner());
			sig.append(",BootstrapMethod=" + bsm.getName());
			sig.append(",BootstrapDesc=" + bsm.getDesc());
			for (int i=0; i<bsmArgs.length; i++) {
				sig.append(",BootstrapArgs" + i + "=" + bsmArgs[i].getClass().getName());
			}
			String label = sig.toString();
			System.out.println(label);

			// Call the original method
			super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
			
			generateLoggingPreservingStackTop(EventType.INVOKE_DYNAMIC, Descriptor.Object, label);
		} else {
			super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
		}
		instructionIndex++;
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		super.visitLookupSwitchInsn(dflt, keys, labels);
		instructionIndex++;
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		super.visitTableSwitchInsn(min, max, dflt, labels);
		instructionIndex++;
	}

	@Override
	public void visitLdcInsn(Object cst) {
		super.visitLdcInsn(cst); // -> [object]
		if (config.recordObject() && 
			!(cst instanceof Integer) && !(cst instanceof Long) && 
			!(cst instanceof Double) && !(cst instanceof Float)) {
			generateLoggingPreservingStackTop(EventType.OBJECT_CONSTANT_LOAD, Descriptor.Object, "Type=" + cst.getClass().getName());
		}
		instructionIndex++;
	}

	private void generateRecordArrayLoad(int opcode) {
		Descriptor elementDesc = OpcodesUtil.getDescForArrayLoad(opcode);
		String desc = "([" + elementDesc.getString() + "II)V";
		if (elementDesc.getString().equals("B")) desc = "(Ljava/lang/Object;II)V"; // to accept byte[] and boolean[]

		// Create dataId used in Logging class
		int dataId = nextDataId(EventType.ARRAY_LOAD, Descriptor.Object, "Opcode=" + opcode);
		nextDataId(EventType.ARRAY_LOAD_INDEX, Descriptor.Integer, "Parent=" + dataId); 
		nextDataId(EventType.ARRAY_LOAD_RESULT, elementDesc, "Parent=" + dataId);

		super.visitInsn(Opcodes.DUP2); // stack: [array, index, array, index]
		super.visitLdcInsn(dataId); // [array, index, array, index, id]
		super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordArrayLoad", desc, false);

		generateLocationUpdate(dataId);

		// the original instruction [array, index] -> [value]
		super.visitInsn(opcode);

		generateLocationUpdate(0);
	}

	private void generateRecordArrayStore(int opcode) {
		String elementDesc = OpcodesUtil.getDescForArrayStore(opcode);
		String methodDesc = "(Ljava/lang/Object;I" + elementDesc + "I)V";

		int arrayDataId = nextDataId(EventType.ARRAY_STORE, Descriptor.Object, "Opcode=" + opcode);
		nextDataId(EventType.ARRAY_STORE_INDEX, Descriptor.Integer, "Parent=" + arrayDataId);
		nextDataId(EventType.ARRAY_STORE_VALUE, Descriptor.get(elementDesc), "Parent=" + arrayDataId);

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


	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (!config.recordFieldAccess()) {
			super.visitFieldInsn(opcode, owner, name, desc);
			instructionIndex++;
			return;
		}

		String label = "Owner=" + owner + ",FieldName=" + name + ",Type=" + desc;

		if (opcode == Opcodes.GETSTATIC) {
			// Record a resultant value
			super.visitFieldInsn(opcode, owner, name, desc); // [] -> [ value ]
			generateLoggingPreservingStackTop(EventType.GET_STATIC_FIELD, Descriptor.get(desc), label);
			
		} else if (opcode == Opcodes.PUTSTATIC) {
			// Record a new value
			generateLoggingPreservingStackTop(EventType.PUT_STATIC_FIELD, Descriptor.get(desc), label);
			super.visitFieldInsn(opcode, owner, name, desc);

		} else if (opcode == Opcodes.GETFIELD) {
			int fieldDataId = generateLoggingPreservingStackTop(EventType.GET_INSTANCE_FIELD, Descriptor.Object, label);

			generateLocationUpdate(fieldDataId);

			// Execute GETFIELD
			super.visitFieldInsn(opcode, owner, name, desc); // -> [value]

			generateLocationUpdate(0);

			// Record the result
			generateLoggingPreservingStackTop(EventType.GET_INSTANCE_FIELD_RESULT, Descriptor.get(desc), "Parent=" + fieldDataId + "," + label);

		} else {
			assert opcode == Opcodes.PUTFIELD;
			if (afterInitialization) {
				// stack: [object, value]
				if (desc.equals("D") || desc.equals("J")) {
					int local = newLocal(OpcodesUtil.getAsmType(desc));
					// Store a value to a local variable, record an object, and then load the value.
					generateNewVarInsn(OpcodesUtil.getStoreInstruction(desc), local); 
					int fieldDataId = generateLoggingPreservingStackTop(EventType.PUT_INSTANCE_FIELD, Descriptor.Object, label);
					generateNewVarInsn(OpcodesUtil.getLoadInstruction(desc), local); 

					// Record a value.
					generateLoggingPreservingStackTop(EventType.PUT_INSTANCE_FIELD_VALUE, Descriptor.get(desc), "Parent=" + fieldDataId + "," + label);

					generateLocationUpdate(fieldDataId);
					
					// Original Instruction
					super.visitFieldInsn(opcode, owner, name, desc);

					generateLocationUpdate(0);

				} else {
					super.visitInsn(Opcodes.DUP2);
					super.visitInsn(Opcodes.SWAP); // -> [object, value, value, object]
					int fieldDataId = generateLogging(EventType.PUT_INSTANCE_FIELD, Descriptor.Object, label);
					generateLogging(EventType.PUT_INSTANCE_FIELD_VALUE, Descriptor.get(desc), "Parent=" + fieldDataId + "," + label);

					generateLocationUpdate(fieldDataId);
					super.visitFieldInsn(opcode, owner, name, desc);
					generateLocationUpdate(0);
				}
			} else {
				// Before the target object is initialized, we cannot record the object.
				generateLoggingPreservingStackTop(EventType.PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION, Descriptor.get(desc), label);
				super.visitFieldInsn(opcode, owner, name, desc);
			}
		}
		instructionIndex++;
	}

	
	private int nextDataId(EventType eventType, Descriptor desc, String label) {
//		assert !label.contains(WeavingInfo.SEPARATOR) : "Location ID cannot includes WeavingInfo.SEPARATOR(" + WeavingInfo.SEPARATOR + ").";
		return weavingInfo.nextDataId(currentLine, instructionIndex, eventType, desc, label);
	}


	@Override
	public void visitVarInsn(int opcode, int var) {
		if (config.recordLocalAccess()) {
			Descriptor d = OpcodesUtil.getDescForStore(opcode);
			if (d != null) { // isStore
				LocalVariableNode local = variables.getStoreVar(instructionIndex, var);
				if (local != null) {
					generateLoggingPreservingStackTop(EventType.LOCAL_STORE,  d, "Var=" + var + ",Name=" + local.name + ",Type=" + local.desc); 
				} else {
					generateLoggingPreservingStackTop(EventType.LOCAL_STORE,  d, "Var=" + var + ",Name=(Unavailable),Type=" + d.getString()); 
				}
			} else if (opcode == Opcodes.RET) {
				d = Descriptor.Integer;
				super.visitVarInsn(Opcodes.ILOAD, var);
				generateLogging(EventType.RET,  d, "Var=" + var); 
				super.visitVarInsn(Opcodes.ISTORE, var);
			}
		}

		super.visitVarInsn(opcode, var);
		
		if (config.recordLocalAccess()) {
			Descriptor d = OpcodesUtil.getDescForLoad(opcode);
			if (d != null) { // isLoad
				if (!(hasReceiver() && var == 0)) {  // Record variables except for "this"
					LocalVariableNode local = variables.getLoadVar(var);
					if (local != null) {
						Descriptor localDesc = Descriptor.get(local.desc);
						generateLoggingPreservingStackTop(EventType.LOCAL_LOAD,  localDesc, "Var=" + var + ",Name=" + local.name + ",Type=" + local.desc); 
					} else {
						generateLoggingPreservingStackTop(EventType.LOCAL_LOAD,  d, "Var=" + var + ",Name=(Unavailable),Type=" + d.getString()); 
					}
				}
			}
		}
		instructionIndex++;
	}

	/**
	 * Use new local variables created by newLocal. This method does not use
	 * super.visitVarInsn because visitVarInsn will renumber variable index. (A
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
	private int generateLogging(EventType eventType, Descriptor valueDesc, String label) {
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
	private int generateLoggingPreservingStackTop(EventType eventType, Descriptor valueDesc, String label) {
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

}
