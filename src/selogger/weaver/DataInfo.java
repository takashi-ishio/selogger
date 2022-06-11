package selogger.weaver;

import java.util.Scanner;

import selogger.EventType;
import selogger.weaver.method.Descriptor;

/**
 * This object is to record attributes of a data ID.
 */
public class DataInfo {

	private static final String SEPARATOR = ",";
	private static final char ATTRIBUTE_KEYVALUE_SEPARATOR = '=';
	private static final char ATTRIBUTE_SEPARATOR = ',';
	
	private int classId;
	private int methodId;
	private int dataId;
	private int line;
	private int instructionIndex;
	private EventType eventType;
	private Descriptor valueDesc;
	private String attributes;
	
	/**
	 * MethodInfo object represented by MethodId.
	 * This reference is provided for convenience.
	 */
	private MethodInfo methodInfo;
	
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
	public DataInfo(int classId, int methodId, int dataId, int line, int instructionIndex, EventType eventType, Descriptor valueDesc, String attributes) {
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
	 * @return additional attributes statically obtained from the instruction.
	 */
	public String getAttributes() {
		return attributes;
	}

	/**
	 * Access a particular attribute of the instruction, assuming the "KEY=VALUE" format.
	 * @param key specifies an attribute key
	 * @param defaultValue is returned if the key is unavailable.
	 * @return the value corresponding to the key.
	 */
	public String getAttribute(String key, String defaultValue) {
		int index = attributes.indexOf(key);
		while (index >= 0) {
			if (index == 0 || attributes.charAt(index-1) == ATTRIBUTE_SEPARATOR) {
				int keyEndIndex = attributes.indexOf(ATTRIBUTE_KEYVALUE_SEPARATOR, index);
				if (keyEndIndex == index + key.length()) {
					int valueEndIndex = attributes.indexOf(ATTRIBUTE_SEPARATOR, keyEndIndex);
					if (valueEndIndex > keyEndIndex) {
						return attributes.substring(index + key.length() + 1, valueEndIndex);
					} else {
						return attributes.substring(index + key.length() + 1);
					}
				}
			}
			index = attributes.indexOf(key, index+1);
		}
		return defaultValue;
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
		buf.append(line);
		buf.append(SEPARATOR);
		buf.append(instructionIndex);
		buf.append(SEPARATOR);
		buf.append(eventType.name());
		buf.append(SEPARATOR);
		buf.append(valueDesc.getString());
		buf.append(SEPARATOR);
		buf.append(attributes);
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
		int line = sc.nextInt();
		int instructionIndex = sc.nextInt();
		EventType t = EventType.valueOf(sc.next());
		Descriptor d = Descriptor.get(sc.next());
		StringBuilder b = new StringBuilder();
		while (sc.hasNext()) {
			b.append(sc.next());
			b.append(DataInfo.ATTRIBUTE_SEPARATOR);
		}
		String attributes = b.toString();
		sc.close();
		return new DataInfo(classId, methodId, dataId, line, instructionIndex, t, d, attributes);
	}
	

}
