package selogger.weaver;

import java.util.ArrayList;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

public class MethodParameters extends SignatureVisitor {

	private ArrayList<Param> parameters;
	
	public MethodParameters(String desc) {
		super(Opcodes.ASM5);
		parameters = new ArrayList<Param>();
		SignatureReader reader = new SignatureReader(desc);
		reader.accept(this);
	}

	private boolean nextParam = false;
	private boolean nextArray = false;
	private String arrayType = null;
	
	@Override
	public SignatureVisitor visitParameterType() {
		assert !nextParam: "A parameter is not processed!"; 
		nextParam = true;
		return super.visitParameterType();
	}
	
	@Override
	public SignatureVisitor visitReturnType() {
		assert !nextParam && !nextArray: "A parameter is not processed!"; 
		return super.visitReturnType();
	}
	
	@Override
	public void visitBaseType(char descriptor) {
		if (nextArray) {
			parameters.add(new Param(arrayType + Character.toString(descriptor)));
			arrayType = null;
			nextArray = false;
		} else if (nextParam) {
			parameters.add(new Param(descriptor));
			nextParam = false;
		}
		super.visitBaseType(descriptor);
	}
	
	@Override
	public SignatureVisitor visitArrayType() {
		if (nextParam) {
			if (arrayType == null) arrayType = "[";
			else arrayType = arrayType + "[";
			nextArray = true;
			nextParam = false;
		}
		return super.visitArrayType();
	}
	
	@Override
	public void visitClassType(String name) {
		if (nextArray) {
			arrayType = arrayType + "L" + name + ";";
			parameters.add(new Param(arrayType));
			arrayType = null;
			nextArray = false;
		} else if (nextParam) {
			parameters.add(new Param("L" + name + ";"));
			nextParam = false;
		}
		super.visitClassType(name);
	}
	
	
	public void setLocalVar(int index, int varIndex) {
		parameters.get(index).localVar = varIndex;
	}
	
	public int getLocalVar(int index) {
		int v = parameters.get(index).localVar;
		assert v != -1: "Uninitialized Local variable"; 
		return v;
	}
	
	public int getWords(int index) {
		return parameters.get(index).size;
	}
	
	public int getLoadInstruction(int index) {
		return parameters.get(index).loadInstruction;
	}
	
	public int getStoreInstruction(int index) {
		return parameters.get(index).storeInstruction;
	}
	
	public Type getType(int index) {
		return parameters.get(index).t;
	}
	
	public String getRecordDesc(int index) {
		String desc = parameters.get(index).t.getDescriptor();
		if (desc.length() == 1) return desc;
		else return "Ljava/lang/Object;";
	}
	
	public int size() {
		return parameters.size();
	}
	
	private class Param {
		int loadInstruction = -1;
		int storeInstruction = -1;
		int localVar = -1;
		int size = 1;
		Type t;
		
		public Param(char descriptor) {
			switch (descriptor) {
			case 'B':
				t = Type.BYTE_TYPE;
				loadInstruction = Opcodes.ILOAD;
				storeInstruction = Opcodes.ISTORE;
				break;
				
			case 'C':
				t = Type.CHAR_TYPE;
				loadInstruction = Opcodes.ILOAD;
				storeInstruction = Opcodes.ISTORE;
				break;
				
			case 'I':
				t = Type.INT_TYPE;
				loadInstruction = Opcodes.ILOAD;
				storeInstruction = Opcodes.ISTORE;
				break;

			case 'S':
				t = Type.SHORT_TYPE;
				loadInstruction = Opcodes.ILOAD;
				storeInstruction = Opcodes.ISTORE;
				break;
				
			case 'Z':
				t = Type.BOOLEAN_TYPE;
				loadInstruction = Opcodes.ILOAD;
				storeInstruction = Opcodes.ISTORE;
				break;

			case 'F':
				t = Type.FLOAT_TYPE;
				loadInstruction = Opcodes.FLOAD;
				storeInstruction = Opcodes.FSTORE;
				break;

			case 'J':
				t = Type.LONG_TYPE;
				loadInstruction = Opcodes.LLOAD;
				storeInstruction = Opcodes.LSTORE;
				size = 2;
				break;

			case 'D':
				t = Type.DOUBLE_TYPE;
				loadInstruction = Opcodes.DLOAD;
				storeInstruction = Opcodes.DSTORE;
				size = 2;
				break;
			
			default:
				assert false; // VOID must not be specified
			}
		}

		public Param(String typeDesc) {
			loadInstruction = Opcodes.ALOAD;
			storeInstruction = Opcodes.ASTORE;
			t = Type.getType(typeDesc);
			assert t.getDescriptor().equals(typeDesc);
		}
	}
}
