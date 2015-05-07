package selogger.weaver;

import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.HashMap;
import java.util.Stack;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.TryCatchBlockSorter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;


public class MethodTransformer extends LocalVariablesSorter {
	
	public static final String LOGGER_CLASS = "selogger/logging/Logging";
	
	private WeavingInfo weavingInfo;
	private int currentLine;
	private String className;
	private int access;
	private String methodName;
	private String methodDesc;
	private int instructionIndex;
	
	private Label startLabel = new Label();
	private Label endLabel = new Label();
	private TObjectLongHashMap<Label> catchBlockToLocationId = new TObjectLongHashMap<Label>();
	private boolean isStartLabelLocated;
	private HashMap<Label, String> labelStringMap = new HashMap<Label, String>();

	private Stack<NewInstruction> newInstructionStack = new Stack<NewInstruction>();
	

	/**
	 * In a constructor, this flag becomes true after the super() is called. 
	 */
	private boolean afterInitialization;

	private LogLevel logLevel;
	private boolean afterNewArray = false;
	
	public MethodTransformer(WeavingInfo w, String sourceFileName, String className, String outerClassName, int access, String methodName, String methodDesc, String signature, String[] exceptions, MethodVisitor mv, LogLevel logLevel) {
		super(Opcodes.ASM5, access, methodDesc, new TryCatchBlockSorter(mv, access, methodName, methodDesc, signature, exceptions));
		this.weavingInfo = w;
		this.className = className;
		//this.outerClassName = outerClassName; // not used
		this.access = access;
		this.methodName = methodName;
		this.methodDesc = methodDesc;

		this.afterInitialization = !methodName.equals("<init>");
		this.afterNewArray = false;
		this.logLevel = logLevel;
		
		this.instructionIndex = 0;

		weavingInfo.startMethod(className, methodName, methodDesc, access, sourceFileName);
		weavingInfo.nextLocationId(-1, -1, -1, "<METHOD>");
	}
	
	private boolean minimumLogging() {
		return this.logLevel == LogLevel.OnlyEntryExit;
	}
	
	private boolean ignoreArrayInit() {
		return this.logLevel != LogLevel.Normal;
	}
	
