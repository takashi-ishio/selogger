package selogger.reader;

import java.util.List;

import selogger.logging.TypeIdMap;

public class Event {

	private int eventType;
	private int rawEventType;
	private long eventId;
	private long threadId;
	private long locationId;
	
	private boolean objectIdAvailable;
	private boolean objectTypeIdAvailable;
	private boolean objectTypeNameAvailable;
	private boolean paramIndexAvailable;
	private boolean valueTypeIdAvailable;
	private boolean valueTypeNameAvailable;
	
	private List<Event> params; // METHOD_ENTRY
	
	private long objectId; // EVENT_OBJECT_CREATION_COMPLETED, EVENT_OBJECT_INITIALIZED, EVENT_ARRAY_LOAD, EVENT_ARRAY_STORE, EVENT_PUT_INSTANCE_FIELD, EVENT_NEW_ARRAY, EVENT_MULTI_NEW_ARRAY
	private String objectTypeName;
	private int objectTypeId;
	private int paramIndex; // index to specify a parameter/an array element. EVENT_FORMAL_PARAM, EVENT_ACTUAL_PARAM, EVENT_ARRAY_LOAD, EVENT_ARRAY_STORE, EVENT_NEW_ARRAY(size)
	private int intValue; // EVENT_FORMAL_PARAM, EVENT_ACTUAL_PARAM, EVENT_ARRAY_LOAD_RESULT, EVENT_ARRAY_STORE, EVENT_METHOD_EXCEPTIONAL_EXIT, EVENT_THROW, EVENT_METHOD_NORMAL_EXIT, EVENT_RETURN_VALUE_AFTER_CALL, EVENT_GET_INSTANCE_FIELD, EVENT_GET_FIELD_RESULT, EVENT_PUT_STATIC_FIELD, EVENT_PUT_INSTANCE_FIELD
	private long longValue;
	private float floatValue;
	private double doubleValue;
	private String valueTypeName;
	private int valueTypeId;
	
	private static Long NULL_ID=0L; 
	
	public Event() {
		objectIdAvailable = false;
		objectTypeIdAvailable = false;
		objectTypeNameAvailable = false;
		paramIndexAvailable = false;
		valueTypeId = 0; //TypeIdMap.TYPEID_NULL;
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
	

	public void setValueType(int typeId, String dataType) {
		valueTypeId = typeId;
		valueTypeIdAvailable = true;
		if (dataType != null) {
			this.valueTypeName = dataType;
			valueTypeNameAvailable = true;
		}
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

	public void setParams(List<Event> params) {
		this.params = params;
	}
	
	public int getEventType() {
		return eventType;
	}
	
	public long getEventId() {
		return eventId;
	}
	
	public long getThreadId() {
		return threadId;
	}
	
	public long getLocationId() {
		return locationId;
	}
	
	public List<Event> getParams() {
		return params;
	}
	
	/**
	 * Use getValueType instead.
	 * @return
	 */
	@Deprecated
	public String getDataType() {
		return getValueType();
	}

	/**
	 * Use getValueTypeId instead.
	 * @return
	 */
	public int getDataTypeId() {
		return getValueTypeId();
	}
	
	/**
	 * This method returns the type information of the value.
	 * @return
	 */
	public String getValueType() {
		assert valueTypeNameAvailable: "Value type name is not available for this event.";
		return valueTypeName;
	}

	/**
	 * This method returns the type id of the value.
	 * @return
	 */
	public int getValueTypeId() {
		assert valueTypeIdAvailable: "Value Type ID is not available for this event.";
		return valueTypeId;
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
	
	public int getRawEventType() {
		return rawEventType;
	}
	

	/**
	 * @return
	 */
	public Object getValue() {
		switch (valueTypeId) {
		case TypeIdMap.TYPEID_VOID:
			return void.class;
		case TypeIdMap.TYPEID_BOOLEAN:
			return getIntValueAsBoolean();
		case TypeIdMap.TYPEID_BYTE:
			return getIntValueAsByte();
		case TypeIdMap.TYPEID_CHAR:
			return getIntValueAsChar();
		case TypeIdMap.TYPEID_DOUBLE:
			return getDoubleValue();
		case TypeIdMap.TYPEID_FLOAT:
			return getFloatValue();
		case TypeIdMap.TYPEID_INT:
			return getIntValue();
		case TypeIdMap.TYPEID_LONG:
			return getLongValue();
		case TypeIdMap.TYPEID_NULL:
			return NULL_ID;
		case TypeIdMap.TYPEID_SHORT:
			return getIntValueAsShort();
		default:
			return getLongValue();
		}
	}
	
	public long getLongValue() {
		return longValue;
	}
	
	public float getFloatValue() {
		assert valueTypeId == TypeIdMap.TYPEID_FLOAT;
		return floatValue;
	}
	
	public double getDoubleValue() {
		assert valueTypeId == TypeIdMap.TYPEID_DOUBLE;
		return doubleValue;
	}

	/**
	 * @return int, char, short, byte as an integer.
	 */
	public int getIntValue() {
		return intValue;
	}
	
	public short getIntValueAsShort() {
		assert valueTypeId == TypeIdMap.TYPEID_SHORT;
		return (short)intValue;
	}

	public boolean getIntValueAsBoolean() {
		assert valueTypeId == TypeIdMap.TYPEID_BOOLEAN;
		return intValue != 0;
	}

	public byte getIntValueAsByte() {
		assert valueTypeId == TypeIdMap.TYPEID_BYTE;
		return (byte)intValue;
	}

	public char getIntValueAsChar() {
		assert valueTypeId == TypeIdMap.TYPEID_CHAR;
		return (char)intValue;
	}

	public void setEventId(long eventId) {
		this.eventId = eventId;
	}
	
	public void setEventType(int eventType) {
		this.eventType = eventType;
	}
	
	public void setRawEventType(int rawEventType) {
		this.rawEventType = rawEventType;
	}
	
	public void setLocationId(long locationId) {
		this.locationId = locationId;
	}
	
	public void setThreadId(long threadId) {
		this.threadId = threadId;
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("EventId=");
		buf.append(eventId);
		buf.append(",");
		buf.append("EventType=");
		buf.append(eventType);
		buf.append(",");
		buf.append("RawEventType=");
		buf.append(rawEventType);
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
		if (valueTypeId != TypeIdMap.TYPEID_NULL) {
			buf.append(",");
			buf.append("value=");
			if (valueTypeId == TypeIdMap.TYPEID_FLOAT) {
				buf.append(floatValue);
			} else if (valueTypeId == TypeIdMap.TYPEID_DOUBLE) {
				buf.append(doubleValue);
			} else if (valueTypeId == TypeIdMap.TYPEID_VOID) {
				buf.append("void");
			} else if (valueTypeId == TypeIdMap.TYPEID_INT ||
					valueTypeId == TypeIdMap.TYPEID_SHORT ||
					valueTypeId == TypeIdMap.TYPEID_CHAR ||
					valueTypeId == TypeIdMap.TYPEID_BYTE) {
				buf.append(intValue);
			} else if (valueTypeId == TypeIdMap.TYPEID_BOOLEAN) {
				buf.append(getIntValueAsBoolean());
			} else if (valueTypeId == TypeIdMap.TYPEID_LONG) {
				buf.append(longValue);
			} else {
				buf.append(longValue);
			}
			if (valueTypeNameAvailable) {
				buf.append(",");
				buf.append("dataType=");
				buf.append(valueTypeName);
			} else if (valueTypeIdAvailable) {
				buf.append(",");
				buf.append("dataTypeId=");
				buf.append(valueTypeId);
			}
		}
		return buf.toString();
	}

}
