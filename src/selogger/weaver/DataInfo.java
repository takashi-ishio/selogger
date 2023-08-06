package selogger.weaver;

import java.util.Scanner;

import org.objectweb.asm.Type;

import selogger.EventType;
import selogger.weaver.method.Descriptor;
import selogger.weaver.method.InstructionAttributes;

/**
 * This object is to record attributes of a data ID.
 */
public class DataInfo {

	private static final String SEPARATOR = ",";
	private static final String ATTRIBUTE_KEYVALUE_SEPARATOR = "=";
	
	private int classId;
	private int methodId;
	private int dataId;
	private int line;
	private int instructionIndex;
	private EventType eventType;
	private Descriptor valueDesc;
	private InstructionAttributes attributes;
	
	/**
	 * MethodInfo object represented by MethodId.
	 * This reference is provided for convenience.
	 */
	private MethodInfo methodInfo;

	/**
	 * ClassInfo object represented by ClassId.
	 */
	private ClassInfo classInfo;

	/**
	 * Create an instance recording the data ID.
	 * @param classId is a class ID assigned by the weaver.
	 * @param methodId is a method ID assigned by the weaver.
	 * @param dataId is a data ID assigned by the weaver.
	 * @param line is the line number of the instruction (if available).
	 * @param instructionIndex is the location of the bytecode instruction in the ASM's InsnList.
	 * @param eventType is the event type.
	 * @param valueDesc is the value type observed by the event.
	 * @param attributes specifies additional attributes statically obtained from the instruction. 
	 */
	public DataInfo(int classId, int methodId, int dataId, int line, int instructionIndex, EventType eventType, Descriptor valueDesc, InstructionAttributes attributes) {
		this.classId = classId;
		this.methodId = methodId;
		this.dataId = dataId;
		this.line = line;
		this.instructionIndex = instructionIndex;
		this.eventType = eventType;
		this.valueDesc = valueDesc;
		this.attributes = attributes;
	}
	
	/**
	 * Link a MethodInfo object to the data info.
	 * @param methodInfo
	 */
	public void setMethodInfo(MethodInfo methodInfo) {
		if (methodInfo.getMethodId() != this.methodId) {
			throw new IllegalArgumentException("Inconsistent MethodInfo object is provided.");
		}
		this.methodInfo = methodInfo;
	}
	
	/**
	 * Link a ClassInfo object to the data info.
	 * This method is called after weaving because the ClassInfo includes the weaving result.
	 * @param classInfo
	 */
	public void setClassInfo(ClassInfo classInfo) {
		if (classInfo.getClassId() != this.classId) {
			throw new IllegalArgumentException("Inconsistent ClassInfo object is provided.");
		}
		this.classInfo = classInfo;
	}
	
	/**
	 * @return the container of the linked class
	 * This method returns null if this object is not linked to a class. 
	 */
	public String getFileContainer() {
		if (classInfo != null) {
			return classInfo.getContainer();
		} else {
			return null;
		}
	}
	
	/**
	 * @return a file name where a class is loaded
	 * This method returns null if this object is not linked to a class. 
	 */
	public String getFileName() {
		if (classInfo != null) {
			return classInfo.getFilename();
		} else {
			return null;
		}
	}
	
	/**
	 * @return a MethodInfo object if available.
	 */
	public MethodInfo getMethodInfo() {
		return methodInfo;
	}
	
	/**
	 * @return the method ID.
	 */
	public int getMethodId() {
		return methodId;
	}
	
	/**
	 * @return the data ID.
	 */
	public int getDataId() {
		return dataId;
	}
	
	/**
	 * @return the line number.
	 */
	public int getLine() {
		return line;
	}
	
	/**
	 * @return the location of the bytecode instruction in the ASM's InsnList.
	 */
	public int getInstructionIndex() {
		return instructionIndex;
	}
	
	/**
	 * @return the event type.
	 */
	public EventType getEventType() {
		return eventType;
	}
	
	/**
	 * @return the value type observed by the event.
	 */
	public Descriptor getValueDesc() {
		return valueDesc;
	}
	
	/**
	 * @return the value type observed by the event.
	 */
	public String getValueType() {
		return Type.getType(valueDesc.getString()).getClassName();
	}
	
	/**
	 * @return additional attributes statically obtained from the instruction.
	 */
	public InstructionAttributes getAttributes() {
		return attributes;
	}

	/**
	 * Access a particular attribute of the instruction, assuming the "KEY=VALUE" format.
	 * @param key specifies an attribute key
	 * @param defaultValue is returned if the key is unavailable.
	 * @return the value corresponding to the key.
	 */
	public String getAttribute(String key, String defaultValue) {
		return attributes.getStringValue(key, defaultValue);
	}

	/**
	 * @return column names for a CSV file.
	 */
	public static String getColumnNames() {
		StringBuilder buf = new StringBuilder();
		buf.append("DataID");
		buf.append(SEPARATOR);
		buf.append("ClassID");
		buf.append(SEPARATOR);
		buf.append("MethodID"); 
		buf.append(SEPARATOR);
		buf.append("ClassName");
		buf.append(SEPARATOR);
		buf.append("MethodName");
		buf.append(SEPARATOR);
		buf.append("Line");
		buf.append(SEPARATOR);
		buf.append("InstructionIndex");
		buf.append(SEPARATOR);
		buf.append("EventType");
		buf.append(SEPARATOR);
		buf.append("ValueDesc");
		buf.append(SEPARATOR);
		buf.append("Attributes");
		return buf.toString();
	}
	
	/**
	 * @return a string representation of the object. 
	 */
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(dataId);
		buf.append(SEPARATOR);
		buf.append(classId);
		buf.append(SEPARATOR);
		buf.append(methodId); 
		buf.append(SEPARATOR);
		buf.append(classInfo.getClassName());
		buf.append(SEPARATOR);
		buf.append(methodInfo.getMethodName()); 
		buf.append(SEPARATOR);
		buf.append(line);
		buf.append(SEPARATOR);
		buf.append(instructionIndex);
		buf.append(SEPARATOR);
		buf.append(eventType.name());
		buf.append(SEPARATOR);
		buf.append(valueDesc.getString());
		buf.append(SEPARATOR);
		buf.append("\"" + attributes + "\"");
		return buf.toString();
	}
	
	/**
	 * Create an instance from a string representation created by DataInfo.toString.
	 * @param s is the string representation
	 * @return a created instance
	 */
	public static DataInfo parse(String s) { 
		Scanner sc = new Scanner(s);
		sc.useDelimiter(SEPARATOR);
		int dataId = sc.nextInt();
		int classId = sc.nextInt();
		int methodId = sc.nextInt();
		String className = sc.next();
		String methodName = sc.next();
		int line = sc.nextInt();
		int instructionIndex = sc.nextInt();
		EventType t = EventType.valueOf(sc.next());
		Descriptor d = Descriptor.get(sc.next());
		InstructionAttributes attr = new InstructionAttributes();
		while (sc.hasNext()) { // for each key=value pair
			String keyvalue = sc.next();
			// Skip if it is a non-key-value entry
			if (keyvalue.indexOf(DataInfo.ATTRIBUTE_KEYVALUE_SEPARATOR) < 0) {
				assert keyvalue.equals("null"): "A non-null, incorrectly formatted attribute: " + keyvalue;
				continue;
			}
			String[] tokens = keyvalue.split(DataInfo.ATTRIBUTE_KEYVALUE_SEPARATOR);
			attr.and(tokens[0], tokens[1]);
		}
		sc.close();
		return new DataInfo(classId, methodId, dataId, line, instructionIndex, t, d, attr);
	}
	

}
