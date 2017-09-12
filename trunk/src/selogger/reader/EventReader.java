package selogger.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import selogger.EventType;
import selogger.logging.TypeIdMap;
import selogger.logging.io.EventDataStream;
import selogger.weaver.method.Descriptor;

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
			while (e != null && (e.getEventType() == EventType.ACTUAL_PARAM || e.getEventType() == EventType.FORMAL_PARAM)) {
				e = readEventFromBuffer();
			}
		}
		
		if (e != null && 
			(e.getEventType() == EventType.METHOD_ENTRY ||
			 e.getEventType() == EventType.CALL)) {
			if (processParams && e.getParamCount() > 0) {
				EventType paramType;
				if (e.getEventType() == EventType.METHOD_ENTRY) paramType = EventType.FORMAL_PARAM;
				else paramType = EventType.ACTUAL_PARAM;

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
	

	


	protected Event readEventFromBuffer() {
		// try to read the next event from a stream.
		while (buffer != null && buffer.remaining() == 0) {
			boolean result = load();
			if (!result) return null;
		}
		if (buffer == null) return null; // end-of-streams

		Event e = new Event();
		e.setEventId(nextEventId++);
		int dataId = buffer.getInt();
		int threadId = buffer.getInt();
		long value = buffer.getLong();
		System.out.println(dataId + "," + threadId + "," + value);
		
		EventType eventType = locationIdMap.getEventType(dataId);
		e.setEventType(eventType);
		//int baseEventType = EventId.getBaseEventType(eventType);
		//e.setRawEventType(eventType);
		e.setThreadId(threadId);
		e.setLocationId(dataId);
		Descriptor valueType = locationIdMap.getValueType(dataId);
		e.setValueType(valueType);

		switch (valueType) {
		case Byte:
		case Boolean:
		case Char:
		case Short:
		case Integer:
			e.setIntValue((int)value);
			break;
		case Double:
			e.setDoubleValue(Double.longBitsToDouble(value));
			break;
		case Float:
			e.setFloatValue(Float.intBitsToFloat((int)value));
			break;
		case Long:
			e.setLongValue(value);
			break;
		case Void:
			break;
		case Object:
			e.setLongValue(value);
			int typeId = objectTypeMap.getObjectTypeId(value);
			String dataType = objectTypeMap.getObjectTypeName(value);
			e.setObjectType(typeId, dataType);
		}
		return e;
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
	
}
