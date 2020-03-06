package selogger.weaver.method;

import java.util.ArrayList;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import selogger.logging.util.TypeIdMap;

/**
 * An instance of this class parses a method descriptor.
 * The object records type and opcode information for loading/storing a parameter
 */
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
	
		
	/**
	 * Record a local variable index for saving a parameter.
	 */
	public void setLocalVar(int index, int varIndex) {
		parameters.get(index).localVar = varIndex;
	}
	
	/**
	 * Return a local variable index for a parameter.
	 */
	public int getLocalVar(int index) {
		int v = parameters.get(index).localVar;
		assert v != -1: "Uninitialized Local variable"; 
		return v;
	}
	
	/**
	 * @return the number of words (1 or 2) for a parameter.
	 */
	public int getWords(int index) {
		return parameters.get(index).size;
	}
	
	public int getRecordWords(int index) {
		if (parameters.get(index).typeId == TypeIdMap.TYPEID_OBJECT) {
			return 2;
		} else {
			return parameters.get(index).size;
		}
	}
	
	/**
	 * @return the opcode to load a given parameter. 
	 */
	public int getLoadInstruction(int index) {
		return parameters.get(index).loadInstruction;
	}
	
	/**
	 * @return the opcode to store a given parameter. 
	 */
	public int getStoreInstruction(int index) {
		return parameters.get(index).storeInstruction;
	}
	
	/**
	 * @return the type of a parameter. 
	 */
	public Type getType(int index) {
		return parameters.get(index).t;
	}

	/**
	 * @return the type ID of a parameter. 
	 */
	public int getTypeId(int index) {
		return parameters.get(index).typeId;
	}

	/**
	 * @return the descriptor of a parameter type.
	 */
	public Descriptor getRecordDesc(int index) {
		return parameters.get(index).desc;
	}

	/**
	 * @return the number of parameters
	 */
	public int size() {
		return parameters.size();
	}
	
	private class Param {
		int loadInstruction = -1;
		int storeInstruction = -1;
		int localVar = -1;
		int size = 1;
		int typeId = 0;
		Type t;
		Descriptor desc;
		
		public Param(char descriptor) {
			switch (descriptor) {
			case 'B':
				t = Type.BYTE_TYPE;
				desc = Descriptor.Byte;
				typeId = TypeIdMap.TYPEID_BYTE;
				loadInstruction = Opcodes.ILOAD;
				storeInstruction = Opcodes.ISTORE;
				break;
				
			case 'C':
				t = Type.CHAR_TYPE;
				desc = Descriptor.Char;
				typeId = TypeIdMap.TYPEID_CHAR;
				loadInstruction = Opcodes.ILOAD;
				storeInstruction = Opcodes.ISTORE;
				break;
				
			case 'I':
				t = Type.INT_TYPE;
				desc = Descriptor.Integer;
				typeId = TypeIdMap.TYPEID_INT;
				loadInstruction = Opcodes.ILOAD;
				storeInstruction = Opcodes.ISTORE;
				break;

			case 'S':
				t = Type.SHORT_TYPE;
				desc = Descriptor.Short;
				typeId = TypeIdMap.TYPEID_SHORT;
				loadInstruction = Opcodes.ILOAD;
				storeInstruction = Opcodes.ISTORE;
				break;
				
			case 'Z':
				t = Type.BOOLEAN_TYPE;
				desc = Descriptor.Boolean;
				typeId = TypeIdMap.TYPEID_BOOLEAN;
				loadInstruction = Opcodes.ILOAD;
				storeInstruction = Opcodes.ISTORE;
				break;

			case 'F':
				t = Type.FLOAT_TYPE;
				desc = Descriptor.Float;
				typeId = TypeIdMap.TYPEID_FLOAT;
				loadInstruction = Opcodes.FLOAD;
				storeInstruction = Opcodes.FSTORE;
				break;

			case 'J':
				t = Type.LONG_TYPE;
				desc = Descriptor.Long;
				typeId = TypeIdMap.TYPEID_LONG;
				loadInstruction = Opcodes.LLOAD;
				storeInstruction = Opcodes.LSTORE;
				size = 2;
				break;

			case 'D':
				t = Type.DOUBLE_TYPE;
				desc = Descriptor.Double;
				typeId = TypeIdMap.TYPEID_DOUBLE;
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
			typeId = TypeIdMap.TYPEID_OBJECT;
			t = Type.getType(typeDesc);
			desc = Descriptor.Object;
			assert t.getDescriptor().equals(typeDesc);
		}
	}
}
