package selogger.reader;

import org.objectweb.asm.Opcodes;

public class MethodInfo {

	public static final int CLASSID_NOT_AVAILABLE = -1;
	
	private ClassInfo classInfo;
	private int classId;
	private int methodId;
	private String className;
	private String methodName;
	private String methodDesc;
	private int access;
	private String sourceFileName;

	public MethodInfo(ClassInfo classInfo, String className, int methodId, String methodName, String methodDesc, int access, String sourceFileName) {
		this(classInfo.getId(), className, methodId, methodName, methodDesc, access, sourceFileName);
		this.classInfo = classInfo;
	}
	
	public MethodInfo(int classId, String className, int methodId, String methodName, String methodDesc, int access, String sourceFileName) {
		this.classId = classId;
		this.className = className;
		this.methodId = methodId;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		this.access = access;
		this.sourceFileName = sourceFileName;
	}

	public MethodInfo(String className, int methodId, String methodName, String methodDesc, int access, String sourceFileName) {
		this(CLASSID_NOT_AVAILABLE, className, methodId, methodName, methodDesc, access, sourceFileName);
	}
	
	/**
	 * @return ClassInfo object if available.
	 */
	public ClassInfo getClassInfo() {
		return classInfo;
	}
	
	/**
	 * @return class ID if available.  If unavailable, CLASSID_NOT_AVAILABLE is returned. 
	 */
	public int getClassId() {
		return classId;
	}

	public int getMethodId() {
		return methodId;
	}

	public String getClassName() {
		return className;
	}
	
	public String getMethodName() {
		return methodName;
	}
	
	public String getMethodDesc() {
		return methodDesc;
	}
	
	public int getAccess() {
		return access;
	}
	
	public String getSourceFileName() {
		return sourceFileName;
	}
	
	public boolean isStatic() {
		return (access & Opcodes.ACC_STATIC) != 0;		
	}
	
	@Override
	public int hashCode() {
		return classId ^ className.hashCode() ^ methodName.hashCode() ^ methodDesc.hashCode() ^ access;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MethodInfo) {
			MethodInfo another = (MethodInfo)obj;
			return classId == another.classId && 
						className.equals(another.className) &&
						methodName.equals(another.methodName) &&
						methodDesc.equals(another.methodDesc) &&
						access == another.access;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return classId + "#" + className + "#" + methodName + "#" + methodDesc;
	}
	
}
