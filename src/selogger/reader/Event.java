package selogger.reader;

import org.objectweb.asm.Type;

import selogger.EventType;
import selogger.weaver.ClassInfo;
import selogger.weaver.DataInfo;
import selogger.weaver.MethodInfo;
import selogger.weaver.method.Descriptor;

/**
 * This class represents an event recorded in a trace.
 */
public class Event {

	private long eventId;
	private int dataId;
	private int threadId;
	private long value;
	private DataIdMap map;
	
	private Event[] params; // METHOD_ENTRY
	
	
	/**
	 * Create an instance containing the information.
	 * @param eventId
	 * @param dataId
	 * @param threadId
	 * @param value
	 * @param dataIdMap
	 */
	public Event(long eventId, int dataId, int threadId, long value, DataIdMap dataIdMap) {
		this.eventId = eventId;
		this.dataId = dataId;
		this.threadId = threadId;
		this.value = value;
		this.map = dataIdMap;
	}

	/**
	 * Link formal parameter events to this event.
	 * @param params
	 */
	public void setParams(Event[] params) {
		this.params = params;
	}
	
	/**
	 * @return the event type
	 */
	public EventType getEventType() {
		return map.getDataId(dataId).getEventType();
	}
	
	/**
	 * @return the event ID representing the order of events. 
	 */
	public long getEventId() {
		return eventId;
	}
	
	/**
	 * @return the thread ID.
	 */
	public int getThreadId() {
		return threadId;
	}
	
	/**
	 * @return the data ID of the event.
	 */
	public int getDataId() {
		return dataId;
	}
	
	/**
	 * @return linked parameter events. 
	 */
	public Event[] getParams() {
		return params;
	}
	
	/**
	 * @return the type information of the value recorded in the event.
	 */
	public Descriptor getValueDesc() {
		return map.getDataId(dataId).getValueDesc();
	}
	
	/**
	 * @return the recorded value.
	 */
	public boolean getBooleanValue() {
		assert getValueDesc() == Descriptor.Boolean;
		return value != 0;
	}
	
	/**
	 * @return the recorded value.
	 */
	public byte getByteValue() {
		assert getValueDesc() == Descriptor.Byte;
		return (byte)value;
	}
	
	/**
	 * @return the recorded value.
	 */
	public char getCharValue() {
		assert getValueDesc() == Descriptor.Char;
		return (char)value;
	}
	
	/**
	 * @return the recorded value.
	 */
	public short getShortValue() {
		assert getValueDesc() == Descriptor.Short;
		return (short)value;
	}
	
	/**
	 * @return the recorded value.
	 */
	public int getIntValue() {
		assert getValueDesc() == Descriptor.Integer;
		return (int)value;
	}
	
	/**
	 * @return the recorded value.
	 */
	public long getLongValue() {
		assert getValueDesc() == Descriptor.Long;
		return (long)value;
	}
	
	/**
	 * @return the recorded value.
	 */
	public float getFloatValue() {
		assert getValueDesc() == Descriptor.Float;
		return Float.intBitsToFloat((int)value);
	}

	/**
	 * @return the recorded value.
	 */
	public double getDoubleValue() {
		assert getValueDesc() == Descriptor.Double;
		return Double.longBitsToDouble(value);
	}

	/**
	 * @return the recorded object ID.
	 */
	public long getObjectValue() {
		assert getValueDesc() == Descriptor.Object;
		return value;
	}
	
	/**
	 * @return a raw data ignoring its type.
	 */
	public long getRawValue() {
		return value;
	}

	/**
	 * @return the number of parameters of the ENTRY/CALL event.
	 */
	public int getParamCount() {
		EventType t = getEventType();
		if (t == EventType.METHOD_ENTRY) {
			String desc = map.getMethod(map.getDataId(dataId).getMethodId()).getMethodDesc();
			return Type.getArgumentTypes(desc).length;
		} else if (t == EventType.CALL) {
			String desc = map.getDataId(dataId).getAttribute("Desc", "()V");
			return Type.getArgumentTypes(desc).length;
		} else if (t == EventType.INVOKE_DYNAMIC) {
			String desc = map.getDataId(dataId).getAttribute("Desc", "()V");
			return Type.getArgumentTypes(desc).length;
		} else {
			return 0;
		}
	}

	/**
	 * @return The parameter index if this object represents a parameter.   -1 if unavailable.
	 */
	public int getParamIndex() {
		String index = map.getDataId(dataId).getAttribute("Index", null);
		if (index != null) {
			return Integer.parseInt(index);
		} else {
			return -1;
		}
	}
	
	/**
	 * To access an attribute of the data ID.
	 * @param key
	 * @param defaultValue
	 * @return an attribute value corresponding to the key.  If the key is unavailable, defaultValue is returned.
	 */
	public String getDataAttribute(String key, String defaultValue) { 
		return getDataIdEntry().getAttribute(key, defaultValue);
	}
	
	/**
	 * @return DataInfo object of the event.
	 */
	public DataInfo getDataIdEntry() {
		return map.getDataId(dataId);
	}

	/**
	 * @return MethodInfo object of the event.
	 */
	public MethodInfo getMethodEntry() {
		return map.getMethod(map.getDataId(dataId).getMethodId());
	}
	
	/**
	 * @return ClassInfo object of the event.
	 */
	public ClassInfo getClassEntry() {
		return map.getClassEntry(getMethodEntry().getClassId());
	}

	/**
	 * @return a string representation of the event.
	 */
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("EventId=");
		buf.append(eventId);
		buf.append(",");
		buf.append("EventType=");
		buf.append(getEventType().name());
		buf.append(",");
		buf.append("ThreadId=");
		buf.append(threadId);
		buf.append(",");
		buf.append("DataId=");
		buf.append(dataId);
		buf.append(",");
		buf.append("Value=");
		buf.append(value);
		if (getValueDesc() == Descriptor.Object) {
			buf.append(",objectType=");
			buf.append(map.getObjectType(value));
		}
		if (getEventType() == EventType.METHOD_ENTRY) {
			buf.append(",method:" + getMethodEntry().toString());
		} else if (getEventType() == EventType.CALL) {
			buf.append(",method:" + map.getDataId(dataId).getAttributes());
		}
		return buf.toString();
	}

}
