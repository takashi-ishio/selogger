package selogger.weaver;

import selogger.EventType;
import selogger.logging.io.MemoryLogger;
import selogger.weaver.method.Descriptor;

/**
 * An iterator object to read events from MemoryLogger.
 */
public class EventIterator {
	
	private MemoryLogger memoryLogger;
	private WeaveLog weaveLog;
	private int eventIndex;
	
	/**
	 * Create an instance for a memory logger
	 * @param mem is a MemoryLogger
	 * @param log is the weave log including dataId information
	 */
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
	
	/**
	 * @return dataId of the event
	 */
	public int getDataId() {
		return memoryLogger.getEvents().get(eventIndex).getDataId();
	}
	
	/**
	 * @return class name of the event 
	 */
	public String getClassName() {
		int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
		int methodId = weaveLog.getDataEntries().get(dataId).getMethodId();
		return weaveLog.getMethods().get(methodId).getClassName();
	}
	
	/**
	 * @return method name of the event 
	 */
	public String getMethodName() {
		int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
		int methodId = weaveLog.getDataEntries().get(dataId).getMethodId();
		return weaveLog.getMethods().get(methodId).getMethodName();
	}
	
	/**
	 * @return event type  
	 */
	public EventType getEventType() {
		int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
		return weaveLog.getDataEntries().get(dataId).getEventType();
	}
	
	/**
	 * @return the observed value of the event  
	 */
	public float getFloatValue() {
		return memoryLogger.getEvents().get(eventIndex).getFloatValue();
	}

	/**
	 * @return the observed value of the event  
	 */
	public int getIntValue() {
		return memoryLogger.getEvents().get(eventIndex).getIntValue();
	}

	/**
	 * @return the observed value of the event  
	 */
	public double getDoubleValue() {
		return memoryLogger.getEvents().get(eventIndex).getDoubleValue();
	}
	
	/**
	 * @return the observed value of the event  
	 */
	public boolean getBooleanValue() {
		return memoryLogger.getEvents().get(eventIndex).getBooleanValue();
	}
	
	/**
	 * @return the observed value of the event  
	 */
	public byte getByteValue() {
		return memoryLogger.getEvents().get(eventIndex).getByteValue();
	}
	
	/**
	 * @return the observed value of the event  
	 */
	public char getCharValue() {
		return memoryLogger.getEvents().get(eventIndex).getCharValue();
	}
	
	/**
	 * @return the observed value of the event  
	 */
	public short getShortValue() {
		return memoryLogger.getEvents().get(eventIndex).getShortValue();
	}

	/**
	 * @return the observed value of the event  
	 */
	public Object getObjectValue() {
		return memoryLogger.getEvents().get(eventIndex).getObjectValue();
	}
	
	/**
	 * @return the observed value of the event  
	 */
	public long getLongValue() {
		return memoryLogger.getEvents().get(eventIndex).getLongValue();
	}

	/**
	 * @return the observed value type   
	 */
	public Class<?> getValueType() {
		return memoryLogger.getEvents().get(eventIndex).getValueType();
	}

	/**
	 * @return the descriptor of the observed value
	 */
	public Descriptor getDataIdValueDesc() {
		int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
		return weaveLog.getDataEntries().get(dataId).getValueDesc();
	}
	
	/**
	 * @return attributes of the event data Id
	 */
	public String getAttributes() {
		int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
		return weaveLog.getDataEntries().get(dataId).getAttributes();
	}
}
