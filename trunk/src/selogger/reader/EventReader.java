package selogger.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import selogger.EventId;
import selogger.logging.TypeIdMap;
import selogger.logging.io.EventDataStream;

public class EventReader {

	private LogDirectory dir;
	protected ObjectTypeMap objectTypeMap;
	protected long nextEventId;
	protected boolean processParams;
	protected ByteBuffer buffer;
	private int fileIndex;
	private LocationIdMap locationIdMap;

	protected EventReader(LogDirectory dir, LocationIdMap locationIdMap) {
		objectTypeMap = new ObjectTypeMap(dir.getDirectory());
		this.locationIdMap = locationIdMap;
		this.dir = dir; 
		this.buffer = ByteBuffer.allocate(dir.getBufferSize());
		this.fileIndex = 0;
		load();
	}
	
	
	/**
	 * Associate a entry/call event with its parameter events.
	 * @param processParams
	 */
	public void setProcessParams(boolean processParams) {
		this.processParams = processParams;
	}
	
	public void close() {
		
	}
	
	public ObjectTypeMap getObjectTypeMap() {
		return objectTypeMap;
	}

	
	protected boolean load() {
		if (fileIndex >= dir.getLogFileCount()) {
			buffer.clear();
			buffer.flip();
			return false;
		}
		try {
			buffer.clear();
			FileInputStream stream = new FileInputStream(dir.getLogFile(fileIndex));
			stream.getChannel().read(buffer);
			stream.close();
			buffer.flip();
			fileIndex++;
			return true;
		} catch (IOException e) {
			buffer.clear();
			buffer.flip();
			return false;
		}
	}
	
	public Event readEvent() {
		Event e;
		// read header
		e = readEventFromBuffer();
		if (processParams) { 
			// skip param events
			while (e != null && (e.getEventType() == EventId.EVENT_ACTUAL_PARAM || e.getEventType() == EventId.EVENT_FORMAL_PARAM)) {
				e = readEventFromBuffer();
			}
		}
		
		if (e != null && 
			(e.getEventType() == EventId.EVENT_METHOD_ENTRY ||
			 e.getEventType() == EventId.EVENT_METHOD_CALL)) {
			if (processParams && e.getParamCount() > 0) {
				int paramType;
				if (e.getEventType() == EventId.EVENT_METHOD_ENTRY) paramType = EventId.EVENT_FORMAL_PARAM;
				else paramType = EventId.EVENT_ACTUAL_PARAM;

				// Load all the parameter events
				ArrayList<Event> params = e.getParams();
				assert params != null;
				boolean nextParam = params.size() < e.getParamCount();
				while (nextParam) {
					Event candidate = readEventFromBuffer();
					
					if (candidate == null) { // end of trace
						nextParam = false;
					} else if (candidate.getEventType() == paramType &&  
						candidate.getThreadId() == e.getThreadId() && 
						candidate.getLocationId() == e.getLocationId()) {
						params.add(candidate);
						nextParam = params.size() < e.getParamCount();
					}
				}
				if (params.size() > 0 && e.getParams() == null) {
					e.setParams(params);
				}
				// Go back to the original event
				seek(e.getEventId()+1);
			}
		}
		
		return e;
	}
	

	

	public enum LogContent { 
		EventType, EventId, ThreadId, LocationId, ObjectId, Index, FixedData, CompactData, ObjectData, NoUse4, NoUse8;
	}; 

	
	private static final LogContent[] noparam, objectId, paramAndValue, objectAndIndex, full, objectAndValue, value;
	private static final boolean[] hasObjectId = new boolean[EventId.MAX_EVENT_TYPE+1];
	private static final boolean[] hasParamIndex = new boolean[EventId.MAX_EVENT_TYPE+1];
	private static final boolean[] hasValue = new boolean[EventId.MAX_EVENT_TYPE+1];
	
