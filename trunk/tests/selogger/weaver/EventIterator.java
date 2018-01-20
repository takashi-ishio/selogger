package selogger.weaver;

import selogger.EventType;
import selogger.logging.io.MemoryLogger;
import selogger.weaver.method.Descriptor;

public class EventIterator {
	
	private MemoryLogger memoryLogger;
	private WeaveLog weaveLog;
	private int eventIndex;
	
	
	public EventIterator(MemoryLogger mem, WeaveLog log) {
		memoryLogger = mem;
		weaveLog = log;
		eventIndex = -1;
	}
	
	/**
	 * Proceed to the next event.
	 * This method must be called before calling other getter methods.
	 * @return true if the event data is available.
	 * False indicate the end of data.
	 */
	public boolean next() {
		eventIndex++;
		return eventIndex < memoryLogger.getEvents().size();
	}
	
	public int getDataId() {
		return memoryLogger.getEvents().get(eventIndex).getDataId();
	}
	
	public String getClassName() {
		int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
		int methodId = weaveLog.getDataEntries().get(dataId).getMethodId();
		return weaveLog.getMethods().get(methodId).getClassName();
	}
	
	public String getMethodName() {
		int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
		int methodId = weaveLog.getDataEntries().get(dataId).getMethodId();
		return weaveLog.getMethods().get(methodId).getMethodName();
	}
	
	public EventType getEventType() {
		int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
		return weaveLog.getDataEntries().get(dataId).getEventType();
	}

	public int getIntValue() {
		return memoryLogger.getEvents().get(eventIndex).getIntValue();
	}

	public double getDoubleValue() {
		return memoryLogger.getEvents().get(eventIndex).getDoubleValue();
	}
	
	public boolean getBooleanValue() {
		return memoryLogger.getEvents().get(eventIndex).getBooleanValue();
	}
	
	public byte getByteValue() {
		return memoryLogger.getEvents().get(eventIndex).getByteValue();
	}
	
	public char getCharValue() {
		return memoryLogger.getEvents().get(eventIndex).getCharValue();
	}
	
	public short getShortValue() {
		return memoryLogger.getEvents().get(eventIndex).getShortValue();
	}

	public Object getObjectValue() {
		return memoryLogger.getEvents().get(eventIndex).getObjectValue();
	}

	public Class<?> getValueType() {
		return memoryLogger.getEvents().get(eventIndex).getValueType();
	}

	public Descriptor getDataIdValueDesc() {
		int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
		return weaveLog.getDataEntries().get(dataId).getValueDesc();
	}
	
	public String getAttributes() {
		int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
		return weaveLog.getDataEntries().get(dataId).getAttributes();
	}
}
