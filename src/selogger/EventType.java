package selogger;


/**
 * This enum defines all the event types in an execution trace.
 * Each event type name is used as a string constant to read/write the event name.
 */
public enum EventType {

	/**
	 * This event is never recorded at runtime.
	 * This event is a placeholder for an event list so that 
	 * a user can recognize which classes are included in 
	 * logging target even if they are empty. 
	 */
	RESERVED, 
	
	/** EXEC event.
	 * This is the first event of a method execution.
	 * A receiver object is recorded with this event 
	 * if the method is an instance method.
	 */
	METHOD_ENTRY,  

	/** EXEC+PARAMETER event. A formal parameter of a method. 
	 * Index attribute indicates the index of the parameter. 
	 */ 
	METHOD_PARAM,

	/** EXEC+PARAMETER. 
	 * This event is recorded in a constructor after an object 
	 * initialization (by "this()" or "super()").
	 * The initialized object ("this") is recorded with this event. 
	 */
	METHOD_OBJECT_INITIALIZED, 
	
	/** EXEC event. 
	 * This event is recorded by an execution of a return statement.
	 * A return value is recorded with this event. 
	 */
	METHOD_NORMAL_EXIT, 
	
	/** EXEC/LABEL event.   
	 * This event is recorded by an execution of a throw statement.
	 * The exception object is recorded with this event. 
	 */
	METHOD_THROW, 
	
	/** EXEC event. 
	 * This event is recorded by the end of a method execution 
	 * terminated by an exception.  
	 * The exception object is recorded with this event. 
	 */
	METHOD_EXCEPTIONAL_EXIT,
	
	/** CALL event. 
	 * This event is recorded before an execution of a method call.
	 * A receiver object is recorded with this event if the method 
	 * is an instance method.
	 */
	CALL, 

	/** CALL+PARAMETER event.
	 * This event is recorded for each actual parameter between 
	 * a CALL event and the actual method call. 
 	 * Index attribute indicates the index of the parameter. 
	 */
	CALL_PARAM,
	
	/** CALL event.  
	 * This event is recorded after a method call is finished 
	 * without exceptions.
	 * A return value is recorded if PARAMETER is enabled. 
	 */
	CALL_RETURN, 
	
	/** Event for CALL, FIELD, ARRAY, LABEL, and SYNC.
	 * This event is recorded when a catch/finally block is executed.
	 * A label is recorded with this event; 
	 * the label points to location where an exception is thrown. 
	 * This is always followed by a CATCH event. 
	 */
	CATCH_LABEL,
	
	/** Event for CALL, FIELD, ARRAY, LABEL, and SYNC.
	 * This event is recorded when a catch/finally block is executed.
	 * An exception object caught by the block is recorded 
	 * with this event.
	 */
	CATCH,
	
	/** CALL+PARAMETER event.  
	 * This event is recorded when a "new" statement created 
	 * an instance of some class. 
	 * This event does not record the object, because
	 * the object is not initialized (i.e. Not accessible) 
	 * at this point of execution.
	 */ 
	NEW_OBJECT,
	
	/** CALL+PARAMETER event.  
	 * This event is recorded after a constructor call is finished.
	 * The initialized object is recorded with this event.
	 */
	NEW_OBJECT_CREATED,
	
	/** FIELD event.
	 * This event is recorded before an instruction 
	 * reads an instance field. 
	 * The accessed object is recorded with this event.
	 * The object may be null if the instruction accidentally used it.
	 */
	GET_INSTANCE_FIELD,
	
	/** FIELD event.
	 * This event is recorded after an instruction read 
	 * an instance field. 
	 * The read value is recorded with this event.
	 * This event is not recorded if NPE occurred.
	 */
	GET_INSTANCE_FIELD_RESULT,
	
	/** FIELD event.
	 * This event is recorded after an instruction read 
	 * a static field. 
	 * The read value is recorded with this event.
	 */
	GET_STATIC_FIELD,
	
	/** FIELD event recording an object.
	 * This event is recorded before an instruction 
	 * writing an instance field.
	 * The target object is recorded with this event.
	 * The object may be null if the instruction accidentally used it.
	 */
	PUT_INSTANCE_FIELD, 
	
	/** FIELD event recording a field value. 
	 * This event is also recorded before an instruction 
	 * writing an instance field.
	 * The written value is recorded with this event.
	 */
	PUT_INSTANCE_FIELD_VALUE,
	
	/** FIELD event recording a field value.  
	 * This event is recorded when a field of an uninitialized 
	 * object is written by an instruction.
	 * This occurs when an anonymous class stores
	 * variables defined in the context. 
	 * The written value is recorded with this event.
	 */
	PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION,
	
	/** FIELD event recording a static field value.
	 * This event is recorded before a static field is written 
	 * by an instruction. 
	 * The written value is recorded with this event.
	 */
	PUT_STATIC_FIELD, 
	
	/** ARRAY event. 
	 * This event is recorded before an instruction reads 
	 * a value from an array.  
	 * An array object is recorded with this event.
	 * The object may be null if the instruction accidentally used it.
	 */
	ARRAY_LOAD, 
	
	/** ARRAY event. 
	 * This event is recorded after ARRAY_LOAD event. 
	 * The index accessed by the instruction is 
	 * recorded with this event.
	 */
	ARRAY_LOAD_INDEX, 
	
	/** ARRAY event. 
	 * This event is recorded after an instruction read 
	 * a value from an array.  
	 * The value is recorded with this event.
	 */
	ARRAY_LOAD_RESULT, 
	