	/**
	 * Create a list of labels in the method to be analyzed.
	 * These existing labels are stored in LocationIdMap. 
	 * @param instructions
	 */
	public void makeLabelList(InsnList instructions) {
		for (int i=0; i<instructions.size(); ++i) {
			AbstractInsnNode node = instructions.get(i);
			if (node.getType() == AbstractInsnNode.LABEL) {
				Label label = ((LabelNode)node).getLabel();
				String right = "00000" + Integer.toString(i);
				String labelString = "L" + right.substring(right.length()-5);
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
		if (label == startLabel) return "LSTART";
		else if (label == endLabel) return "LEND";
		
		assert labelStringMap.containsKey(label): "Unknown label";
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
	
	@Override
	public void visitCode() {
		
		super.visitCode();

		if (weavingInfo.recordExecution()) {
			super.visitTryCatchBlock(startLabel, endLabel, endLabel, "java/lang/Throwable");
			if (!methodName.equals("<init>")) { // In a constructor, a try block cannot start before a super() call.
				super.visitLabel(startLabel);
				isStartLabelLocated = true;
			}
			long locationId = nextLocationId("ENTRY");
			super.visitLdcInsn(locationId);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordMethodEntry", "(J)V", false);
			
			if (weavingInfo.recordParameters()) {
				// Generate instructions to record parameters
				MethodParameters params = new MethodParameters(methodDesc);
				int paramIndex = 0;
				int varIndex = 0;
				
				// Receiver object 
				if ((access & Opcodes.ACC_STATIC) == 0) { // Does the method has a receiver object?
					if (!methodName.equals("<init>")) {   // If the method is a constructor, a receiver object is unrecordable until initialization
						super.visitVarInsn(Opcodes.ALOAD, 0);
						super.visitLdcInsn(0); // param index = 0
						super.visitLdcInsn(locationId);
						super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordFormalParam", "(Ljava/lang/Object;IJ)V", false);
					}
					paramIndex = 1;
					varIndex = 1;
				}
				
				// Parameters except for receiver
				for (int i=0; i<params.size(); i++) {
					super.visitVarInsn(params.getLoadInstruction(i), varIndex);
		            super.visitLdcInsn(paramIndex);
		            super.visitLdcInsn(locationId); 
					super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordFormalParam", "(" + params.getRecordDesc(i) + "IJ)V", false);
		            varIndex += params.getWords(i);
		            paramIndex++;
				}
			}
		}
	}
	
	/**
	 * Record entry points of catch blocks.
	 * The method must be called BEFORE visit* methods for other instructions, 
	 * according to the implementation of MethodNode class. 
	 */
	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		super.visitTryCatchBlock(start, end, handler, type);
		
		String block = "CATCH";
		if (type == null) block = "FINALLY";
		// Output label information based on the offset 
		long locationId = nextLocationId(block + ";" + type + ";" + getLabelString(start) + ";" + getLabelString(end) + ";" + getLabelString(handler));
		catchBlockToLocationId.put(handler, locationId);
	}
	
	@Override
	public void visitLabel(Label label) {
		// Process the label 
		super.visitLabel(label);
		
		// Generate logging code to identify an exceptional exit from a method call 
		if (weavingInfo.recordMethodCall() && !minimumLogging()) { 
			// if the label is a catch block, add a logging code 
			if (catchBlockToLocationId.containsKey(label)) {
				long locationId = catchBlockToLocationId.get(label);
				super.visitInsn(Opcodes.DUP); // [ exception ] -> [ exception, exception ]
				super.visitLdcInsn(locationId); // -> [ exception, exception, locationId ]
				super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordCatch", "(Ljava/lang/Object;J)V", false); 
			}
		} 

		if (weavingInfo.recordLabel() && !minimumLogging()) {
			long locationId = nextLocationId(getLabelString(label)); // use a different location id from CATCH. 
			super.visitLdcInsn(locationId); 
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordLabel", "(J)V", false); 
		}
		
		instructionIndex++;
	}
	
	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack,
			Object[] stack) {
		super.visitFrame(type, nLocal, local, nStack, stack);
		instructionIndex++;
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		super.visitJumpInsn(opcode, label);
		instructionIndex++;
	}
	
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		assert newInstructionStack.isEmpty();
		assert isStartLabelLocated || !weavingInfo.recordExecution();
		
		if (weavingInfo.recordExecution()) {
			// Since visitMaxs is called at the end of a method, insert an exception handler to record an exception in the method. 
			// The conceptual code: catch (Throwable t) { recordExceptionalExit(t, LocationID); throw t; }
			long locationId = nextLocationId("EXCEPTIONAL EXIT");
			super.visitLabel(endLabel);
			super.visitInsn(Opcodes.DUP);
			super.visitLdcInsn(locationId);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordExceptionalExit", "(Ljava/lang/Object;J)V", false);
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
	
	public void visitTypeInsn(int opcode, String type) {
		if (opcode == Opcodes.NEW) {
			super.visitTypeInsn(opcode, type);
			long locationId = nextLocationId("NEW " + type);
			newInstructionStack.push(new NewInstruction(locationId, type)); 
		} else if (opcode == Opcodes.ANEWARRAY) {
			if (weavingInfo.recordArrayInstructions() && !minimumLogging()) {
				long locationId = nextLocationId("ANEWARRAY " + type);
				super.visitInsn(Opcodes.DUP);
				super.visitTypeInsn(opcode, type); // -> stack: [SIZE, ARRAYREF] 
				super.visitInsn(Opcodes.DUP_X1);  // -> stack: [ARRAYREF, SIZE, ARRAYREF]
				super.visitLdcInsn(locationId);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordNewArray", "(ILjava/lang/Object;J)V", false);
			} else {
				super.visitTypeInsn(opcode, type); 
			}
			afterNewArray = true;
		} else if (opcode == Opcodes.INSTANCEOF && weavingInfo.recordMiscInstructions() && !minimumLogging()) {
			long locationId = nextLocationId("INSTANCEOF " + type);
			super.visitInsn(Opcodes.DUP); // -> [object, object]
			super.visitTypeInsn(opcode, type); // -> [ object, result ]
			super.visitInsn(Opcodes.DUP_X1);     // ->  [ result, object, result ]
			super.visitLdcInsn(locationId);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordInstanceOf", "(Ljava/lang/Object;ZJ)V", false);
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
				long locationId = nextLocationId("NEWARRAY " + getArrayElementType(operand));
				super.visitInsn(Opcodes.DUP); // -> stack: [SIZE, SIZE]
				super.visitIntInsn(opcode, operand); // -> stack: [SIZE, ARRAYREF]
				super.visitInsn(Opcodes.DUP_X1);  // -> stack: [ARRAYREF, SIZE, ARRAYREF]
				super.visitLdcInsn(locationId);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordNewArray", "(ILjava/lang/Object;J)V", false);
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
	public void visitVarInsn(int opcode, int var) {
		super.visitVarInsn(opcode, var);
		instructionIndex++;
	}
	
	/**
	 * Use new local variables created by newLocal.
	 * This method does not use super.visitVarInsn because 
	 * visitVarInsn will renumber variable index.
	 * (A return value of newLocal is a renumbered  index.)
	 * @param opcode
	 * @param local
	 */
	private void generateNewVarInsn(int opcode, int local) {
		if (mv != null) mv.visitVarInsn(opcode, local);
	}
	
	private long generateRecordCall(int opcode, String owner, String name, String desc, NewInstruction newInst) {
		String op;
		switch (opcode) {
		case Opcodes.INVOKESPECIAL:
			op = "INVOKESPECIAL ";
			break;
		case Opcodes.INVOKEINTERFACE:
			op = "INVOKEINTERFACE ";
			break;
		case Opcodes.INVOKEDYNAMIC:
			op = "INVOKEDYNAMIC ";
			break;
		case Opcodes.INVOKESTATIC:
			op = "INVOKESTATIC ";
			break;
		default:
			op = "INVOKEVIRTUAL ";
		}
		long locationId;
		// if the call is relevant to a new instruction, associate the location ID of the instruction with the call instruction. 
		if (newInst != null) {
			locationId = nextLocationId(op + owner + "#" + name + "#" + desc, newInst.getLocationId());
		} else {
			locationId = nextLocationId(op + owner + "#" + name + "#" + desc);
		}
		super.visitLdcInsn(locationId);
		super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordCall", "(J)V", false);
		return locationId;
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name,
			String desc, boolean itf) {

		// This method call is a constructor chain if the call initializes "this" object by this() or suepr().
		boolean isConstructorChain = name.equals("<init>") && methodName.equals("<init>") && newInstructionStack.isEmpty();    
		assert !isConstructorChain || opcode == Opcodes.INVOKESPECIAL: "A constructor chain must use INVOKESPECIAL.";

		// Pop a corresponding new instruction if this method call initializes an object.
		NewInstruction newInstruction = null; 
		if (!isConstructorChain && name.equals("<init>")) {
			newInstruction = newInstructionStack.pop();
			assert newInstruction.getTypeName().equals(owner);
		}

		// Generate instructions to record method call and its parameters
		if (weavingInfo.recordMethodCall() && !minimumLogging()) {
		
			long locationId = generateRecordCall(opcode, owner, name, desc, newInstruction);
			
			if (weavingInfo.recordParameters()) {
				// Generate code to record parameters
				MethodParameters params = new MethodParameters(desc);
				
				// Store parameters into additional local variables.   
				for (int i=params.size()-1; i>=0; i--) {
					int local = super.newLocal(params.getType(i)); 
					params.setLocalVar(i, local);
					generateNewVarInsn(params.getStoreInstruction(i), local);
				}
				// Here, all parameters (except for a receiver) are stored in local variables.
				
				// Duplicate an object reference to record the created object
				int offset = 0;
				if (isConstructorChain || newInstruction != null) {
					// For constructor, duplicate the object reference.  Record it later (after the constructor call).
					offset = 1; 
					super.visitInsn(Opcodes.DUP);
				} else if (opcode != Opcodes.INVOKESTATIC) {
					// For a regular non-static method, duplicate and record the object reference. 
					offset = 1;
					super.visitInsn(Opcodes.DUP);
		            super.visitLdcInsn(0); 
		            super.visitLdcInsn(locationId);
					super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordActualParam", "(Ljava/lang/Object;IJ)V", false);
				}
				
				// Load each parameter and record its value
				for (int i=0; i<params.size(); i++) {
					generateNewVarInsn(params.getLoadInstruction(i), params.getLocalVar(i));
		            super.visitLdcInsn(i+offset);
		            super.visitLdcInsn(locationId); 
					super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordActualParam", "(" + params.getRecordDesc(i) + "IJ)V", false);
				}
		
				// Restore parameters from local variables
				for (int i=0; i<params.size(); i++) {
					generateNewVarInsn(params.getLoadInstruction(i), params.getLocalVar(i));
				}
			}

			// Call the original method
			super.visitMethodInsn(opcode, owner, name, desc, itf);

			if (weavingInfo.recordParameters()) {
				// Record a return value or an initialized object.
				if (newInstruction != null) { 
					// Record an object created by "new X()"
					super.visitLdcInsn(locationId);
					super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordObjectCreated", "(Ljava/lang/Object;J)V", false);
				} else if (isConstructorChain) { 
					// Record an object initialized by this() or super()
		            super.visitLdcInsn(locationId); 
					super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordObjectInitialized", "(Ljava/lang/Object;J)V", false);

				} else {
					// record return value
					generateRecordReturnValue(locationId, desc);
				}
			} else {
				generateRecordReturnValue(locationId, "()V");
			}
			
		} else {  // A regular method call if (weavingInfo.recordMethodCall() && !minimumLogging()) {
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}
		
		// Start a try block to record an exception thrown by the remaining code.  
		// Because Java Verifier does not allow "try { super(); } catch ...  ", this code generate "super(); try { ... }". 
		if (weavingInfo.recordExecution() && isConstructorChain) {
			super.visitLabel(startLabel);
			isStartLabelLocated = true;
		}

		instructionIndex++;
	}
	
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if (weavingInfo.recordArrayInstructions() && !minimumLogging()) {
			long locationId = nextLocationId("MULTINEWARRAY " + desc + " " + Integer.toString(dims));
			super.visitMultiANewArrayInsn(desc, dims);
			super.visitInsn(Opcodes.DUP);
			super.visitLdcInsn(locationId);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordMultiNewArray", "(Ljava/lang/Object;J)V", false);
		} else {
			super.visitMultiANewArrayInsn(desc, dims);
		}
		afterNewArray = true;
		instructionIndex++;
	}
	
	
	private static final String RECORD_RETURN_VALUE = "recordReturnValueAfterCall";
	
	private void generateRecordReturnValue(long locationId, String desc) {
		int index = desc.indexOf(')');
		String returnTypeName = desc.substring(index+1); 
		if (returnTypeName.length() > 1) {
			assert returnTypeName.startsWith("L") || returnTypeName.startsWith("["); 
			super.visitInsn(Opcodes.DUP);
			super.visitLdcInsn(locationId);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, RECORD_RETURN_VALUE, "(Ljava/lang/Object;J)V", false);
		} else {
			char type = desc.charAt(desc.length()-1);
			switch (type) {
			case 'V':
				super.visitLdcInsn(locationId);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, RECORD_RETURN_VALUE, "(J)V", false);
				break;
			case 'C':
			case 'B':
			case 'F':
			case 'I':
			case 'S':
			case 'Z':
				super.visitInsn(Opcodes.DUP);
				super.visitLdcInsn(locationId);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, RECORD_RETURN_VALUE, "(" + type + "J)V", false);
				break;
			case 'J':
			case 'D':
				super.visitInsn(Opcodes.DUP2);
				super.visitLdcInsn(locationId);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, RECORD_RETURN_VALUE, "(" + type + "J)V", false);
				break;
			default:
				assert false: "Unknown premitive type";
			}
		}
	}
	
	@Override
	public void visitIincInsn(int var, int increment) {
		super.visitIincInsn(var, increment);
		instructionIndex++;
	}
	
	@Override
	public void visitInsn(int opcode) {
		
		if (isReturn(opcode)) {
			if (weavingInfo.recordExecution()) {
				generateRecordReturn(opcode); 
			}
			super.visitInsn(opcode);
		} else if (opcode == Opcodes.ATHROW) {
			if (weavingInfo.recordExecution()) {
				super.visitInsn(Opcodes.DUP);
				long locationId = nextLocationId("THROW");
				super.visitLdcInsn(locationId);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordThrowStatement", "(Ljava/lang/Object;J)V", false);
			}
			super.visitInsn(opcode);
		} else if (!minimumLogging()) {
			if (isArrayLoad(opcode)) {
				if (weavingInfo.recordArrayInstructions()) {
					generateRecordArrayLoad(opcode);
				} else {
					super.visitInsn(opcode);
				}
			} else if (isArrayStore(opcode)) {
				if (weavingInfo.recordArrayInstructions() && !(ignoreArrayInit() && afterNewArray)) {
					generateRecordArrayStore(opcode);
				} else {
					super.visitInsn(opcode);
				}
			} else if (opcode == Opcodes.ARRAYLENGTH) {
				if (weavingInfo.recordArrayInstructions()) {
					super.visitInsn(Opcodes.DUP); // -> [arrayref, arrayref]
					long locationId = nextLocationId("ARRAYLENGTH");
					super.visitLdcInsn(locationId); // -> [arrayref, arrayref, locationId]
					super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordArrayLength", "(Ljava/lang/Object;J)V", false);
					super.visitInsn(opcode);  // -> [ arraylength]
				} else {
					super.visitInsn(opcode);
				}
			} else if (opcode == Opcodes.MONITORENTER) {
				if (weavingInfo.recordMiscInstructions()) {
					super.visitInsn(Opcodes.DUP); // -> [objectref, objectref]
					super.visitInsn(opcode); // Enter the monitor
					long locationId = nextLocationId("MONITORENTER");
					super.visitLdcInsn(locationId);  // -> [objectref, locationId]
					super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordMonitorEnter", "(Ljava/lang/Object;J)V", false);
				} else {
					super.visitInsn(opcode);
				}
			} else if (opcode == Opcodes.MONITOREXIT) {
				if (weavingInfo.recordMiscInstructions()) {
					long locationId = nextLocationId("MONITOREXIT");
					super.visitInsn(Opcodes.DUP); // -> [objectref, objectref]
					super.visitLdcInsn(locationId);  // -> [objectref, locationId]
					super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordMonitorExit", "(Ljava/lang/Object;J)V", false);
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

	private boolean isArrayStore(int opcode) {
		switch (opcode) {
		case Opcodes.AASTORE:
		case Opcodes.BASTORE:
		case Opcodes.CASTORE:
		case Opcodes.DASTORE:
		case Opcodes.FASTORE:
		case Opcodes.IASTORE:
		case Opcodes.LASTORE:
		case Opcodes.SASTORE:
			return true;
		default:
			return false;
		}
	}
	
	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
			Object... bsmArgs) {
		if (weavingInfo.recordMethodCall() && !minimumLogging()) {
			long locationId = nextLocationId("DYNAMIC-CALL " + name + "#" + desc);
			
			MethodParameters params = new MethodParameters(desc);
			// Store parameters to additional local variables.
			for (int i=params.size()-1; i>=0; i--) {
				int local = super.newLocal(params.getType(i));
				params.setLocalVar(i, local);
				generateNewVarInsn(params.getStoreInstruction(i), local);
			}
			// Duplicate an object reference to record the created object
			super.visitInsn(Opcodes.DUP);
			super.visitLdcInsn(0); 
			super.visitLdcInsn(locationId);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordActualParam", "(Ljava/lang/Object;IJ)V", false);
			
			// Load each parameter and record its value
			for (int i=0; i<params.size(); i++) {
				generateNewVarInsn(params.getLoadInstruction(i), params.getLocalVar(i));
	            super.visitLdcInsn(i+1);
	            super.visitLdcInsn(locationId); 
				super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordActualParam", "(" + params.getRecordDesc(i) + "IJ)V", false);
			}
			// Load parameters and invoke the original call 
			for (int i=0; i<params.size(); i++) {
				generateNewVarInsn(params.getLoadInstruction(i), params.getLocalVar(i));
			}
			super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
			
			// record return value
			generateRecordReturnValue(locationId, desc);
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
	public void visitTableSwitchInsn(int min, int max, Label dflt,
			Label... labels) {
		super.visitTableSwitchInsn(min, max, dflt, labels);
		instructionIndex++;
	}

	private void generateRecordArrayStore(int opcode) {
		int storeInstruction;
		int loadInstruction;
		int dup_x2 = Opcodes.DUP_X2;
		Type t;
		String desc;
		switch (opcode) {
		case Opcodes.BASTORE:
			t = Type.BYTE_TYPE;
			storeInstruction = Opcodes.ISTORE;
			loadInstruction = Opcodes.ILOAD;
			desc = "B";
			break;
		case Opcodes.CASTORE:
			t = Type.CHAR_TYPE;
			storeInstruction = Opcodes.ISTORE;
			loadInstruction = Opcodes.ILOAD;
			desc = "C";
			break;
		case Opcodes.DASTORE:
			t = Type.DOUBLE_TYPE;
			storeInstruction = Opcodes.DSTORE;
			loadInstruction = Opcodes.DLOAD;
			desc = "D";
			dup_x2 = Opcodes.DUP2_X2;
			break;
		case Opcodes.FASTORE:
			t = Type.FLOAT_TYPE;
			storeInstruction = Opcodes.FSTORE;
			loadInstruction = Opcodes.FLOAD;
			desc = "F";
			break;
		case Opcodes.IASTORE:
			t = Type.INT_TYPE;
			storeInstruction = Opcodes.ISTORE;
			loadInstruction = Opcodes.ILOAD;
			desc = "I";
			break;
		case Opcodes.LASTORE:
			t = Type.LONG_TYPE;
			storeInstruction = Opcodes.LSTORE;
			loadInstruction = Opcodes.LLOAD;
			dup_x2 = Opcodes.DUP2_X2;
			desc = "J";
			break;
		case Opcodes.SASTORE:
			t = Type.SHORT_TYPE;
			storeInstruction = Opcodes.ISTORE;
			loadInstruction = Opcodes.ILOAD;
			desc = "S";
			break;
		default:
			assert opcode == Opcodes.AASTORE;
			t = Type.getObjectType("java/lang/Object");
			storeInstruction = Opcodes.ASTORE;
			loadInstruction = Opcodes.ALOAD;
			desc = "Ljava/lang/Object;";
		}
        int valueStore = super.newLocal(t);
		// Stack: [ array, index, value ]
        //super.visitInsn(dup_x2); // -> stack [value, array, index, value] Copy the value to keep the type information of the value. 
        generateNewVarInsn(storeInstruction, valueStore); // -> Local: [value], Stack: [array, index]. 
		super.visitInsn(Opcodes.DUP2);     // -> Local: [value], Stack: [array, index, array, index]
		generateNewVarInsn(loadInstruction, valueStore); // -> [array, index, array, index, value]
		super.visitInsn(dup_x2);     // -> Local: [value], Stack: [array, index, value, array, index, value]
		super.visitLdcInsn(nextLocationId("ARRAY STORE" + opcode)); // -> [array, index, value, array, index, value, location]
        super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordArrayStore", "(Ljava/lang/Object;I" + desc + "J)V", false);
        super.visitInsn(opcode); 
	}
	
	@Override
	public void visitLdcInsn(Object cst) {
		super.visitLdcInsn(cst); // -> [object]
		if (weavingInfo.recordMiscInstructions() &&
			!(cst instanceof Integer) &&
			!(cst instanceof Long) &&
			!(cst instanceof Double) &&
			!(cst instanceof Float)) {
			long locationId = nextLocationId("LDC " + cst.getClass().getName());
			super.visitInsn(Opcodes.DUP); 
			super.visitLdcInsn(locationId); // -> [object, object, locationId]
	        super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordConstantLoad", "(Ljava/lang/Object;J)V", false);  // -> [object]
		}
		instructionIndex++;
	}
	
	private boolean isArrayLoad(int opcode) {
		switch (opcode) {
		case Opcodes.AALOAD:
		case Opcodes.BALOAD:
		case Opcodes.CALOAD:
		case Opcodes.DALOAD:
		case Opcodes.FALOAD:
		case Opcodes.IALOAD:
		case Opcodes.LALOAD:
		case Opcodes.SALOAD:
			return true;
		default:
			return false;
		}
	}
	
	private void generateRecordArrayLoad(int opcode) {
		long locationId = nextLocationId("ARRAY LOAD " + opcode);
        super.visitInsn(Opcodes.DUP2); // stack: [array, index, array, index]
        super.visitLdcInsn(locationId); // [array, index, array, index, location]

        String desc;
        if (opcode == Opcodes.BALOAD) desc = "(Ljava/lang/Object;IJ)V"; // Use Object to represent byte[] and boolean[]
        else if (opcode == Opcodes.CALOAD) desc = "([CIJ)V";
        else if (opcode == Opcodes.DALOAD) desc = "([DIJ)V";
        else if (opcode == Opcodes.FALOAD) desc = "([FIJ)V";
        else if (opcode == Opcodes.IALOAD) desc = "([IIJ)V";
        else if (opcode == Opcodes.LALOAD) desc = "([JIJ)V";
        else if (opcode == Opcodes.SALOAD) desc = "([SIJ)V";
        else {
        	assert (opcode == Opcodes.AALOAD);
        	desc = "([Ljava/lang/Object;IJ)V";
        }
        	
        super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordArrayLoad", desc, false); 

        // the original instruction [array, index] -> [value]
	    super.visitInsn(opcode); 
	}

	private void generateRecordReturn(int opcode) {
		long locationId = nextLocationId("EXIT " + className + "#" + methodName + "#" + methodDesc);
		switch (opcode) {
		case Opcodes.ARETURN:
		case Opcodes.IRETURN:
		case Opcodes.FRETURN:
		case Opcodes.DRETURN:
		case Opcodes.LRETURN:
			int pos = methodDesc.indexOf(')');
			assert pos>=0: "No return value info in a descriptor: " + className + "#" + methodName + "#" + methodDesc;
			String desc = methodDesc.substring(pos+1);
			generateDup(desc);
			super.visitLdcInsn(locationId);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordNormalExit", "(" + normalizeDesc(desc) + "J)V", false);
			break;
		case Opcodes.RETURN:
			super.visitLdcInsn(locationId);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordNormalExit", "(J)V", false);
			break;
		default:
			assert false;
		}
	}
	
	private boolean isReturn(int opcode) {
		return (opcode == Opcodes.ARETURN) || (opcode == Opcodes.RETURN) ||
				(opcode == Opcodes.IRETURN) || (opcode == Opcodes.FRETURN) ||
				(opcode == Opcodes.LRETURN) || (opcode == Opcodes.DRETURN);
	}
	
	/**
	 * Translate a descriptor for a class to a descriptor for Object.
	 * If a descriptor is for a primitive, return the descriptor itself.   
	 */
	private String normalizeDesc(String desc) {
		if (desc.length() == 1) {
			return desc;
		} else {
			return "Ljava/lang/Object;"; 
		}
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name,
			String desc) {
		if (minimumLogging() || !weavingInfo.recordFieldAccess()) {
			super.visitFieldInsn(opcode, owner, name, desc);
			instructionIndex++;
			return;
		}
		
		String label;
		if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.GETFIELD) {
			label  = "READ " + owner + "#" + name + "#" + desc;
		} else {
			label  = "WRITE " + owner + "#" + name + "#" + desc;
		}
		long locationId = nextLocationId(label);
		if (opcode == Opcodes.GETSTATIC) { 
			// Execute GETSTATIC
			super.visitFieldInsn(opcode, owner, name, desc); // [] -> [ value ]
			// Record the result 
			generateDup(desc);
			super.visitLdcInsn(locationId); // -> [value, value, locationId]
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordGetStaticField", "(" + normalizeDesc(desc) + "J)V", false);
		} else if (opcode == Opcodes.GETFIELD) {
			// Call recordGetFieldTarget to record a field reference event using a null reference 
			super.visitInsn(Opcodes.DUP);
			super.visitLdcInsn(locationId); // -> [obj, obj, locationId]
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordGetInstanceFieldTarget", "(Ljava/lang/Object;J)V", false);
			// Duplicate 
			super.visitInsn(Opcodes.DUP); // -> [obj, obj]
			// Execute GETFIELD
			super.visitFieldInsn(opcode, owner, name, desc); // -> [obj, value]
			// Record the result
			generateDupX1(desc);  // -> [value, obj, value]
			super.visitLdcInsn(locationId); // -> [value, obj, value, locationId]
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordGetInstanceField", "(Ljava/lang/Object;" + normalizeDesc(desc) + "J)V", false);
		} else if (opcode == Opcodes.PUTSTATIC) {
			// stack: [value]
			generateDup(desc);
			super.visitLdcInsn(locationId); // -> [value, value, locationId] 
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordPutStatic", "(" + normalizeDesc(desc) + "J)V", false);
			super.visitFieldInsn(opcode, owner, name, desc);
		} else {
			assert opcode == Opcodes.PUTFIELD;
			if (afterInitialization) {
				// stack: [object, value]
				if (desc.equals("D")) {
					int local = super.newLocal(Type.DOUBLE_TYPE);
					generateNewVarInsn(Opcodes.DSTORE, local); // -> [object]
					super.visitInsn(Opcodes.DUP);
					generateNewVarInsn(Opcodes.DLOAD, local); // -> [object, object, value]
					super.visitLdcInsn(locationId);
					super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordPutField", "(Ljava/lang/Object;" + desc + "J)V", false);
					generateNewVarInsn(Opcodes.DLOAD, local);
					super.visitFieldInsn(opcode, owner, name, desc);
				} else if (desc.equals("J")){
					int local = super.newLocal(Type.LONG_TYPE);
					generateNewVarInsn(Opcodes.LSTORE, local);
					super.visitInsn(Opcodes.DUP);
					generateNewVarInsn(Opcodes.LLOAD, local);
					super.visitLdcInsn(locationId);
					super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordPutField", "(Ljava/lang/Object;" + desc + "J)V", false);
					generateNewVarInsn(Opcodes.LLOAD, local);
					super.visitFieldInsn(opcode, owner, name, desc);
				} else {
					super.visitInsn(Opcodes.DUP2); // -> [object, value, object, value]
					super.visitLdcInsn(locationId);
					if (desc.length() == 1) {
						super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordPutField", "(Ljava/lang/Object;" + desc + "J)V", false);
					} else {
						super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordPutField", "(Ljava/lang/Object;Ljava/lang/Object;J)V", false);
					}
					super.visitFieldInsn(opcode, owner, name, desc);
				}
			} else {
				// Before the target object is initialized, we cannot record the object. 
				generateDup(desc); // -> [object, value, value]
				super.visitLdcInsn(locationId);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, "recordPutFieldBeforeInit", "(" + normalizeDesc(desc) + "J)V", false);
				super.visitFieldInsn(opcode, owner, name, desc);
			}
		}
		instructionIndex++;
	}
	
	private long nextLocationId(String label) {
		return nextLocationId(label, -1);
	}

	private long nextLocationId(String label, long relevantLocationId) {
		assert !label.contains(WeavingInfo.SEPARATOR): "Location ID cannot includes WeavingInfo.SEPARATOR(" + WeavingInfo.SEPARATOR + ").";
		return weavingInfo.nextLocationId(currentLine, instructionIndex, relevantLocationId, label);
	}

	private void generateDup(String desc) {
		if (desc.equals("D") || desc.equals("J")) {
			super.visitInsn(Opcodes.DUP2);
		} else {
			super.visitInsn(Opcodes.DUP);
		}
	}

	private void generateDupX1(String desc) {
		if (desc.equals("D") || desc.equals("J")) {
			super.visitInsn(Opcodes.DUP2_X1);
		} else {
			super.visitInsn(Opcodes.DUP_X1);
		}
	}

	private String getArrayElementType(int type) {
		switch (type) {
		case Opcodes.T_BOOLEAN: 
			return "boolean";
		case Opcodes.T_CHAR:
			return "char";
		case Opcodes.T_FLOAT:
			return "float";
		case Opcodes.T_DOUBLE:
			return "double";
		case Opcodes.T_BYTE:
			return "byet";
		case Opcodes.T_SHORT:
			return "short";
		case Opcodes.T_INT:
			return "int";
		case Opcodes.T_LONG:
			return "long";
		default:
			assert false: "Unknown Array Type";
			return "Unknown";
		}
	}
	
	private class NewInstruction {
		
		private long locationId;
		private String typeName;

		public NewInstruction(long locationId, String typeName) {
			this.locationId = locationId;
			this.typeName = typeName;
		}
		
		public long getLocationId() {
			return locationId;
		}
		
		public String getTypeName() {
			return typeName;
		}
	}

}
