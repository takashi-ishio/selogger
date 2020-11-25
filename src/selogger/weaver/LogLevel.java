package selogger.weaver;

/**
 * This object is to control the level of weaving 
 * when the class file is too large to include additional logging instructions.
 */
public enum LogLevel {
	
	/**
	 * Insert all logging instructions
	 */
	Normal, 
	/**
	 * Ignore instructions that assign initial values to an array.
	 * For example, an array creation statement "int[] a = new int[]{1, 2, 3};" 
	 * is compiled to three instructions a[0]=1, a[1]=2, a[2]=3.
	 * SELogger inserts logging code for the three ARRAY_STORE instructions.
	 * This kind of code may be unacceptable if the method initializes a large array object.  
	 */
	IgnoreArrayInitializer, 
	/**
	 * This level inserts only ENTRY and EXIT events.
	 * This level may be necessary if the method itself is 
	 * too large to include additional instructions.
	 */
	OnlyEntryExit, 
	/**
	 * This does not inject any instructions.
	 */
	Failed;
	
	/**
	 * A string representation of the log level.
	 */
	@Override
	public String toString() {
		switch (this) {
		case Normal:
			return "Normal";
		case IgnoreArrayInitializer:
			return "IgnoreArrayInitializer";
		case OnlyEntryExit:
			return "OnlyEntryExit";
		case Failed:
			return "Failed";
		default:
			return "";
		}
	}
}
