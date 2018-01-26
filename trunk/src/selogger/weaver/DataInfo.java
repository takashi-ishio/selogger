package selogger.weaver;

import java.util.Scanner;

import selogger.EventType;
import selogger.weaver.method.Descriptor;

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
	
	public int getMethodId() {
		return methodId;
	}
	
	public int getDataId() {
		return dataId;
	}
	
	public int getLine() {
		return line;
	}
	
	public int getInstructionIndex() {
		return instructionIndex;
	}
	
	public EventType getEventType() {
		return eventType;
	}
	
	public Descriptor getValueDesc() {
		return valueDesc;
	}
	
	public String getAttributes() {
		return attributes;
	}

	/**
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
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
