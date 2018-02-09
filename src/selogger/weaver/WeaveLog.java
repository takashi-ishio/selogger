package selogger.weaver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import selogger.EventType;
import selogger.weaver.method.Descriptor;

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

	
	public WeaveLog(int classId, int nextMethodId, int nextDataId) {
		this.classId = classId;
		this.methodId = nextMethodId;
		this.dataId = nextDataId;
		dataEntries = new ArrayList<>();
		methodEntries = new ArrayList<>();
		logger = new StringWriter();
		loggerWrapper = new PrintWriter(logger);
	}

	public void setFullClassName(String name) {
		this.fullClassName = name;
	}
	
	public String getFullClassName() {
		return fullClassName;
	}

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
	
	public void startMethod(String className, String methodName, String methodDesc, int access, String sourceFileName) {
		MethodInfo entry = new MethodInfo(classId, methodId, className, methodName, methodDesc, access, sourceFileName);
		methodEntries.add(entry);
		methodId++;
	}
	
	/**
	 * Create a new Data ID and record the information.
	 * @param line
	 * @param instructionIndex
	 * @param eventType
	 * @param valueDesc
	 * @param attributes
	 * @return
	 */
	public int nextDataId(int line, int instructionIndex, EventType eventType, Descriptor valueDesc, String attributes) {
		DataInfo entry = new DataInfo(classId, methodId-1, dataId, line, instructionIndex, eventType, valueDesc, attributes);
		dataEntries.add(entry);
		return dataId++;
	}
	
	public void log(String msg) {
		loggerWrapper.println(msg);
	}

//	public void log(Throwable e) {
//		e.printStackTrace(loggerWrapper);
//	}
	
	public String getLog() {
		loggerWrapper.close();
		return logger.toString();
	}
	
	public ArrayList<DataInfo> getDataEntries() {
		return dataEntries;
	}
	
	public ArrayList<MethodInfo> getMethods() {
		return methodEntries;
	}
	

}
