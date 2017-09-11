package selogger.reader;

import java.util.ArrayList;

import selogger.EventType;
import selogger.logging.TypeIdMap;
import selogger.weaver.method.Descriptor;

public class Event {

	private EventType eventType;
	private long eventId;
	private int threadId;
	private long locationId;
	
	private boolean objectIdAvailable;
	private boolean objectTypeIdAvailable;
	private boolean objectTypeNameAvailable;
	private boolean paramIndexAvailable;
	private boolean valueTypeIdAvailable;
	private boolean valueTypeNameAvailable;
	
	private ArrayList<Event> params; // METHOD_ENTRY
	
	private long objectId; // EVENT_OBJECT_CREATION_COMPLETED, EVENT_OBJECT_INITIALIZED, EVENT_ARRAY_LOAD, EVENT_ARRAY_STORE, EVENT_PUT_INSTANCE_FIELD, EVENT_NEW_ARRAY, EVENT_MULTI_NEW_ARRAY
	private String objectTypeName;
	private int objectTypeId;
	private int paramIndex; // index to specify a parameter/an array element. EVENT_FORMAL_PARAM, EVENT_ACTUAL_PARAM, EVENT_ARRAY_LOAD, EVENT_ARRAY_STORE, EVENT_NEW_ARRAY(size)
	private int paramCount;
	private int intValue; // EVENT_FORMAL_PARAM, EVENT_ACTUAL_PARAM, EVENT_ARRAY_LOAD_RESULT, EVENT_ARRAY_STORE, EVENT_METHOD_EXCEPTIONAL_EXIT, EVENT_THROW, EVENT_METHOD_NORMAL_EXIT, EVENT_RETURN_VALUE_AFTER_CALL, EVENT_GET_INSTANCE_FIELD, EVENT_GET_FIELD_RESULT, EVENT_PUT_STATIC_FIELD, EVENT_PUT_INSTANCE_FIELD
	private long longValue;
	private float floatValue;
	private double doubleValue;
	private Descriptor valueType;
	
	private static Long NULL_ID=0L; 
	
	public Event() {
		objectIdAvailable = false;
		objectTypeIdAvailable = false;
		objectTypeNameAvailable = false;
		paramIndexAvailable = false;
		valueType = null;
		valueTypeNameAvailable = false;
		params = null;
	}
	
	public void setObjectId(long objectId) {
		this.objectId = objectId;
		objectIdAvailable = true;
	}

	public void setObjectType(int typeId, String dataType) {
		objectTypeId = typeId;
		objectTypeIdAvailable = true;
		if (dataType != null) {
			this.objectTypeName = dataType;
			objectTypeNameAvailable = true;
		}
	}
	

	public void setValueType(Descriptor type) {
		valueType = type;
	}
	
	public void setParamIndex(int paramIndex) {
		this.paramIndex = paramIndex;
		paramIndexAvailable = true;
	}
	
	public void setIntValue(int value) {
		this.intValue = value;
	}

	public void setLongValue(long value) {
		this.longValue = value;
	}
	
	public void setFloatValue(float value) {
		this.floatValue = value;
	}
	
	public void setDoubleValue(double value) {
		this.doubleValue = value;
	}

	public void setParams(ArrayList<Event> params) {
		this.params = params;
	}
	
	public EventType getEventType() {
		return eventType;
	}
	
	public long getEventId() {
		return eventId;
	}
	
	public int getThreadId() {
		return threadId;
	}
	
	public long getLocationId() {
		return locationId;
	}
	
	public ArrayList<Event> getParams() {
		return params;
	}
	
	/**
	 * This method returns the type information of the value.
	 * @return
	 */
	public Descriptor getValueType() {
		assert valueTypeNameAvailable: "Value type name is not available for this event.";
		return valueType;
	}

	public long getObjectId() {
		assert objectIdAvailable: "Object ID is not available for this event.";
		return objectId;
	}
	
	public String getObjectType() {
		assert objectTypeNameAvailable: "Object type name is not available for this event.";
		return objectTypeName; 
	}
	
