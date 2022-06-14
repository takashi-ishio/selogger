package selogger.weaver.method;

/**
 * A class to represent a NEW instruction.
 */
public class ANewInstruction {

	private int instructionIndex;
	private String typeName;

	/**
	 * Create an instance representing a NEW instruction
	 * @param instructionIndex specifies the event location of the NEW_OBJECT event
	 * @param typeName specifies the type name recorded by the instruction
	 */
	public ANewInstruction(int instructionIndex, String typeName) {
		this.instructionIndex = instructionIndex;
		this.typeName = typeName;
	}
	
	/**
	 * @return the index.  This is used to link the NEW instruction and its constructor call
	 */
	public int getInstructionIndex() {
		return instructionIndex;
	}
	
	/**
	 * @return the type name.  This is used just for checking the consistency of the bytecode.
	 */
	public String getTypeName() {
		return typeName;
	}

}
