package selogger.reader;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.InflaterInputStream;

import selogger.EventId;
import selogger.logging.BinaryFileListStream;
import selogger.logging.FixedSizeEventStream;
import selogger.logging.TypeIdMap;

public class EventReader {

	private LogDirectory dir;
	protected ObjectTypeMap objectTypeMap;
	protected Event nextEvent;
	protected long eventCount;
	protected boolean processParams;
	protected ByteBuffer buffer;
	private ByteBuffer decompressionBuffer;
	private int fileIndex;

	protected EventReader(LogDirectory dir) {
		nextEvent = null;
		objectTypeMap = new ObjectTypeMap(dir.getDirectory());
		this.dir = dir; 
		this.buffer = ByteBuffer.allocate(dir.getBufferSize());
		if (dir.doDecompress()) decompressionBuffer = ByteBuffer.allocate(dir.getBufferSize() / 4);
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
			int bufsize = dir.getBufferSize();
			buffer.clear();
			FileInputStream stream = new FileInputStream(dir.getLogFile(fileIndex));
			if (dir.doDecompress()) {
				decompressionBuffer.clear();
				stream.getChannel().read(decompressionBuffer);
				stream.close();
				decompressionBuffer.flip();
				InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(decompressionBuffer.array(), decompressionBuffer.position(), decompressionBuffer.limit()));
				int size = inflater.read(buffer.array());
				int count = 0;
				while (count < bufsize && size != -1) { 
					count += size;
					size = inflater.read(buffer.array(), count, buffer.capacity()-count);
				}
				buffer.position(count);
			} else {
				stream.getChannel().read(buffer);
				stream.close();
			}
			buffer.flip();
			fileIndex++;
			return true;
		} catch (IOException e) {
			buffer.clear();
			buffer.flip();
			return false;
		}
	}

	/**
	 * Use readEvent instead.
	 * @return
	 */
	@Deprecated
	public Event nextEvent() {
		return readEvent();
	}
	
	public Event readEvent() {
		Event e;
		// read header
		if (nextEvent != null) { 
			e = nextEvent;
			nextEvent = null;
		} else {
			e = readEventFromBuffer();
		}
		
		if (e != null && 
			(e.getEventType() == EventId.EVENT_METHOD_ENTRY ||
			 e.getEventType() == EventId.EVENT_METHOD_CALL)) {
			if (processParams) {
				int paramType;
				if (e.getEventType() == EventId.EVENT_METHOD_ENTRY) paramType = EventId.EVENT_FORMAL_PARAM;
				else paramType = EventId.EVENT_ACTUAL_PARAM;

				ArrayList<Event> params = new ArrayList<Event>(); 
				boolean nextParam = true;
				while (nextParam) {
					Event candidate = readEventFromBuffer();
					
					if (candidate == null) { // end of trace
						nextParam = false;
					} else if (candidate.getEventType() == paramType &&  
						candidate.getLocationId() == e.getLocationId()) {
						params.add(candidate);
					} else {
						nextParam = false;
						nextEvent = candidate; // preserve the read event
					}
				}
				if (params.size() > 0) e.setParams(params);

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
		fixedRecordFormat[EventId.EVENT_ARRAY_LOAD] = value;
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
		e.setEventId(eventCount++);
		int eventType = buffer.getShort();
		int baseEventType = EventId.getBaseEventType(eventType);
		e.setEventType(baseEventType);
		e.setRawEventType(eventType);
		e.setThreadId(buffer.getInt());
		e.setLocationId(buffer.getInt());

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

	public void seek(long eventId) {
		if (eventCount <= eventId && 
			(eventId / BinaryFileListStream.EVENTS_PER_FILE) == (eventCount / BinaryFileListStream.EVENTS_PER_FILE)) {
			skipEvent(buffer, (int)(eventId - eventCount));
		} else {
			fileIndex = (int)(eventId / BinaryFileListStream.EVENTS_PER_FILE);
			eventCount = fileIndex * BinaryFileListStream.EVENTS_PER_FILE;
			boolean success = load(); // load a file and fileIndex++
			if (success) {
				skipEvent(buffer, (int)(eventId - eventCount));
			}
		}
	}
	
	/**
	 * This method skips the specified number of events.  This is a helper method for seek(long).   
	 */
	protected void skipEvent(ByteBuffer buffer, int count) {
		if (buffer.position() + FixedSizeEventStream.BYTES_PER_EVENT * count >= buffer.limit()) {
			buffer.position(buffer.limit());
			eventCount += (buffer.limit() - buffer.position()) / FixedSizeEventStream.BYTES_PER_EVENT;
		} else {
			buffer.position(buffer.position() + FixedSizeEventStream.BYTES_PER_EVENT * count);
			eventCount += count;
		}
	}

}
