package selogger.reader;


import java.nio.ByteBuffer;

import selogger.EventId;
import selogger.logging.FixedSizeEventStream;

public class FixedSizeEventReader extends EventReader {

	public FixedSizeEventReader(LogDirectory dir) {
		super(dir);
	}
	
	private static final LogContent[][] fixedRecordFormat;
	static {
		LogContent[] noparam = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.NoUse8, LogContent.NoUse4, LogContent.NoUse8 };
		LogContent[] objectId = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.NoUse4, LogContent.NoUse8 };  
		LogContent[] paramAndValue = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.NoUse8, LogContent.Index, LogContent.FixedData };  
		LogContent[] objectAndIndex = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.Index, LogContent.NoUse8 };  
		LogContent[] full = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.Index, LogContent.FixedData };  
		LogContent[] fullObject = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.Index, LogContent.ObjectData };  
		LogContent[] objectAndValue = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.NoUse4, LogContent.FixedData };  
		LogContent[] value = new LogContent[] { LogContent.ThreadId, LogContent.LocationId, LogContent.NoUse8, LogContent.NoUse4, LogContent.FixedData };  
		
		fixedRecordFormat = new LogContent[EventId.MAX_EVENT_TYPE+1][];
		fixedRecordFormat[EventId.EVENT_METHOD_ENTRY] = noparam; 
		fixedRecordFormat[EventId.EVENT_METHOD_CALL] = noparam; 
		fixedRecordFormat[EventId.EVENT_LABEL] = noparam; 
		fixedRecordFormat[EventId.EVENT_GET_STATIC_FIELD] = noparam; 
		fixedRecordFormat[EventId.EVENT_OBJECT_CREATION_COMPLETED] = objectId;
		fixedRecordFormat[EventId.EVENT_OBJECT_INITIALIZED] = objectId;
		fixedRecordFormat[EventId.EVENT_INSTANCEOF] = objectAndValue;
		fixedRecordFormat[EventId.EVENT_MULTI_NEW_ARRAY] = objectId;
		fixedRecordFormat[EventId.EVENT_ARRAY_LENGTH] = objectId;
		fixedRecordFormat[EventId.EVENT_MULTI_NEW_ARRAY] = objectId;
		fixedRecordFormat[EventId.EVENT_GET_INSTANCE_FIELD] = objectId;
		fixedRecordFormat[EventId.EVENT_CATCH] = value;
		fixedRecordFormat[EventId.EVENT_MONITOR_ENTER] = objectId;
		fixedRecordFormat[EventId.EVENT_MONITOR_EXIT] = objectId;
		fixedRecordFormat[EventId.EVENT_METHOD_EXCEPTIONAL_EXIT] = value;
		fixedRecordFormat[EventId.EVENT_THROW] = value;
		fixedRecordFormat[EventId.EVENT_ACTUAL_PARAM] = paramAndValue;
		fixedRecordFormat[EventId.EVENT_FORMAL_PARAM] = paramAndValue;
		fixedRecordFormat[EventId.EVENT_ARRAY_LOAD] = objectAndIndex;
		fixedRecordFormat[EventId.EVENT_NEW_ARRAY] = objectAndIndex;
		fixedRecordFormat[EventId.EVENT_ARRAY_STORE] = full;
		fixedRecordFormat[EventId.EVENT_MULTI_NEW_ARRAY_CONTENT] = fullObject;
		fixedRecordFormat[EventId.EVENT_PUT_INSTANCE_FIELD] = objectAndValue;
		fixedRecordFormat[EventId.EVENT_ARRAY_LOAD_RESULT] = value;
		fixedRecordFormat[EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION] = value;
		fixedRecordFormat[EventId.EVENT_PUT_STATIC_FIELD] = value;
		fixedRecordFormat[EventId.EVENT_METHOD_NORMAL_EXIT] = value;
		fixedRecordFormat[EventId.EVENT_RETURN_VALUE_AFTER_CALL] = value;
		fixedRecordFormat[EventId.EVENT_GET_FIELD_RESULT] = value;
		fixedRecordFormat[EventId.EVENT_ARRAY_LENGTH_RESULT] = value;
		fixedRecordFormat[EventId.EVENT_CONSTANT_OBJECT_LOAD] = value;
	}
	
	@Override
	protected LogContent[] getEventDataFormat(int eventType) {
		return fixedRecordFormat[eventType];
	}
	
	@Override
	protected void assignEventId(Event e) {
		e.setEventId(eventCount);
	}
	
	@Override
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
