package selogger.weaver;

import selogger.EventType;
import selogger.weaver.method.Descriptor;

public class DataIdEntry {

	private static final String SEPARATOR = ",";
	
	private int classId;
	private int methodId;
	private int dataId;
	private int line;
	private int instructionIndex;
	private EventType eventType;
	private Descriptor valueDesc;
	private String attributes;
	
	public DataIdEntry(int classId, int methodId, int dataId, int line, int instructionIndex, EventType eventType, Descriptor valueDesc, String attributes) {
		this.classId = classId;
		this.methodId = methodId;
		this.dataId = dataId;
		this.line = line;
		this.instructionIndex = instructionIndex;
		this.eventType = eventType;
		this.valueDesc = valueDesc;
		this.attributes = attributes;
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
		buf.append(valueDesc.getNormalizedString());
		buf.append(SEPARATOR);
		buf.append(attributes);
		return buf.toString();
	}

}
