package selogger.reader;

import org.objectweb.asm.Type;

import selogger.EventType;
import selogger.weaver.ClassInfo;
import selogger.weaver.DataInfo;
import selogger.weaver.MethodInfo;
import selogger.weaver.method.Descriptor;

public class Event {

	private long eventId;
	private int dataId;
	private int threadId;
	private long value;
	private DataIdMap map;
	
	private Event[] params; // METHOD_ENTRY
	
	
	public Event(long eventId, int dataId, int threadId, long value, DataIdMap dataIdMap) {
		this.eventId = eventId;
		this.dataId = dataId;
		this.threadId = threadId;
		this.value = value;
		this.map = dataIdMap;
	}

	public void setParams(Event[] params) {
		this.params = params;
	}
	
	public EventType getEventType() {
		return map.getDataId(dataId).getEventType();
	}
	
	public long getEventId() {
		return eventId;
	}
	
	public int getThreadId() {
		return threadId;
	}
	
	public int getDataId() {
		return dataId;
	}
	
	public Event[] getParams() {
		return params;
	}
	
	/**
	 * This method returns the type information of the value.
	 * @return
	 */
	public Descriptor getValueDesc() {
		return map.getDataId(dataId).getValueDesc();
	}
	
	public boolean getBooleanValue() {
		assert getValueDesc() == Descriptor.Boolean;
		return value != 0;
	}
	
	public byte getByteValue() {
		assert getValueDesc() == Descriptor.Byte;
		return (byte)value;
	}
	
	public char getCharValue() {
		assert getValueDesc() == Descriptor.Char;
		return (char)value;
	}
	
	public short getShortValue() {
		assert getValueDesc() == Descriptor.Short;
		return (short)value;
	}
	
	public int getIntValue() {
		assert getValueDesc() == Descriptor.Integer;
		return (int)value;
	}
	
	public long getLongValue() {
		assert getValueDesc() == Descriptor.Long;
		return (long)value;
	}
	
	public float getFloatValue() {
		assert getValueDesc() == Descriptor.Float;
		return Float.intBitsToFloat((int)value);
	}

	public double getDoubleValue() {
		assert getValueDesc() == Descriptor.Double;
		return Double.longBitsToDouble(value);
	}

	/**
	 * @return an object ID.
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
	 * 
	 * @return -1 if unavailable.
	 */
	public int getParamIndex() {
		String index = map.getDataId(dataId).getAttribute("Index", null);
		if (index != null) {
			return Integer.parseInt(index);
		} else {
			return -1;
		}
	}
	
	public String getDataAttribute(String key, String defaultValue) { 
		return getDataIdEntry().getAttribute(key, defaultValue);
	}
	
	public DataInfo getDataIdEntry() {
		return map.getDataId(dataId);
	}

	public MethodInfo getMethodEntry() {
		return map.getMethod(map.getDataId(dataId).getMethodId());
	}
	
	public ClassInfo getClassEntry() {
		return map.getClassEntry(getMethodEntry().getClassId());
	}

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
		return buf.toString();
	}

}
