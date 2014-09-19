package selogger.reader;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.zip.InflaterInputStream;

import selogger.EventId;
import selogger.logging.BinaryFileListStream;
import selogger.logging.TypeIdMap;

public abstract class EventReader {

	public enum LogContent { 
		EventType, EventId, ThreadId, LocationId, ObjectId, Index, FixedData, CompactData, ObjectData, NoUse4, NoUse8;
	}; 

	private LogDirectory dir;
	protected ObjectTypeMap objectTypeMap;
	protected LinkedList<Event> eventQueue; 
	protected long eventCount;
	protected boolean processParams;
	protected ByteBuffer buffer;
	private ByteBuffer decompressionBuffer;
	private int fileIndex;

	protected EventReader(LogDirectory dir) {
		eventQueue = new LinkedList<Event>();
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

	
	public Event nextEvent() {
		if (!eventQueue.isEmpty()) {
			return eventQueue.removeFirst();
		} else {
			return readEvent();
		}
	}
	
	protected Event readEvent() {
		// try to read the next event from a stream.
		while (buffer != null && buffer.remaining() == 0) {
			boolean result = load();
			if (!result) return null;
		}
		if (buffer == null) return null; // end-of-streams
		
		// read header
		Event e = new Event();
		readFormat(e, LogContent.EventType);
		LogContent[] format = getEventDataFormat(e.getEventType());
		assert format != null: "Data format is not defined for event type: " + e.getEventType();
		for (LogContent c: format) {
			readFormat(e, c);
		}
		assignEventId(e);
		assert e.getEventId() == eventCount: "Event ID is not sequential: COUNT=" + eventCount + "  ID=" + e.getEventId();
		eventCount++;
		
		if ((e.getEventType() == EventId.EVENT_METHOD_ENTRY) ||
			(e.getEventType() == EventId.EVENT_METHOD_CALL)) {
			if (processParams) {
				int paramType;
				if (e.getEventType() == EventId.EVENT_METHOD_ENTRY) paramType = EventId.EVENT_FORMAL_PARAM;
				else paramType = EventId.EVENT_ACTUAL_PARAM;
				
				ArrayList<Event> params = new ArrayList<Event>();
				boolean nextParam = true;
				while (nextParam) {
					Event candidate = readEvent();
					if (candidate == null) { // end of trace
						nextParam = false;
					} else if (candidate.getEventType() == paramType &&  
						candidate.getLocationId() == e.getLocationId()) {
						params.add(candidate);
					} else {
						nextParam = false;
						eventQueue.addFirst(candidate); // preserve the read event  
					}
				}
				if (params.size() > 0) {
					e.setParams(params);
				}
			}
		}
		
		return e;
	}

	private void readFormat(Event e, LogContent c) {
		switch (c) {
		case EventType:
			int eventType = buffer.getShort();
			e.setEventType(EventId.getBaseEventType(eventType));
			e.setRawEventType(eventType);
			break;
		case EventId:
			e.setEventId(buffer.getLong());
			break;
		case ThreadId:
			e.setThreadId(buffer.getInt());
			break;
		case LocationId:
			e.setLocationId(buffer.getInt());
			break;
		case ObjectId:
			long objectId = buffer.getLong();
			e.setObjectId(objectId);
			int objectTypeId = objectTypeMap.getObjectTypeId(objectId);
			e.setObjectType(objectTypeId, objectTypeMap.getTypeName(objectTypeId));
			break;
		case Index:
			e.setParamIndex(buffer.getInt());
			break;
		case CompactData:
		case FixedData:
			readValue(e, c);
			break;
		case ObjectData:
			long targetId = buffer.getLong();
			e.setValue(targetId);
			int typeId = objectTypeMap.getObjectTypeId(targetId);
			e.setValueType(typeId, objectTypeMap.getTypeName(typeId));
			break;
		case NoUse4:
			buffer.getInt();
			break;
		case NoUse8:
			buffer.getLong();
			break;
		default:
			assert false: "Unknown Log Content";
		}
	}
	
	private void readValue(Event e, LogContent c) {
		assert (c == LogContent.CompactData || c == LogContent.FixedData): "Value Type";
		assert (EventId.hasType(e.getRawEventType())): "Event has no type: " + e.getRawEventType();
		int decodedType = EventId.decodeDataType(e.getRawEventType());
		if (decodedType == TypeIdMap.TYPEID_OBJECT) {
			long objectId = buffer.getLong();
			e.setValue(objectId); 
			int typeId = objectTypeMap.getObjectTypeId(objectId);
			e.setValueType(typeId, objectTypeMap.getTypeName(typeId));
		} else {
			e.setValueType(decodedType, objectTypeMap.getTypeName(decodedType));
			switch (decodedType) {
			case TypeIdMap.TYPEID_VOID:
				e.setValue(void.class);
				if (c == LogContent.FixedData) buffer.getLong();
				break;
			case TypeIdMap.TYPEID_BYTE:
				e.setValue((byte)buffer.getInt());
				if (c == LogContent.FixedData) buffer.getInt();
				break;
			case TypeIdMap.TYPEID_CHAR:
				e.setValue((char)buffer.getInt());
				if (c == LogContent.FixedData) buffer.getInt();
				break;
			case TypeIdMap.TYPEID_DOUBLE:
				e.setValue(buffer.getDouble());
				break;
			case TypeIdMap.TYPEID_FLOAT:
				e.setValue(buffer.getFloat());
				if (c == LogContent.FixedData) buffer.getInt();
				break;
			case TypeIdMap.TYPEID_INT:
				e.setValue(buffer.getInt());
				if (c == LogContent.FixedData) buffer.getInt();
				break;
			case TypeIdMap.TYPEID_LONG:
				e.setValue(buffer.getLong());
				break;
			case TypeIdMap.TYPEID_SHORT:
				e.setValue((short)buffer.getInt());
				if (c == LogContent.FixedData) buffer.getInt();
				break;
			case TypeIdMap.TYPEID_BOOLEAN:
				e.setValue( buffer.getInt() != 0 );
				if (c == LogContent.FixedData) buffer.getInt();
				break;
			default:
				assert false: "Unknown Data Type";
			}
		}
	}
	
	/**
	 * This method skips the specified number of events.  This is a helper method for seek(long).   
	 */


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
	
	protected abstract LogContent[] getEventDataFormat(int eventType);
	protected abstract void assignEventId(Event e);
	protected abstract void skipEvent(ByteBuffer buffer, int count);

}