	public int getObjectTypeId() {
		assert objectTypeIdAvailable: "Object type ID is not available for this event.";
		return objectTypeId;
	}
	
	public int getParamIndex() {
		assert paramIndexAvailable: "Parameter Index is not available for this event.";
		return paramIndex;
	}
	
	public int getParamCount() {
		assert eventType == EventType.METHOD_ENTRY || eventType == EventType.CALL;
		return paramCount;
	}

	public void setParamCount(int count) {
		assert eventType == EventType.METHOD_ENTRY || eventType == EventType.CALL;
		paramCount = count;
	}

	/**
	 * @return
	 */
	public Object getValue() {
		switch (valueType) {
		case Void:
			return void.class;
		case Boolean:
			return getIntValueAsBoolean();
		case Byte:
			return getIntValueAsByte();
		case Char:
			return getIntValueAsChar();
		case Double:
			return getDoubleValue();
		case Float:
			return getFloatValue();
		case Integer:
			return getIntValue();
		case Long:
			return getLongValue();
		case Object:
		case Exception:
			return getLongValue();
		case Short:
			return getIntValueAsShort();
		}
		return NULL_ID;
	}
	
	public long getLongValue() {
		return longValue;
	}
	
	public float getFloatValue() {
		assert valueType == Descriptor.Float;
		return floatValue;
	}
	
	public double getDoubleValue() {
		assert valueType == Descriptor.Double;
		return doubleValue;
	}

	/**
	 * @return int, char, short, byte as an integer.
	 */
	public int getIntValue() {
		return intValue;
	}
	
	public short getIntValueAsShort() {
		assert valueType == Descriptor.Short;
		return (short)intValue;
	}

	public boolean getIntValueAsBoolean() {
		assert valueType == Descriptor.Boolean;
		return intValue != 0;
	}

	public byte getIntValueAsByte() {
		assert valueType == Descriptor.Byte;
		return (byte)intValue;
	}

	public char getIntValueAsChar() {
		assert valueType == Descriptor.Char;
		return (char)intValue;
	}

	public void setEventId(long eventId) {
		this.eventId = eventId;
	}
	
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}
	
	public void setLocationId(long locationId) {
		this.locationId = locationId;
	}
	
	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("EventId=");
		buf.append(eventId);
		buf.append(",");
		buf.append("Event=");
		buf.append(eventType.toString());
		buf.append(",");
		buf.append("EventType=");
		buf.append(eventType.ordinal());
		buf.append(",");
		buf.append("ThreadId=");
		buf.append(threadId);
		buf.append(",");
		buf.append("LocationId=");
		buf.append(locationId);
		if (paramIndexAvailable) {
			buf.append(",");
			buf.append("paramIndex=");
			buf.append(paramIndex);
		}
		if (objectIdAvailable) {
			buf.append(",");
			buf.append("objectId=");
			buf.append(objectId);
			if (objectTypeNameAvailable) {
				buf.append(",");
				buf.append("objectType=");
				buf.append(objectTypeName);
			} else if (objectTypeIdAvailable) {
				buf.append(",");
				buf.append("objectTypeId=");
				buf.append(objectTypeId);
			}
		}
		if (valueType != null) {
			buf.append(",");
			buf.append("value=");
			if (valueType == Descriptor.Float) {
				buf.append(floatValue);
			} else if (valueType == Descriptor.Double) {
				buf.append(doubleValue);
			} else if (valueType == Descriptor.Void) {
				buf.append("void");
			} else if (valueType == Descriptor.Integer ||
					valueType == Descriptor.Short ||
					valueType == Descriptor.Char ||
					valueType == Descriptor.Byte) {
				buf.append(intValue);
			} else if (valueType == Descriptor.Boolean) {
				buf.append(getIntValueAsBoolean());
			} else if (valueType == Descriptor.Long) {
				buf.append(longValue);
			} else {
				buf.append(longValue);
			}
			buf.append(",valueType=" + valueType.toString());
		}
		return buf.toString();
	}

}