	private static final LogContent[][] fixedRecordFormat;
	static {
		noparam = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.NoUse8, LogContent.NoUse4, LogContent.NoUse8 };
		objectId = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.NoUse4, LogContent.NoUse8 };  
		paramAndValue = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.NoUse8, LogContent.Index, LogContent.FixedData };  
		objectAndIndex = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.Index, LogContent.NoUse8 };  
		full = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.Index, LogContent.FixedData };  
		objectAndValue = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.NoUse4, LogContent.FixedData };  
		value = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.NoUse8, LogContent.NoUse4, LogContent.FixedData };  
		
		fixedRecordFormat = new LogContent[EventId.MAX_EVENT_TYPE+1][];
		fixedRecordFormat[EventId.EVENT_METHOD_ENTRY] = noparam; 
		fixedRecordFormat[EventId.EVENT_METHOD_CALL] = noparam; 
		fixedRecordFormat[EventId.EVENT_LABEL] = noparam; 
		fixedRecordFormat[EventId.EVENT_GET_STATIC_FIELD] = noparam; 
		fixedRecordFormat[EventId.EVENT_OBJECT_CREATION_COMPLETED] = objectId;
		fixedRecordFormat[EventId.EVENT_OBJECT_INITIALIZED] = objectId;
		fixedRecordFormat[EventId.EVENT_INSTANCEOF] = objectAndValue;
		fixedRecordFormat[EventId.EVENT_MULTI_NEW_ARRAY] = objectId;
		fixedRecordFormat[EventId.EVENT_ARRAY_LENGTH] = objectAndValue;
		fixedRecordFormat[EventId.EVENT_MULTI_NEW_ARRAY] = objectId;
		fixedRecordFormat[EventId.EVENT_GET_INSTANCE_FIELD] = objectAndValue;
		fixedRecordFormat[EventId.EVENT_GET_STATIC_FIELD] = value;
		fixedRecordFormat[EventId.EVENT_CATCH] = value;
		fixedRecordFormat[EventId.EVENT_MONITOR_ENTER] = objectId;
		fixedRecordFormat[EventId.EVENT_MONITOR_EXIT] = objectId;
		fixedRecordFormat[EventId.EVENT_METHOD_EXCEPTIONAL_EXIT] = value;
		fixedRecordFormat[EventId.EVENT_THROW] = value;
		fixedRecordFormat[EventId.EVENT_ACTUAL_PARAM] = paramAndValue;
		fixedRecordFormat[EventId.EVENT_FORMAL_PARAM] = paramAndValue;
		fixedRecordFormat[EventId.EVENT_ARRAY_LOAD] = full;
		fixedRecordFormat[EventId.EVENT_NEW_ARRAY] = objectAndIndex;
		fixedRecordFormat[EventId.EVENT_ARRAY_STORE] = full;
		fixedRecordFormat[EventId.EVENT_MULTI_NEW_ARRAY_CONTENT] = full;
		fixedRecordFormat[EventId.EVENT_PUT_INSTANCE_FIELD] = objectAndValue;
		fixedRecordFormat[EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION] = value;
		fixedRecordFormat[EventId.EVENT_PUT_STATIC_FIELD] = value;
		fixedRecordFormat[EventId.EVENT_METHOD_NORMAL_EXIT] = value;
		fixedRecordFormat[EventId.EVENT_RETURN_VALUE_AFTER_CALL] = value;
		fixedRecordFormat[EventId.EVENT_CONSTANT_OBJECT_LOAD] = value;
		
		for (int i=0; i<=EventId.MAX_EVENT_TYPE; ++i) {
			if (fixedRecordFormat[i] != null) {
				hasObjectId[i] = (fixedRecordFormat[i][2] == LogContent.ObjectId);
				hasParamIndex[i] = (fixedRecordFormat[i][3] == LogContent.Index);
				hasValue[i] = (fixedRecordFormat[i][4] == LogContent.FixedData);
			}
		}
	}


	protected Event readEventFromBuffer() {
		// try to read the next event from a stream.
		while (buffer != null && buffer.remaining() == 0) {
			boolean result = load();
			if (!result) return null;
		}
		if (buffer == null) return null; // end-of-streams

		Event e = new Event();
		e.setEventId(nextEventId++);
		int eventId = buffer.getInt();
		int threadId = buffer.getInt();
		long value = buffer.getLong();
		
		int eventType = locationIdMap.getEventType(eventId);
		int baseEventType = EventId.getBaseEventType(eventType);
		e.setEventType(baseEventType);
		e.setRawEventType(eventType);
		e.setThreadId(buffer.getInt());
		e.setLocationId(buffer.getInt());
		
		if (eventType == EventId.EVENT_METHOD_ENTRY ||
			eventType == EventId.EVENT_METHOD_CALL) {
			// decode parameters in a special format
			readEncodedParamsInEvent(e);
		} else {
			// Decode parameters
			long objectId = buffer.getLong();
			if (hasObjectId[baseEventType]) {
				e.setObjectId(objectId);
				int objectTypeId = objectTypeMap.getObjectTypeId(objectId);
				e.setObjectType(objectTypeId, objectTypeMap.getTypeName(objectTypeId));
			}
			
			int paramIndex = buffer.getInt();
			if (hasParamIndex[baseEventType]) {
				e.setParamIndex(paramIndex);
			}
			
			if (hasValue[baseEventType]) {
				readValue(e);
			} else {
				buffer.getLong();
			}
		}
		return e;
	}

	
	
	private void readValue(Event e) {
		int decodedType = EventId.decodeDataType(e.getRawEventType());
		if (decodedType == TypeIdMap.TYPEID_OBJECT) {
			long objectId = buffer.getLong();
			e.setLongValue(objectId); 
			int typeId = objectTypeMap.getObjectTypeId(objectId);
			e.setValueType(typeId, objectTypeMap.getTypeName(typeId));
		} else {
			e.setValueType(decodedType, objectTypeMap.getTypeName(decodedType));
			switch (decodedType) {
			case TypeIdMap.TYPEID_VOID:
				buffer.getLong();
				break;
			case TypeIdMap.TYPEID_BYTE:
			case TypeIdMap.TYPEID_CHAR:
			case TypeIdMap.TYPEID_INT:
			case TypeIdMap.TYPEID_SHORT:
			case TypeIdMap.TYPEID_BOOLEAN:
				e.setIntValue(buffer.getInt());
				buffer.getInt();
				break;
			case TypeIdMap.TYPEID_DOUBLE:
				e.setDoubleValue(buffer.getDouble());
				break;
			case TypeIdMap.TYPEID_FLOAT:
				e.setFloatValue(buffer.getFloat());
				buffer.getInt();
				break;
			case TypeIdMap.TYPEID_LONG:
				e.setLongValue(buffer.getLong());
				break;
			default:
				assert false: "Unknown Data Type";
			}
		}
	}

	/**
	 * Move to a paritcular event.
	 * The specified eventId is obtained by the next readEvent call.
	 * @param eventId
	 */
	public void seek(long eventId) {
		if (eventId == nextEventId) return;
		if ((eventId / EventDataStream.MAX_EVENTS_PER_FILE) != fileIndex-1) { // != on memory file
			fileIndex = (int)(eventId / EventDataStream.MAX_EVENTS_PER_FILE);
			nextEventId = fileIndex * EventDataStream.MAX_EVENTS_PER_FILE;
			boolean success = load(); // load a file and fileIndex++
			if (!success) return;
		}
		int pos = (int)(EventDataStream.BYTES_PER_EVENT * (eventId % EventDataStream.MAX_EVENTS_PER_FILE));
		buffer.position(pos);
		nextEventId = eventId;
	}


	private void readEncodedParamsInEvent(Event e) {
		int paramTypes = buffer.getInt();
		int types = paramTypes >> 9;
		int firstIndex = (paramTypes >> 8) & 1;
		int paramCount = paramTypes & 0xFF;
		e.setParamCount(paramCount);
		if (paramCount > 0) {
			ArrayList<Event> params = new ArrayList<Event>(paramCount);
			int current = buffer.position();
			Event param1 = createParam(e, (types >> 8) & 0xF, firstIndex);
			Event param2 = createParam(e, (types >> 4) & 0xF, firstIndex+1);
			Event param3 = createParam(e, (types >> 0) & 0xF, firstIndex+2);
			if (param1 != null) params.add(param1);
			if (param2 != null) params.add(param2);
			if (param3 != null) params.add(param3);
			e.setParams(params);
			int used = buffer.position() - current; 
			assert used <= 16: "Invalid trace format";
			if (used < 16) {
				// skip the padding data
				buffer.position(buffer.position() + (16-used));
			}
		} else {
			// read and dispose data
			buffer.getInt();
			buffer.getInt();
			buffer.getInt();
			buffer.getInt();
		}
	}
	
	private Event createParam(Event e, int type, int index) {
		if (type == 0) return null;
		Event param = new Event();
		param.setEventId(e.getEventId());
		int eventType;
		if (e.getEventType() == EventId.EVENT_METHOD_ENTRY) eventType = EventId.EVENT_FORMAL_PARAM; 
		else eventType = EventId.EVENT_ACTUAL_PARAM;
		param.setEventType(eventType);
		param.setRawEventType(eventType + type);
		param.setThreadId(e.getThreadId());
		param.setLocationId(e.getLocationId());
		param.setParamIndex(index);

		if (type == TypeIdMap.TYPEID_OBJECT) {
			long objectId = buffer.getLong();
			param.setLongValue(objectId); 
			int typeId = objectTypeMap.getObjectTypeId(objectId);
			param.setValueType(typeId, objectTypeMap.getTypeName(typeId));
		} else {
			param.setValueType(type, objectTypeMap.getTypeName(type));
			switch (type) {
			case TypeIdMap.TYPEID_BYTE:
			case TypeIdMap.TYPEID_CHAR:
			case TypeIdMap.TYPEID_INT:
			case TypeIdMap.TYPEID_SHORT:
			case TypeIdMap.TYPEID_BOOLEAN:
				param.setIntValue(buffer.getInt());
				break;
			case TypeIdMap.TYPEID_DOUBLE:
				param.setDoubleValue(Double.longBitsToDouble(buffer.getLong()));
				break;
			case TypeIdMap.TYPEID_FLOAT:
				param.setFloatValue(Float.intBitsToFloat(buffer.getInt()));
				break;
			case TypeIdMap.TYPEID_LONG:
				param.setLongValue(buffer.getLong());
				break;
			default:
				assert false: "Unknown Data Type";
			}
		}
		return param;
	}


}
