package selogger.weaver.method;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class OpcodesUtil {

	public static boolean isReturn(int opcode) {
		return (opcode == Opcodes.ARETURN) || (opcode == Opcodes.RETURN) ||
				(opcode == Opcodes.IRETURN) || (opcode == Opcodes.FRETURN) ||
				(opcode == Opcodes.LRETURN) || (opcode == Opcodes.DRETURN);
	}
	
	public static boolean isArrayLoad(int opcode) {
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

	public static boolean isArrayStore(int opcode) {
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
	
	public static int getDupInstruction(String desc) {
		if (desc.equals("D") || desc.equals("J")) {
			return Opcodes.DUP2;
		} else {
			return Opcodes.DUP;
		}
	}
	
	public static int getLoadInstruction(String desc) {
		switch (desc) {
		case "B":
		case "C":
		case "I":
		case "S":
		case "Z":
			return Opcodes.ILOAD;
			
		case "D":
			return Opcodes.DLOAD;

		case "F":
			return Opcodes.FLOAD;

		case "J":
			return Opcodes.LLOAD;

		case "Ljava/lang/Object;":
			return Opcodes.ALOAD;

		case "V":
			assert false: "Void is not a data";
			
		default:
			assert false: "Unknown primitive";
			return Opcodes.NOP;
		}
	}
	
	public static Type getAsmType(String desc) {
		switch (desc) {
		case "B":
			return Type.BYTE_TYPE;
			
		case "C":
			return Type.CHAR_TYPE;
			
		case "I":
			return Type.INT_TYPE;
			
		case "S":
			return Type.SHORT_TYPE;
			
		case "Z":
			return Type.BOOLEAN_TYPE;

		case "D":
			return Type.DOUBLE_TYPE;
			
		case "F":
			return Type.FLOAT_TYPE;

		case "J":
			return Type.LONG_TYPE;
			
		case "Ljava/lang/Object;":
			return Type.getObjectType("java/lang/Object");
			
		case "V":
			return Type.VOID_TYPE;
			
		default:
			assert false: "Unknown primitive";
			return null;
		}
		
	}

	/**
	 * Return a store instruction for 
	 * @param desc
	 * @return
	 */
	public static int getStoreInstruction(String desc) {
		switch (desc) {
		case "B":
		case "C":
		case "I":
		case "S":
		case "Z":
			return Opcodes.ISTORE;

		case "D":
			return Opcodes.DSTORE;
			
		case "F":
			return Opcodes.FSTORE;

		case "J":
			return Opcodes.LSTORE;
			
		case "Ljava/lang/Object;":
			return Opcodes.ASTORE;
			
		case "V":
			assert false: "Void is not a data";
			
		default:
			assert false: "Unknown primitive";
			return Opcodes.NOP;
		}
	}


	public static String getArrayElementType(int type) {
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
			return "byte";
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
	

	public static String getDescForArrayStore(int opcode) {
		switch (opcode) {
		case Opcodes.BASTORE: return "B";
		case Opcodes.CASTORE: return "C";
		case Opcodes.DASTORE: return "D";
		case Opcodes.FASTORE: return "F";
		case Opcodes.IASTORE: return "I";
		case Opcodes.LASTORE: return "J";
		case Opcodes.SASTORE: return "S";
		default:
			assert opcode == Opcodes.AASTORE;
			return "Ljava/lang/Object;";
		}
		
	}

	public static Descriptor getDescForArrayLoad(int opcode) {
		Descriptor elementDesc;
		if (opcode == Opcodes.BALOAD)
			elementDesc = Descriptor.Byte; // Use Object to represent byte[] and boolean[]
		else if (opcode == Opcodes.CALOAD)
			elementDesc = Descriptor.Char;
		else if (opcode == Opcodes.DALOAD)
			elementDesc = Descriptor.Double;
		else if (opcode == Opcodes.FALOAD)
			elementDesc = Descriptor.Float;
		else if (opcode == Opcodes.IALOAD)
			elementDesc = Descriptor.Integer;
		else if (opcode == Opcodes.LALOAD)
			elementDesc = Descriptor.Long;
		else if (opcode == Opcodes.SALOAD)
			elementDesc = Descriptor.Short;
		else {
			assert (opcode == Opcodes.AALOAD);
			elementDesc = Descriptor.Object;
		}
		return elementDesc;
	}
	
	public static Descriptor getDescForStore(int opcode) {
		switch (opcode) {
		case Opcodes.ISTORE: return Descriptor.Integer;
		case Opcodes.FSTORE: return Descriptor.Float;
		case Opcodes.DSTORE: return Descriptor.Double;
		case Opcodes.LSTORE: return Descriptor.Long;
		case Opcodes.ASTORE: return Descriptor.Object;
		default:
			return null;
		}
	}

	public static Descriptor getDescForLoad(int opcode) {
		switch (opcode) {
		case Opcodes.ILOAD: return Descriptor.Integer;
		case Opcodes.FLOAD: return Descriptor.Float;
		case Opcodes.DLOAD: return Descriptor.Double;
		case Opcodes.LLOAD: return Descriptor.Long;
		case Opcodes.ALOAD: return Descriptor.Object;
		default:
			return null;
		}
	}
	
	public static Descriptor getDescForReturn(int opcode) {
		switch (opcode) {
		case Opcodes.IRETURN: return Descriptor.Integer;
		case Opcodes.FRETURN: return Descriptor.Float;
		case Opcodes.DRETURN: return Descriptor.Double;
		case Opcodes.LRETURN: return Descriptor.Long;
		case Opcodes.ARETURN: return Descriptor.Object;
		case Opcodes.RETURN:  return Descriptor.Void;
		default:
			return null;
		}
	}
	
	public static String getCallInstructionName(int opcode) {
		switch (opcode) {
		case Opcodes.INVOKESPECIAL:
			return "INVOKESPECIAL";
		case Opcodes.INVOKEINTERFACE:
			return "INVOKEINTERFACE";
		case Opcodes.INVOKEDYNAMIC:
			return "INVOKEDYNAMIC";
		case Opcodes.INVOKESTATIC:
			return "INVOKESTATIC";
		default:
			return "INVOKEVIRTUAL";
		}
	}

}
