package selogger.weaver.method;


public enum Descriptor {

	Boolean("Z"), Byte("B"), Char("C"), Short("S"), 
	Integer("I"), Long("J"), Float("F"), Double("D"), 
	Object("Ljava/lang/Object;"), Void("V");
	
	private String desc;
	
	public static Descriptor get(String s) {
		switch (s) {
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
	
	private Descriptor(String s) {
		this.desc = s;
	}
	
	public String getString() {
		return desc;
	}
	
}
