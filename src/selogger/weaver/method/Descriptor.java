package selogger.weaver.method;

/**
 * This enum represents a data type in Java bytecode. 
 */
public enum Descriptor {

	Boolean("Z"), Byte("B"), Char("C"), Short("S"), 
	Integer("I"), Long("J"), Float("F"), Double("D"), 
	Object("Ljava/lang/Object;"), Void("V");
	
	private String desc;

	/**
	 * Translate a descriptor into a Descriptor object.
	 * @param desc is a descriptor
	 * @return a Descriptor object corresponding to desc.
	 * Any descriptor representing a class is translated into Descriptor.Object. 
	 */
	public static Descriptor get(String desc) {
		switch (desc) {
		case "Z":
			return Boolean;
		case "B":
			return Byte;
		case "C":
			return Char;
		case "S":
			return Short;
		case "I":
			return Integer;
		case "J":
			return Long;
		case "F":
			return Float;
		case "D":
			return Double;
		case "V":
			return Void;
		default:
			return Object;
		}
	}
	
	/**
	 * A constructor to record the string representation.
	 * @param desc
	 */
	private Descriptor(String desc) {
		this.desc = desc;
	}
	
	/**
	 * @return the descriptor string.
	 */
	public String getString() {
		return desc;
	}
	
}
