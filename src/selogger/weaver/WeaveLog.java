package selogger.weaver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import selogger.EventType;
import selogger.weaver.method.Descriptor;

/**
 * This object generates data IDs and records the weaving process 
 * for a single class file.
 * If a bytecode weaving is terminated by an error,
 * the generated data IDs are disabled by disposing this log object. 
 */
public class WeaveLog {
	
	private int classId;
	private int methodId;
	private int dataId;
	private ArrayList<DataInfo> dataEntries;
	private ArrayList<MethodInfo> methodEntries;
	private StringWriter logger;
	private PrintWriter loggerWrapper;
	
	private String fullClassName;
	
	public static final String SEPARATOR = ",";

	/**
	 * Create an instance having the given state. 
	 * @param classId is the ID used by the target class (if succeeded)
	 * @param nextMethodId is the first ID of the methods of the target class
	 * @param nextDataId is the first ID of the data IDs of the target class 
	 */
	public WeaveLog(int classId, int nextMethodId, int nextDataId) {
		this.classId = classId;
		this.methodId = nextMethodId;
		this.dataId = nextDataId;
		dataEntries = new ArrayList<>();
		methodEntries = new ArrayList<>();
		logger = new StringWriter();
		loggerWrapper = new PrintWriter(logger);
	}

	
	/**
	 * Record the full class name obtained from the target class file.
	 * @param name
	 */
	public void setFullClassName(String name) {
		this.fullClassName = name;
	}

	/**
	 * @return the full class name.
	 */
	public String getFullClassName() {
		return fullClassName;
	}

	/**
	 * @return the next method ID after this weaving.
	 */
	public int getNextMethodId() {
		return methodId;
	}
	
	/**
	 * @return the next data ID.
	 * This method is to transfer the current Data ID to the next weaving target class. 
	 */
	public int getNextDataId() { 
		return dataId;
	}
	
	/**
	 * Record a woven method name.
	 * @param className is a clas name.
	 * @param methodName is a method name.
	 * @param methodDesc is a descripor representing the signature.
	 * @param access represents modifiers, e.g. static.
	 * @param sourceFileName specifies a source file name recorded in the class file.
	 */
	public void startMethod(String className, String methodName, String methodDesc, int access, String sourceFileName, String methodHash) {
		MethodInfo entry = new MethodInfo(classId, methodId, className, methodName, methodDesc, access, sourceFileName, methodHash);
		methodEntries.add(entry);
		methodId++;
	}
	
	/**
	 * Create a new Data ID and record the information.
	 * @param line specifies the line number.
	 * @param instructionIndex specifies the location of the instruction in the ASM's InsnList object. 
	 * @param eventType is an event type (decided by the instruction type).
	 * @param valueDesc specifies a data type of the value observed by the event. 
	 * @param attributes specifies additional static information obtained from bytecode.
	 * @return
	 */
	public int nextDataId(int line, int instructionIndex, EventType eventType, Descriptor valueDesc, String attributes) {
		DataInfo entry = new DataInfo(classId, methodId-1, dataId, line, instructionIndex, eventType, valueDesc, attributes);
		dataEntries.add(entry);
		return dataId++;
	}
	
	/**
	 * Record an error message.
	 * @param msg
	 */
	public void log(String msg) {
		loggerWrapper.println(msg);
	}

	/**
	 * @return a string including the error messages recorded in this object.
	 */
	public String getLog() {
		loggerWrapper.close();
		return logger.toString();
	}
	
	/**
	 * @return data ID objects created during the weaving.
	 */
	public ArrayList<DataInfo> getDataEntries() {
		return dataEntries;
	}
	
	/**
	 * @return methods processed by this weaving.
	 */
	public ArrayList<MethodInfo> getMethods() {
		return methodEntries;
	}
	

}
