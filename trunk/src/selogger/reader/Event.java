package selogger.reader;

import java.util.List;

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
	private boolean valueAvailable;
	private boolean valueTypeIdAvailable;
	private boolean valueTypeNameAvailable;
	
	private List<Event> params; // METHOD_ENTRY
	
	private long objectId; // EVENT_OBJECT_CREATION_COMPLETED, EVENT_OBJECT_INITIALIZED, EVENT_ARRAY_LOAD, EVENT_ARRAY_STORE, EVENT_PUT_INSTANCE_FIELD, EVENT_NEW_ARRAY, EVENT_MULTI_NEW_ARRAY
	private String objectTypeName;
	private int objectTypeId;
	private int paramIndex; // index to specify a parameter/an array element. EVENT_FORMAL_PARAM, EVENT_ACTUAL_PARAM, EVENT_ARRAY_LOAD, EVENT_ARRAY_STORE, EVENT_NEW_ARRAY(size)
	private Object value; // EVENT_FORMAL_PARAM, EVENT_ACTUAL_PARAM, EVENT_ARRAY_LOAD_RESULT, EVENT_ARRAY_STORE, EVENT_METHOD_EXCEPTIONAL_EXIT, EVENT_THROW, EVENT_METHOD_NORMAL_EXIT, EVENT_RETURN_VALUE_AFTER_CALL, EVENT_GET_INSTANCE_FIELD, EVENT_GET_FIELD_RESULT, EVENT_PUT_STATIC_FIELD, EVENT_PUT_INSTANCE_FIELD
	private String valueTypeName;
	private int valueTypeId;
	
	public Event() {
		objectIdAvailable = false;
		objectTypeIdAvailable = false;
		objectTypeNameAvailable = false;
		paramIndexAvailable = false;
		valueAvailable = false;
		valueTypeIdAvailable = false;
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
	
	public void setValue(Object value) {
		this.value = value;
		valueAvailable = true;
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
	

	public Object getValue() {
		assert valueAvailable: "Value is not available for this event.";
		return value;
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
		if (valueAvailable) {
			buf.append(",");
			buf.append("value=");
			buf.append(value);
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
