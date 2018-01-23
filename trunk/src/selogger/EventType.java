package selogger;



public enum EventType {

	RESERVED, 
	/** EXEC event.
	 * The first event of a method execution.  
	 * A receiver object is recorded for this event if the method is an instance method.
	 */
	METHOD_ENTRY,  
	/** EXEC+PARAMETER event. A formal parameter of a method. 
	 * Index attribute indicates the index of the parameter. */ 
	METHOD_PARAM,
	/** EXEC+PARAMETER. 
	 * In a constructor, this event records an initialized object ("this"). 
	 */
	METHOD_OBJECT_INITIALIZED, 
	/** EXEC event. 
	 * A return statement.  A return value is recorded. */
	METHOD_NORMAL_EXIT, 
	/** EXEC or LABEL.   A throw statement is executed. */
	METHOD_THROW, 
	/** Event for CALL, FIELD, ARRAY, LABEL, and SYNC. 
	 * An exception is terminating a method execution.  
	 * The last instruction (label) is recorded. */
	METHOD_EXCEPTIONAL_EXIT_LABEL,  
	/** EXEC event. The last event of a method execution terminated by an exception.  The exception object is recorded. */
	METHOD_EXCEPTIONAL_EXIT,
	/** CALL event. CALL_RETURN event will be generated if the call is successfully finished. */
	CALL, 
	/** CALL+PARAMETER event. */
	CALL_PARAM, 
	/** CALL event.  If PARAMETER is enabled, this event stores a return value. */
	CALL_RETURN, 
	/** Event for CALL, FIELD, ARRAY, LABEL, and SYNC to record a location where an exception is thrown. 
	 * This is always followed by CATCH event. */
	CATCH_LABEL,
	/** Event for CALL, FIELD, ARRAY, LABEL, and SYNC to record an exception.  */
	CATCH,
	/** CALL+PARAMETER event.  A new instruction. */ 
	NEW_OBJECT, 
	/** CALL+PARAMETER event.  Record an object after a constructor call corresponding to a new instruction. */ 
	NEW_OBJECT_CREATED,
	/** FIELD event recording an object. */
	GET_INSTANCE_FIELD, 
	/** FIELD event recording a field value. */
	GET_INSTANCE_FIELD_RESULT,
	/** FIELD event recording a static field value. */
	GET_STATIC_FIELD, 
	/** FIELD event recording an object. */
	PUT_INSTANCE_FIELD, 
	/** FIELD event recording a field value. */
	PUT_INSTANCE_FIELD_VALUE,
	/** FIELD event recording a field value.  
	 * This event occurs without PUT_INSTANCE_FIELD event for 
	 * an object without initialization. 
	 */
	PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION,
	/** FIELD event recording a static field value. */
	PUT_STATIC_FIELD, 
	/** ARRAY event. */
	ARRAY_LOAD, 
	/** ARRAY event. */
	ARRAY_LOAD_INDEX, 
	/** ARRAY event. */
	ARRAY_LOAD_RESULT, 
	/** ARRAY event. */
	ARRAY_STORE, 
	/** ARRAY event. */
	ARRAY_STORE_INDEX, 
	/** ARRAY event. */
	ARRAY_STORE_VALUE,
	/** ARRAY event. */
	NEW_ARRAY,
	/** ARRAY event. */
	NEW_ARRAY_RESULT, 
	/** ARRAY event. */
	MULTI_NEW_ARRAY, 
	/** ARRAY event. */
	MULTI_NEW_ARRAY_OWNER, 
	/** ARRAY event. */
	MULTI_NEW_ARRAY_ELEMENT,
	/** ARRAY event. */
	ARRAY_LENGTH, 
	/** ARRAY event. */
	ARRAY_LENGTH_RESULT, 
	/** SYNC event. 
	 * Before entering synchronized(object){...}.  
	 * A value stores the object. */
	MONITOR_ENTER, 
	/** SYNC event.  
	 * After entering a synchronized block. 
	 */
	MONITOR_ENTER_RESULT, 
	/** SYNC event.  End of synchronized(object){...}.  The object is recorded. */
	MONITOR_EXIT,  
	/** OBJECT event.  A constant object (usually String) is recorded. */
	OBJECT_CONSTANT_LOAD, 
	/** OBJECT event. */
	OBJECT_INSTANCEOF, 
	/** OBJECT event.  A boolean value is recorded. */
	OBJECT_INSTANCEOF_RESULT,
	/** CALL event. 
	 * This event is recorded when INVOKEDYNAMIC instruction created a function object.  */
	INVOKE_DYNAMIC,
	/** LABEL event. An execution passed a particular code location.  The event records a Data ID corresponding to a previous location. */
	LABEL,
	/** LABEL event placeholder. A jump event does not appear in event sequence.  The Data ID is recorded in Jump instructions. */
	JUMP,
	/** LOCAL event. */
	LOCAL_LOAD, 
	/** LOCAL event. */
	LOCAL_STORE,
	/** LOCAL event. */
	LOCAL_INCREMENT,
	/** A return from subroutine. This is recorded as a local variable access. */
	RET,
	/**	Event placeholder representing an arithmetic division.  
	 * No events actually have this event type, but a LABEL event may refer to this type as a cause. */
	DIVIDE
	;
	
}
