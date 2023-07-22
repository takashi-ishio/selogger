package selogger.weaver;

import java.util.Scanner;


/**
 * This object is to record the information of a method processed by a weaver.  
 */
public class MethodInfo {

	private static final String SEPARATOR = ",";

	private int classId;
	private int methodId;
	private String className;
	private String methodName;
	private String methodDesc;
	private int access;
	private String sourceFileName;
	private String methodHash;
	
	/**
	 * Create an instance recording the information.
	 * @param classId is a class ID assigned by the weaver.
	 * @param methodId is a method ID assigned by the weaver.
	 * @param className is the class name.
	 * @param methodName is the method name.
	 * @param methodDesc is the descriptor representing parameters and return value.
	 * @param access includes modifiers of the method.
	 * @param sourceFileName is a source file name recorded in the class.  This may be null.
	 * @param methodHash is a hash value for bytecode instructions.  If two versions of a class have the same instructions, they have the same hash. 
	 */
	public MethodInfo(int classId, int methodId, String className, String methodName, String methodDesc, int access, String sourceFileName, String methodHash) {
		this.classId = classId;
		this.methodId = methodId;
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		this.access = access;
		this.sourceFileName = sourceFileName;
		this.methodHash = methodHash;
	}
	
	/**
	 * @return the class ID of the method
	 */
	public int getClassId() {
		return classId;
	}
	
	/**
	 * @return the method ID of the method
	 */
	public int getMethodId() {
		return methodId;
	}

	/**
	 * @return the class name
	 */
	public String getClassName() {
		return className;
	}
	
	/**
	 * @return the name of the method
	 */
	public String getMethodName() {
		return methodName;
	}
	
	/**
	 * @return the descriptor of the method
	 */
	public String getMethodDesc() {
		return methodDesc;
	}
	
	/**
	 * @return the access flags 
	 */
	public int getAccess() {
		return access;
	}
	
	/**
	 * @return a source file name recorded by a compiler.  This may return null.
	 */
	public String getSourceFileName() {
		return sourceFileName;
	}
	
	/**
	 * @return a hash code for method instructions.  
	 * If two methods have the same instructions except for line numbers, they return a same hash code.  
	 */
	public String getMethodHash() {
		return methodHash;
	}
	
	/**
	 * @return a shortened hash code for method instructions.
	 */
	public String getShortMethodHash() {
		return methodHash.substring(0, 8);
	}
	
	/**
	 * @return column names for a CSV file.
	 */
	public static String getColumnNames() {
		StringBuilder buf = new StringBuilder();
		buf.append("ClassID");  
		buf.append(SEPARATOR);
		buf.append("MethodID");  
		buf.append(SEPARATOR);
		buf.append("ClassName");
		buf.append(SEPARATOR);
		buf.append("MethodName");
		buf.append(SEPARATOR);
		buf.append("MethodDesc");
		buf.append(SEPARATOR);
		buf.append("Access");
		buf.append(SEPARATOR);
		buf.append("SourceFileName");
		buf.append(SEPARATOR);
		buf.append("MethodHash");
		return buf.toString();
	}
	
	/**
	 * Create a string representation to be stored in a text file.
	 */
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(classId);  
		buf.append(SEPARATOR);
		buf.append(methodId);  
		buf.append(SEPARATOR);
		buf.append(className);
		buf.append(SEPARATOR);
		buf.append(methodName);
		buf.append(SEPARATOR);
		buf.append(methodDesc);
		buf.append(SEPARATOR);
		buf.append(access);
		buf.append(SEPARATOR);
		if (sourceFileName != null) buf.append(sourceFileName);
		buf.append(SEPARATOR);
		if (methodHash != null) buf.append(methodHash);
		return buf.toString();
	}
	
	/**
	 * Extract MethodInfo from a string
	 * @param s specifies the content created by MethodInfo.toString
	 * @return a MethodInfo instance.
	 */
	public static MethodInfo parse(String s) {
		Scanner sc = new Scanner(s);
		sc.useDelimiter(SEPARATOR);
		int classId = sc.nextInt();
		int methodId = sc.nextInt();
		String className = sc.next();
		String methodName = sc.next();
		String methodDesc = sc.next();
		int access = sc.nextInt();
		String sourceFileName = sc.hasNext() ? sc.next() : null;
		String methodHash = sc.hasNext() ? sc.next() : null;
		sc.close();
		return new MethodInfo(classId, methodId, className, methodName, methodDesc, access, sourceFileName, methodHash);
	}
	

}
