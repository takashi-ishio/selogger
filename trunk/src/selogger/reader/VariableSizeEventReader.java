package selogger.reader;

import java.nio.ByteBuffer;

import selogger.EventId;

public class VariableSizeEventReader extends EventReader {

	public VariableSizeEventReader(LogDirectory dir) {
		super(dir);
	}
	
	private static final LogContent[][] variableRecordFormat;
	static {
		LogContent[] noparam = new LogContent[] { LogContent.EventId, LogContent.ThreadId, LogContent.LocationId };
		LogContent[] objectId = new LogContent[] { LogContent.EventId, LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId };  
		LogContent[] paramAndValue = new LogContent[] { LogContent.EventId, LogContent.ThreadId, LogContent.LocationId, LogContent.Index, LogContent.CompactData };  
		LogContent[] objectAndIndex = new LogContent[] { LogContent.EventId, LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.Index };  
		LogContent[] full = new LogContent[] { LogContent.EventId, LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.Index, LogContent.CompactData };  
		LogContent[] fullObject = new LogContent[] { LogContent.EventId, LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.Index, LogContent.ObjectData };  
		LogContent[] objectAndValue = new LogContent[] { LogContent.EventId, LogContent.ThreadId, LogContent.LocationId, LogContent.ObjectId, LogContent.CompactData };  
		LogContent[] value = new LogContent[] { LogContent.EventId, LogContent.ThreadId, LogContent.LocationId, LogContent.CompactData };  
		
		variableRecordFormat = new LogContent[EventId.MAX_EVENT_TYPE+1][];
		variableRecordFormat[EventId.EVENT_METHOD_ENTRY] = noparam; 
		variableRecordFormat[EventId.EVENT_METHOD_CALL] = noparam; 
		variableRecordFormat[EventId.EVENT_LABEL] = noparam; 
		variableRecordFormat[EventId.EVENT_GET_STATIC_FIELD] = noparam; 
		variableRecordFormat[EventId.EVENT_OBJECT_CREATION_COMPLETED] = objectId;
		variableRecordFormat[EventId.EVENT_OBJECT_INITIALIZED] = objectId;
		variableRecordFormat[EventId.EVENT_INSTANCEOF] = objectAndValue;
		variableRecordFormat[EventId.EVENT_MULTI_NEW_ARRAY] = objectId;
		variableRecordFormat[EventId.EVENT_ARRAY_LENGTH] = objectId;
		variableRecordFormat[EventId.EVENT_MULTI_NEW_ARRAY] = objectId;
		variableRecordFormat[EventId.EVENT_GET_INSTANCE_FIELD] = objectId;
		variableRecordFormat[EventId.EVENT_CATCH] = value;
		variableRecordFormat[EventId.EVENT_MONITOR_ENTER] = objectId;
		variableRecordFormat[EventId.EVENT_MONITOR_EXIT] = objectId;
		variableRecordFormat[EventId.EVENT_METHOD_EXCEPTIONAL_EXIT] = value;
		variableRecordFormat[EventId.EVENT_THROW] = value;
		variableRecordFormat[EventId.EVENT_ACTUAL_PARAM] = paramAndValue;
		variableRecordFormat[EventId.EVENT_FORMAL_PARAM] = paramAndValue;
		variableRecordFormat[EventId.EVENT_ARRAY_LOAD] = objectAndIndex;
		variableRecordFormat[EventId.EVENT_NEW_ARRAY] = objectAndIndex;
		variableRecordFormat[EventId.EVENT_ARRAY_STORE] = full;
		variableRecordFormat[EventId.EVENT_MULTI_NEW_ARRAY_CONTENT] = fullObject;
		variableRecordFormat[EventId.EVENT_PUT_INSTANCE_FIELD] = objectAndValue;
		variableRecordFormat[EventId.EVENT_ARRAY_LOAD_RESULT] = value;
		variableRecordFormat[EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION] = value;
		variableRecordFormat[EventId.EVENT_PUT_STATIC_FIELD] = value;
		variableRecordFormat[EventId.EVENT_METHOD_NORMAL_EXIT] = value;
		variableRecordFormat[EventId.EVENT_RETURN_VALUE_AFTER_CALL] = value;
		variableRecordFormat[EventId.EVENT_GET_FIELD_RESULT] = value;
		variableRecordFormat[EventId.EVENT_ARRAY_LENGTH_RESULT] = value;
		variableRecordFormat[EventId.EVENT_CONSTANT_OBJECT_LOAD] = value;
	}
	
	@Override
	protected LogContent[] getEventDataFormat(int eventType) {
		return variableRecordFormat[eventType];
	}
	
	@Override
	protected void assignEventId(Event e) {
		assert e.getEventId() == eventCount: "Event ID is not consistent with Event Count.";
	}
	
	@Override
	protected void skipEvent(ByteBuffer buffer, int count) {
		for (int i=0; i<count; ++i) {
			readEvent();
		}
	}


}