	/** ARRAY event. 
	 * This event is recorded before an instruction writes
	 * a value to an array. 
	 * An array object is recorded with this event. 
	 */
	ARRAY_STORE, 

	/** ARRAY event. 
	 * This event is recorded before an instruction writes
	 * a value to an array. 
	 * The index accessed by the instruction is recorded 
	 * with this event. 
	 */
	ARRAY_STORE_INDEX,
	
	/** ARRAY event. 
	 * This event is recorded before an instruction writes
	 * a value to an array. 
	 * The value written by the instruction is recorded 
	 * with this event. 
	 */
	ARRAY_STORE_VALUE,
	
	/** ARRAY event.
	 * This event is recorded before creating an array. 
	 * The size of an array is recorded with this event.
	 */
	NEW_ARRAY,

	/** ARRAY event.
	 * This event is recorded after an array is created. 
	 * The array object is recorded with this event.
	 */
	NEW_ARRAY_RESULT, 
	
	/** ARRAY event.
	 * This event is recorded after a multi-dimensional 
	 * array is created.
	 * The array object is recorded with this event.
	 */
	MULTI_NEW_ARRAY, 
	
	/** ARRAY event.
	 * This event is recorded after a MULTI_NEW_ARRAY event
	 * for recording array elements.
	 * An array object is recorded with this event.
	 * The following MULTI_NEW_ARRAY_ELEMENT events 
	 * record the contents of the array object. 
	 */
	MULTI_NEW_ARRAY_OWNER, 

	/** ARRAY event.
	 * This event is recorded after a MULTI_NEW_ARRAY_OWNER event
	 * for recording array elements.
	 * An array element is recorded with this event.
	 */
	MULTI_NEW_ARRAY_ELEMENT,
	
	/** ARRAY event.
	 * This event is recorded before an execution of 
	 * an array.length instruction. 
	 * An array object is recorded with this event.
	 */
	ARRAY_LENGTH, 

	/** ARRAY event.
	 * This event is recorded after an execution of 
	 * an array.length instruction. 
	 * The length of the array is recorded with this event.
	 */
	ARRAY_LENGTH_RESULT, 
	
	/** SYNC event. 
	 * This event is recorded before entering 
	 * synchronized(object){...}.  
	 * The object is recorded with this event.
	 */
	MONITOR_ENTER, 
	
	/** SYNC event. 
	 * This event is recorded after entering 
	 * synchronized(object){...}.  
	 * The same object as MONITOR_ENTER event is 
	 * recorded with this event.
	 */
	MONITOR_ENTER_RESULT,
	
	/** SYNC event.  
	 * This event is recorded before leaving 
	 * synchronized(object){...} block.  
	 * The object is recorded with this event. 
	 */
	MONITOR_EXIT,  
	
	/** OBJECT event.  
	 * This event is recorded when a constant object 
	 * (usually String) is loaded by an instruction. 
	 * The object is recorded with this event.
	 */
	OBJECT_CONSTANT_LOAD, 
	
	/** OBJECT event. 
	 * This event is recorded before "instanceof" is executed.
	 * The object is recorded with this event. 
	 */
	OBJECT_INSTANCEOF, 
	
	/** OBJECT event.  
	 * This event is recorded after "instanceof" is executed.
	 * A boolean value representing the instruction result 
	 * is recorded with this event. 
	 */
	OBJECT_INSTANCEOF_RESULT,
	
	/** CALL event. 
	 * This event is recorded before INVOKEDYNAMIC instruction 
	 * creates a function object.  
	 * The example of event sequence related to INVOKEDYNAMIC 
	 * is found in selogger.weaver.WeaverTest#testInvokeDynamic().
	 */
	INVOKE_DYNAMIC,
	
	/** CALL event. 
	 * This event is recorded after INVOKE_DYNAMIC event.
	 * A parameter for a function object is recorded with this event.  
	 */
	INVOKE_DYNAMIC_PARAM,
	
	/** CALL event. 
	 * This event is recorded after INVOKEDYNAMIC instruction.
	 * A created function object is recorded with this event. 
	 */
	INVOKE_DYNAMIC_RESULT,
	
	/** LABEL event. 
	 * This event is recorded when an execution passed 
	 * a particular code location.  
	 * A dataid corresponding to a previous location 
	 * is recorded with this event so that a user can 
	 * trace a control-flow path. 
	 */
	LABEL,
	
	/** LABEL event placeholder. 
	 * This event is not directly recorded.
	 * This dataid represents a jump instruction 
	 * so that a user can trace a control-flow path. */
	JUMP,
	
	/** LOCAL event. 
	 * This event is recorded after a local variable is loaded.
	 * The value is recorded with this event. 
	 */
	LOCAL_LOAD, 
	
	/** LOCAL event. 
	 * This event is recorded before a local variable is updated.
	 * The value is recorded with this event.
	 */
	LOCAL_STORE,
	
	/** LOCAL event. 
	 * This event is recorded after an increment instruction is executed.
	 * The updated value is recorded with this event.
	 */
	LOCAL_INCREMENT,
	
	/** LOCAL event.
	 * This event is recorded before a RET instruction 
	 * (returning from subroutine) is executed.
	 * The program counter value in a local variable is 
	 * recorded with this event. 
	 */
	RET,
	
	/**	LABEL event placeholder. 
	 * This event is not directly recorded. 
	 * This event represents an arithmetic division.  
	 * A LABEL event may refer to this type as a control-flow path 
	 * if divided-by-zero exception occurred. 
	 */
	DIVIDE
	;
	
}
