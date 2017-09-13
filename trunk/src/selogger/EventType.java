package selogger;



public enum EventType {

	RESERVED, 
	/** The first event of a method execution. */
	METHOD_ENTRY,  
	/** A parameter of a method. */ 
	FORMAL_PARAM,  
	/** The last event of a method execution.  A return value is recorded. */
	METHOD_NORMAL_EXIT, 
	/** An exception is terminating a method execution.  The last instruction (label) is recorded. */
	METHOD_EXCEPTIONAL_EXIT_LABEL,  
	/** The last event of a method execution terminated by an exception.  The exception object is recorded. */
	METHOD_EXCEPTIONAL_EXIT, 
	CATCH, 
	THROW, 
	OBJECT_CREATION_COMPLETED, 
	OBJECT_INITIALIZED, 
	CALL, 
	ACTUAL_PARAM, 
	CALL_RETURN, 
	GET_INSTANCE_FIELD, 
	GET_INSTANCE_FIELD_RESULT,
	GET_STATIC_FIELD, 
	PUT_INSTANCE_FIELD, 
	PUT_INSTANCE_FIELD_VALUE,
	PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION,
	PUT_STATIC_FIELD, 
	ARRAY_LOAD, 
	ARRAY_LOAD_INDEX, 
	ARRAY_LOAD_RESULT, 
	ARRAY_LOAD_FAIL,
	ARRAY_STORE, 
	ARRAY_STORE_INDEX, 
	ARRAY_STORE_VALUE,
	MULTI_NEW_ARRAY, 
	MULTI_NEW_ARRAY_CONTENT,
	ARRAY_LENGTH, 
	ARRAY_LENGTH_RESULT, 
	/** Beginning of synchronized(object){...}.  The object is recorded. */
	MONITOR_ENTER, 
	/** End of synchronized(object){...}.  The object is recorded. */
	MONITOR_EXIT,  
	CONSTANT_OBJECT_LOAD, 
	NEW_OBJECT, 
	NEW_OBJECT_CREATION_COMPLETED,
	NEW_OBJECT_INITIALIZED, 
	NEW_ARRAY,
	NEW_ARRAY_RESULT, 
	INSTANCEOF, 
	INSTANCEOF_RESULT,
	LABEL, 
	JUMP,
	LOCAL_LOAD, 
	LOCAL_STORE, 
	RET;
	
}
