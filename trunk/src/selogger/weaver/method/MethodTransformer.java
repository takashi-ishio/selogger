package selogger.weaver.method;

import selogger.EventType;
import selogger.weaver.LogLevel;
import selogger.weaver.WeavingInfo;

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

	private WeavingInfo weavingInfo;
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

	private int pcPositionVar;

	/**
	 * In a constructor, this flag becomes true after the super() is called.
	 */
	private boolean afterInitialization;

	private LogLevel logLevel;
	private boolean afterNewArray = false;

	public MethodTransformer(WeavingInfo w, String sourceFileName, String className, String outerClassName, int access,
			String methodName, String methodDesc, String signature, String[] exceptions, MethodVisitor mv,
			LogLevel logLevel) {
		super(Opcodes.ASM5, access, methodDesc, mv);
		this.weavingInfo = w;
		this.className = className;
		// this.outerClassName = outerClassName; // not used
		this.access = access;
		this.methodName = methodName;
		this.methodDesc = methodDesc;

		this.afterInitialization = !methodName.equals("<init>");
		this.afterNewArray = false;
		this.logLevel = logLevel;

		this.instructionIndex = 0;

		weavingInfo.startMethod(className, methodName, methodDesc, access, sourceFileName);
		weavingInfo.nextDataId(-1, -1, EventType.RESERVED, Descriptor.Void, className + "#" + methodName + "#" + methodDesc);
	}

	private boolean minimumLogging() {
		return this.logLevel == LogLevel.OnlyEntryExit;
	}

	private boolean ignoreArrayInit() {
		return this.logLevel != LogLevel.Normal;
	}
	
	public void setLocalVariables(List<?> localVariableNodes, InsnList instructions) {
		variables = new LocalVariables(localVariableNodes, instructions);
	}

	/**
	 * Create a list of labels in the method to be analyzed. These existing
	 * labels are stored in LocationIdMap.
	 * 
	 * @param instructions
	 */
	public void makeLabelList(InsnList instructions) {
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
		weavingInfo.finishMethod();
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

	private boolean hasReceiver() {
		return (access & Opcodes.ACC_STATIC) == 0;
	}
	
	/**
	 * Visiting a method body. Generate try { recordParams; body(); } finally {
	 * }.
	 */
	@Override
	public void visitCode() {

		super.visitCode();

		if (weavingInfo.recordExecution()) {
			super.visitTryCatchBlock(startLabel, endLabel, endLabel, "java/lang/Throwable");
			
			pcPositionVar = newLocal(Type.INT_TYPE);
			super.visitLdcInsn(0);
			generateNewVarInsn(Opcodes.ISTORE, pcPositionVar);

			if (!methodName.equals("<init>")) { // In a constructor, a try block cannot start before a super() call.
				super.visitLabel(startLabel);
				isStartLabelLocated = true;
			}

			boolean parameterExist = false;
			if (weavingInfo.recordParameters()) {

				// Generate instructions to record parameters
				MethodParameters params = new MethodParameters(methodDesc);

				boolean receiverInitialized = !methodName.equals("<init>");

				int varIndex = 0; // Index for local variable table
				int receiverOffset = 0;

				// Record an entry point
				generateLogging(EventType.METHOD_ENTRY, Descriptor.Void, "");

				// Receiver a receiver object as an ENTRY event.
				if (hasReceiver()) { // Does the method has a receiver object?
					if (receiverInitialized) { // A receiver object is
												// unrecordable until
												// initialization
						super.visitVarInsn(Opcodes.ALOAD, 0);
						generateLogging(EventType.FORMAL_PARAM, Descriptor.Object, "Index=0,Receiver=true");
						parameterExist = true;
					}
					varIndex = 1;
					receiverOffset = 1;
				}
				// Record Remaining parameters
				int paramIndex = 0;
				while (paramIndex < params.size()) {
					super.visitVarInsn(params.getLoadInstruction(paramIndex), varIndex);
					generateLogging(EventType.FORMAL_PARAM, params.getRecordDesc(paramIndex), "Index=" + Integer.toString(paramIndex + receiverOffset));
					varIndex += params.getWords(paramIndex);
					paramIndex++;
					parameterExist = true;
				}

			}
			if (!parameterExist) {
				// Record a method entry event without parameters
				generateLogging(EventType.METHOD_ENTRY, Descriptor.Void, "NoParam");
			}
		}
	}

	/**
	 * Record entry points of catch blocks. The method must be called BEFORE
	 * visit* methods for other instructions, according to the implementation of
	 * MethodNode class.
	 */
	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		super.visitTryCatchBlock(start, end, handler, type);

		// Record catch block information 
		String block = type != null ? "CATCH" : "FINALLY";
		catchBlockInfo.put(handler, "BlockType=" + block + ",ExceptionType=" + type + ",Start=" + getLabelString(start) + ",End=" + getLabelString(end) + ",Handler=" + getLabelString(handler));
	}

	@Override
	public void visitLabel(Label label) {
		variables.visitLabel(label);
		
		// Process the label
		super.visitLabel(label);
		if (weavingInfo.recordLabel() && !minimumLogging()) {
			generateNewVarInsn(Opcodes.ILOAD, pcPositionVar);
			generateLogging(EventType.LABEL, Descriptor.Integer, "Label=" + getLabelString(label));
		}

		// Generate logging code to identify an exceptional exit from a method call
		if (weavingInfo.recordMethodCall() && !minimumLogging()) {
			// if the label is a catch block, add a logging code
			if (catchBlockInfo.containsKey(label)) {
				// Record exception object
				generateLoggingPreservingStackTop(EventType.CATCH, Descriptor.Exception, catchBlockInfo.get(label));
			}
		}

		instructionIndex++;
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		super.visitFrame(type, nLocal, local, nStack, stack);
		instructionIndex++;
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		// Generate a data ID recorded by EVENT_LABEL. 
		int dataId = nextDataId(EventType.JUMP, Descriptor.Void, "JumpTo=" + getLabelString(label));
		super.visitLdcInsn(dataId);
		generateNewVarInsn(Opcodes.ISTORE, pcPositionVar);
		super.visitJumpInsn(opcode, label);
		instructionIndex++;
	}

	/**
	 * Finalize the method. Generate code to record an exception going to a
	 * caller.
	 */
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		assert newInstructionStack.isEmpty();
		assert isStartLabelLocated || !weavingInfo.recordExecution();

		if (weavingInfo.recordExecution()) {
			// Since visitMaxs is called at the end of a method, insert an
			// exception handler to record an exception in the method.
			// The conceptual code: catch (Throwable t) { recordExceptionalExit(t, LocationID); throw t; }
			super.visitLabel(endLabel);
			generateNewVarInsn(Opcodes.ILOAD, pcPositionVar);
			generateLogging(EventType.METHOD_EXCEPTIONAL_EXIT, Descriptor.Integer, "ExceptionalExit");
			super.visitInsn(Opcodes.DUP);
			generateLogging(EventType.METHOD_EXCEPTIONAL_EXIT_RETHROW, Descriptor.Exception, "ExceptionalExit-Rethrow");
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
			int dataId = generateLogging(EventType.NEW_OBJECT, Descriptor.Void, "Type=" + type);
			newInstructionStack.push(new ANewInstruction(dataId, type));
		} else if (opcode == Opcodes.ANEWARRAY) {
			if (weavingInfo.recordArrayInstructions() && !minimumLogging()) {
				int dataId = generateLoggingPreservingStackTop(EventType.NEW_ARRAY, Descriptor.Integer, "ElementType=" + type);
				super.visitTypeInsn(opcode, type); // -> stack: [ARRAYREF]
				generateLoggingPreservingStackTop(EventType.NEW_ARRAY_RESULT, Descriptor.Object, "Parent=" + dataId);
			} else {
				super.visitTypeInsn(opcode, type);
			}
			afterNewArray = true;
		} else if (opcode == Opcodes.INSTANCEOF && weavingInfo.recordMiscInstructions() && !minimumLogging()) {
			int dataId = generateLoggingPreservingStackTop(EventType.INSTANCEOF, Descriptor.Object, "INSTANCEOF " + type);

			super.visitTypeInsn(opcode, type); // -> [ result ]

			generateLoggingPreservingStackTop(EventType.INSTANCEOF_RESULT, Descriptor.Boolean, "Parent=" + dataId);
		} else {
			super.visitTypeInsn(opcode, type);
		}
		instructionIndex++;
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (opcode == Opcodes.NEWARRAY && !minimumLogging()) {
			if (weavingInfo.recordArrayInstructions()) {
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
		if (weavingInfo.recordMethodCall() && !minimumLogging()) {

			String callSig = "Instruction=" + OpcodesUtil.getCallInstructionName(opcode) + ",Owner=" + owner + ",Name=" + name + ",Desc=" + desc;

			if (weavingInfo.recordParameters()) {
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
					firstDataId = generateLogging(EventType.CALL, Descriptor.Void, "CallType=Static," +callSig);
					offset = 0;
				}

				// Record remaining parameters
				int paramIndex = 0;
				while (paramIndex < params.size()) {
					generateNewVarInsn(params.getLoadInstruction(paramIndex), params.getLocalVar(paramIndex));
					generateLogging(EventType.ACTUAL_PARAM, params.getRecordDesc(paramIndex), "CallParent=" + firstDataId + ",Index=" + Integer.toString(paramIndex + offset) + ",Type=" + params.getType(paramIndex).getDescriptor());
					paramIndex++;
				}

				// Restore parameters from local variables
				for (int i = 0; i < params.size(); i++) {
					generateNewVarInsn(params.getLoadInstruction(i), params.getLocalVar(i));
				}

				// Store the current location for exceptional exit
				super.visitLdcInsn(firstDataId);
				generateNewVarInsn(Opcodes.ISTORE, pcPositionVar);
				// Call the original method
				super.visitMethodInsn(opcode, owner, name, desc, itf);
				// Reset the current location for exceptional exit
				super.visitLdcInsn(0);
				generateNewVarInsn(Opcodes.ISTORE, pcPositionVar);

				// Record a return value or an initialized object.
				if (receiverNotInitialized) { // == newInstruction != null ||
												// isConstructorChain
					if (newInstruction != null) {
						// Record an object created by "new X()"
						generateLogging(EventType.NEW_OBJECT_CREATION_COMPLETED, Descriptor.Object, "CallParent=" + firstDataId + ",NewParent=" + newInstruction.getDataId());
					} else if (isConstructorChain) {
						// Record an object initialized by this() or super()
						generateLogging(EventType.NEW_OBJECT_INITIALIZED, Descriptor.Object, "CallParent=" + firstDataId);
					} else {
						assert false;
					}
				} else {
					// record return value
					String returnDesc = getReturnValueDesc(desc);
					Descriptor d = Descriptor.get(returnDesc);
					generateLoggingPreservingStackTop(EventType.CALL_RETURN, d, "CallParent=" + firstDataId + ",Type=" + returnDesc);
				}

			} else { // !recordParameters
				// Call an occurrence of a call
				String label = (newInstruction != null) ? "NewParent=" + newInstruction.getDataId() + "," + callSig : callSig;
				int callId = generateLogging(EventType.CALL, Descriptor.Void, label);

				// Call the original method
				super.visitMethodInsn(opcode, owner, name, desc, itf);

				generateLogging(EventType.CALL_RETURN, Descriptor.Void, "Parent=" + callId);
			}

		} else { // A regular method call if (weavingInfo.recordMethodCall() && !minimumLogging()) {
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}

		// If this call is a constructor-chain (super()/this() at the beginning
		// of a constructor), start a try block to record an exception thrown by
		// the remaining code.
		// Because Java Verifier does not allow "try { super(); } catch ... ",
		// this code generate "super(); try { ... }".
		if (weavingInfo.recordExecution() && isConstructorChain) {
			super.visitLabel(startLabel);
			isStartLabelLocated = true;
		}

		instructionIndex++;
	}

	private String getReturnValueDesc(String methodDesc) {
		int index = methodDesc.indexOf(')');
		String returnTypeName = methodDesc.substring(index + 1);
		return returnTypeName;
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if (weavingInfo.recordArrayInstructions() && !minimumLogging()) {
			int dataId = nextDataId(EventType.MULTI_NEW_ARRAY, Descriptor.Object, "Type=" + desc + ",Dimensions="  + dims);
			nextDataId(EventType.MULTI_NEW_ARRAY_CONTENT, Descriptor.Object, "Parent=" + dataId);
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
		instructionIndex++;
	}

	@Override
	public void visitInsn(int opcode) {

		if (OpcodesUtil.isReturn(opcode)) {
			if (weavingInfo.recordExecution()) {
				generateLoggingPreservingStackTop(EventType.METHOD_NORMAL_EXIT, OpcodesUtil.getDescForReturn(opcode), "");
			}
			super.visitInsn(opcode);
		} else if (opcode == Opcodes.ATHROW) {
			if (weavingInfo.recordExecution()) {
				generateLoggingPreservingStackTop(EventType.THROW, Descriptor.Exception, "");
			}
			super.visitInsn(opcode);
		} else if (!minimumLogging()) {
			if (OpcodesUtil.isArrayLoad(opcode)) {
				if (weavingInfo.recordArrayInstructions()) {
					generateRecordArrayLoad(opcode);
				} else {
					super.visitInsn(opcode);
				}
			} else if (OpcodesUtil.isArrayStore(opcode)) {
				if (weavingInfo.recordArrayInstructions() && !(ignoreArrayInit() && afterNewArray)) {
					generateRecordArrayStore(opcode);
				} else {
					super.visitInsn(opcode);
				}
			} else if (opcode == Opcodes.ARRAYLENGTH) {
				if (weavingInfo.recordArrayInstructions()) {
					int arrayLengthId = generateLoggingPreservingStackTop(EventType.ARRAY_LENGTH, Descriptor.Object, "");
					super.visitInsn(opcode); // -> [ arraylength ]
					generateLoggingPreservingStackTop(EventType.ARRAY_LENGTH_RESULT, Descriptor.Integer, "Parent=" + arrayLengthId);
				} else {
					super.visitInsn(opcode);
				}
			} else if (opcode == Opcodes.MONITORENTER) {
				if (weavingInfo.recordMiscInstructions()) {
					super.visitInsn(Opcodes.DUP); // -> [objectref, objectref]
					super.visitInsn(opcode); // Enter the monitor
					generateLogging(EventType.MONITOR_ENTER, Descriptor.Object, "");
				} else {
					super.visitInsn(opcode);
				}
			} else if (opcode == Opcodes.MONITOREXIT) {
				if (weavingInfo.recordMiscInstructions()) {
					super.visitInsn(Opcodes.DUP); // -> [objectref, objectref]
					generateLogging(EventType.MONITOR_EXIT, Descriptor.Object, "");
					super.visitInsn(opcode);
				} else {
					super.visitInsn(opcode);
				}
			} else {
				super.visitInsn(opcode);
			}
		} else {
			super.visitInsn(opcode);
		}
		instructionIndex++;
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		if (weavingInfo.recordMethodCall() && !minimumLogging()) {

			MethodParameters params = new MethodParameters(desc);
			// Store parameters to additional local variables.
			for (int i = params.size() - 1; i >= 0; i--) {
				int local = super.newLocal(params.getType(i));
				params.setLocalVar(i, local);
				generateNewVarInsn(params.getStoreInstruction(i), local);
			}
			// Duplicate an object reference to record the created object
			StringBuilder sig = new StringBuilder();
			sig.append("CallType=Dynamic");
			sig.append(",Bootstrap=" + bsm.getOwner());
			sig.append(",BootstrapMethod=" + bsm.getName());
			sig.append(",BootstrapDesc=" + bsm.getDesc());
			for (int i=0; i<bsmArgs.length; i++) {
				sig.append(",BootstrapArgs" + i + "=" + bsmArgs[i].getClass().getName());
			}
			
			int firstDataId = generateLoggingPreservingStackTop(EventType.CALL, Descriptor.Object, sig.toString());

			// Load each parameter and record its value
			for (int i = 0; i < params.size(); i++) {
				generateNewVarInsn(params.getLoadInstruction(i), params.getLocalVar(i));
				generateLogging(EventType.ACTUAL_PARAM, params.getRecordDesc(i), "CallParent=" + firstDataId + ",Index=" + (i+1) + "," + "Type=" + params.getType(i).getDescriptor());
			}
			// Load parameters and invoke the original call
			for (int i = 0; i < params.size(); i++) {
				generateNewVarInsn(params.getLoadInstruction(i), params.getLocalVar(i));
			}
			super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);

			// record return value
			String returnDesc = getReturnValueDesc(desc);
			generateLoggingPreservingStackTop(EventType.CALL_RETURN, Descriptor.get(returnDesc), "CallParent=" + firstDataId + ",Type=" + returnDesc);
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
		if (weavingInfo.recordMiscInstructions() && !(cst instanceof Integer) && !(cst instanceof Long)
				&& !(cst instanceof Double) && !(cst instanceof Float)) {
			generateLoggingPreservingStackTop(EventType.CONSTANT_OBJECT_LOAD, Descriptor.Object, "Type=" + cst.getClass().getName());
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
		nextDataId(EventType.ARRAY_LOAD_FAIL, Descriptor.Void, "Parent=" + dataId);

		super.visitInsn(Opcodes.DUP2); // stack: [array, index, array, index]
		super.visitLdcInsn(dataId); // [array, index, array, index, id]
		super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordArrayLoad", desc, false);

		// the original instruction [array, index] -> [value]
		super.visitInsn(opcode);
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

		super.visitInsn(opcode);
	}


	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (minimumLogging() || !weavingInfo.recordFieldAccess()) {
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

			// Execute GETFIELD
			super.visitFieldInsn(opcode, owner, name, desc); // -> [value]

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

					// Original Instruction
					super.visitFieldInsn(opcode, owner, name, desc);

				} else {
					super.visitInsn(Opcodes.DUP2);
					super.visitInsn(Opcodes.SWAP); // -> [object, value, value, object]
					int fieldDataId = generateLogging(EventType.PUT_INSTANCE_FIELD, Descriptor.Object, label);
					generateLogging(EventType.PUT_INSTANCE_FIELD_VALUE, Descriptor.get(desc), "Parent=" + fieldDataId + "," + label);

					super.visitFieldInsn(opcode, owner, name, desc);
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

	/**
	 * The tool does not record any information about local variable
	 * manipulation.
	 */
	@Override
	public void visitVarInsn(int opcode, int var) {
		if (!minimumLogging()) {
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
		
		if (!minimumLogging()) {
			Descriptor d = OpcodesUtil.getDescForLoad(opcode);
			if (d != null) { // isLoad
				if (!(hasReceiver() && var == 0)) {  // Record variables except for "this"
					LocalVariableNode local = variables.getLoadVar(var);
					if (local != null) {
						generateLoggingPreservingStackTop(EventType.LOCAL_LOAD,  d, "Var=" + var + ",Name=" + local.name + ",Type=" + local.desc); 
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
