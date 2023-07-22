package selogger.weaver.method;

import java.util.ArrayList;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import selogger.logging.util.TypeIdMap;

/**
 * An instance of this class parses a method parameter descriptor.
 * The object also records type and opcode information for loading/storing a parameter.
 */
public class MethodParameters {

	private ArrayList<Param> parameters;
	
	/**
	 * Create an instance 
	 * @param desc specifies a method descriptor, e.g. "(I)V".
	 */
	public MethodParameters(String desc) {
		parameters = new ArrayList<Param>();
		// Read parameter list and put the information to parameters.
		ParameterList params = new ParameterList();
		SignatureReader reader = new SignatureReader(desc);
		reader.accept(params);
	}
		
	/**
	 * Record a local variable index for saving a parameter on a method call.
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
	
	/**
	 * Internal class representing a single method parameter.
	 */
	private class Param {
		int loadInstruction = -1;
		int storeInstruction = -1;
		int localVar = -1;
		int size = 1;
		int typeId = 0;
		Type t;
		Descriptor desc;
		
		/**
		 * Craete an instance for a given type.
		 * @param descriptor specifies a paramter type.
		 */
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

		/**
		 * This constructor creates an instance for an object-type paramter.
		 * @param typeDesc specifies a type name.
		 */
		public Param(String typeDesc) {
			loadInstruction = Opcodes.ALOAD;
			storeInstruction = Opcodes.ASTORE;
			typeId = TypeIdMap.TYPEID_OBJECT;
			t = Type.getType(typeDesc);
			desc = Descriptor.Object;
			assert t.getDescriptor().equals(typeDesc);
		}
	}
	
	/**
	 * A visitor implementation for storing the parameter list.
	 */
	private class ParameterList extends SignatureVisitor {
		
		private boolean processingMethodParameter = false;
		private boolean processingArrayType = false;
		private String arrayType = null;
		
		/**
		 * Initialize the object.
		 */
		public ParameterList() {
			super(Opcodes.ASM5);
		}
		
		/**
		 * This method is called for each method parameter.
		 * In other words, the next method call represents the actual type.
		 */
		@Override
		public SignatureVisitor visitParameterType() {
			assert !processingMethodParameter: "A parameter is not processed!"; 
			processingMethodParameter = true;
			return super.visitParameterType();
		}
		
		/**
		 * This method is called for a return type.
		 * The next method call represents the actual type.
		 * Since the return type is uninteresting, 
		 * this method just checks the state of the object.
		 */
		@Override
		public SignatureVisitor visitReturnType() {
			assert !processingMethodParameter && !processingArrayType: "A parameter is not processed!"; 
			return super.visitReturnType();
		}
		
		/**
		 * This method is called for a basic type
		 */
		@Override
		public void visitBaseType(char descriptor) {
			if (processingArrayType) { 
				parameters.add(new Param(arrayType + Character.toString(descriptor)));
				arrayType = null;
				processingArrayType = false;
			} else if (processingMethodParameter) {  
				parameters.add(new Param(descriptor));
				processingMethodParameter = false;
			}
			super.visitBaseType(descriptor);
		}
		
		/**
		 * This method is called for an array type.
		 * An array type representing a method parameter is interesting.
		 */
		@Override
		public SignatureVisitor visitArrayType() {
			if (processingMethodParameter) {
				if (arrayType == null) arrayType = "[";
				else arrayType = arrayType + "[";
				processingArrayType = true;
				processingMethodParameter = false;
			}
			return super.visitArrayType();
		}
		
		/**
		 * This method is called for a class type.
		 */
		@Override
		public void visitClassType(String name) {
			if (processingArrayType) {
				arrayType = arrayType + "L" + name + ";";
				parameters.add(new Param(arrayType));
				arrayType = null;
				processingArrayType = false;
			} else if (processingMethodParameter) {
				parameters.add(new Param("L" + name + ";"));
				processingMethodParameter = false;
			}
			super.visitClassType(name);
		}
		
	}
	
}
