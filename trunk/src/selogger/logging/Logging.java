package selogger.logging;

import selogger.EventId;


public class Logging {
	
	 
	/**
	 * Constructor call is finished.
	 * @param o
	 * @param locationId
	 */
	public static void recordObjectCreated(Object o, long locationId) {
		LogWriter.INSTANCE.writeObjectEvent(EventId.EVENT_OBJECT_CREATION_COMPLETED, o, locationId);
	}
	
	public static void recordObjectInitialized(Object o, long locationId) {
		LogWriter.INSTANCE.writeObjectEvent(EventId.EVENT_OBJECT_INITIALIZED, o, locationId);
	}
	
	public static void recordNewArray(int size, Object array, long locationId) {
		LogWriter.INSTANCE.writeNewArray(array, size, locationId);
	}

	public static void recordMultiNewArray(Object array, long locationId) {
		LogWriter.INSTANCE.writeMultiNewArray(array, locationId);
	}

	public static void recordArrayLength(Object array, long locationId) {
		LogWriter.INSTANCE.writeObjectEvent(EventId.EVENT_ARRAY_LENGTH, array, locationId);
	}

	public static void recordArrayLengthResult(int arraysize, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_ARRAY_LENGTH_RESULT, arraysize, locationId);
	}

	public static void recordInstanceOf(Object o, boolean result, long locationId) {
		LogWriter.INSTANCE.writeInstanceOf(o, result, locationId);
	}

	public static void recordCall(long locationId) {
		LogWriter.INSTANCE.writeEventWithoutValue(EventId.EVENT_METHOD_CALL, locationId);
	}
	
	public static void recordCatch(Object throwable, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_CATCH, throwable, locationId);
	}

	public static void recordMonitorEnter(Object monitor, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_MONITOR_ENTER, monitor, locationId);
	}

	public static void recordMonitorExit(Object monitor, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_MONITOR_EXIT, monitor, locationId);
	}

	public static void recordFormalParam(Object v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_FORMAL_PARAM_OBJECT, paramIndex, v, locationId);
	}
	public static void recordFormalParam(byte v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_FORMAL_PARAM_BYTE, paramIndex, v, locationId);
	}
	public static void recordFormalParam(char v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_FORMAL_PARAM_CHAR, paramIndex, v, locationId);
	}
	public static void recordFormalParam(double v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_FORMAL_PARAM_DOUBLE, paramIndex, v, locationId);
	}
	public static void recordFormalParam(float v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_FORMAL_PARAM_FLOAT, paramIndex, v, locationId);
	}
	public static void recordFormalParam(int v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_FORMAL_PARAM_INT, paramIndex, v, locationId);
	}
	public static void recordFormalParam(long v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_FORMAL_PARAM_LONG, paramIndex, v, locationId);
	}
	public static void recordFormalParam(short v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_FORMAL_PARAM_SHORT, paramIndex, v, locationId);
	}
	public static void recordFormalParam(boolean v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_FORMAL_PARAM_BOOLEAN, paramIndex, v, locationId);
	}

	
	
	public static void recordActualParam(Object v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_ACTUAL_PARAM_OBJECT, paramIndex, v, locationId);
	}
	public static void recordActualParam(byte v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_ACTUAL_PARAM_BYTE, paramIndex, v, locationId);
	}
	public static void recordActualParam(char v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_ACTUAL_PARAM_CHAR, paramIndex, v, locationId);
	}
	public static void recordActualParam(double v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_ACTUAL_PARAM_DOUBLE, paramIndex, v, locationId);
	}
	public static void recordActualParam(float v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_ACTUAL_PARAM_FLOAT, paramIndex, v, locationId);
	}
	public static void recordActualParam(int v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_ACTUAL_PARAM_INT, paramIndex, v, locationId);
	}
	public static void recordActualParam(long v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_ACTUAL_PARAM_LONG, paramIndex, v, locationId);
	}
	public static void recordActualParam(short v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_ACTUAL_PARAM_SHORT, paramIndex, v, locationId);
	}
	public static void recordActualParam(boolean v, int paramIndex, long locationId) {
		LogWriter.INSTANCE.writeMethodParameter(EventId.EVENT_ACTUAL_PARAM_BOOLEAN, paramIndex, v, locationId);
	}

	public static void recordConstantLoad(Object loaded, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_CONSTANT_OBJECT_LOAD, loaded, locationId);
	}

	public static void recordArrayLoad(Object array, int index, long locationId) {
		LogWriter.INSTANCE.writeArrayLoad(array, index, locationId);
	}
	
	/**
	 * @return a boolean value that indicates the array is byte[], not a boolean[].
	 */
	public static boolean recordByteArrayLoad(Object array, int index, long locationId) {
		LogWriter.INSTANCE.writeArrayLoad(array, index, locationId);
		return (array instanceof byte[]);
	}

	public static void recordArrayLoadResult(boolean isByte, byte value, long locationId) {
		if (isByte) {
			LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_ARRAY_LOAD_RESULT_BYTE, value, locationId);
		} else {
			LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_ARRAY_LOAD_RESULT_BOOLEAN, value, locationId);
		}
	}
	
	public static void recordArrayLoadResult(char value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_ARRAY_LOAD_RESULT_CHAR, value, locationId);
	}
	public static void recordArrayLoadResult(double value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_ARRAY_LOAD_RESULT_DOUBLE, value, locationId);
	}
	public static void recordArrayLoadResult(float value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_ARRAY_LOAD_RESULT_FLOAT, value, locationId);
	}
	public static void recordArrayLoadResult(int value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_ARRAY_LOAD_RESULT_INT, value, locationId);
	}
	public static void recordArrayLoadResult(long value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_ARRAY_LOAD_RESULT_LONG, value, locationId);
	}
	public static void recordArrayLoadResult(short value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_ARRAY_LOAD_RESULT_SHORT, value, locationId);
	}
	public static void recordArrayLoadResult(Object value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_ARRAY_LOAD_RESULT_OBJECT, value, locationId);
	}
	
	
	public static void recordArrayStore(Object array, int index, byte value, long locationId) {
		LogWriter.INSTANCE.writeArrayStoreValue(array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, char value, long locationId) {
		LogWriter.INSTANCE.writeArrayStoreValue(array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, double value, long locationId) {
		LogWriter.INSTANCE.writeArrayStoreValue(array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, float value, long locationId) {
		LogWriter.INSTANCE.writeArrayStoreValue(array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, int value, long locationId) {
		LogWriter.INSTANCE.writeArrayStoreValue(array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, long value, long locationId) {
		LogWriter.INSTANCE.writeArrayStoreValue(array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, short value, long locationId) {
		LogWriter.INSTANCE.writeArrayStoreValue(array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, Object value, long locationId) {
		LogWriter.INSTANCE.writeArrayStoreValue(array, index, value, locationId);
	}

	public static void recordLabel(long locationId) {
		LogWriter.INSTANCE.writeEventWithoutValue(EventId.EVENT_LABEL, locationId);
	}

	public static void recordBeginExec(long locationId) {
		LogWriter.INSTANCE.writeEventWithoutValue(EventId.EVENT_METHOD_ENTRY, locationId);
	}

	public static void recordThrowStatement(Object throwable, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_THROW, throwable, locationId);
	}
	
	public static void recordExceptionalExit(Object o, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_METHOD_EXCEPTIONAL_EXIT, o, locationId);
	}
	
	public static void recordNormalExit(long locationId) {
		LogWriter.INSTANCE.writeEventWithValueVoid(EventId.EVENT_METHOD_NORMAL_EXIT_VOID, locationId);
	}
	
	public static void recordNormalExit(Object o, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_METHOD_NORMAL_EXIT_OBJECT, o, locationId);
	}
	
	public static void recordNormalExit(byte v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_METHOD_NORMAL_EXIT_BYTE, v, locationId);
	}
	
	public static void recordNormalExit(char v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_METHOD_NORMAL_EXIT_CHAR, v, locationId);
	}
	
	public static void recordNormalExit(double v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_METHOD_NORMAL_EXIT_DOUBLE, v, locationId);
	}
	
	public static void recordNormalExit(float v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_METHOD_NORMAL_EXIT_FLOAT, v, locationId);
	}
	
	public static void recordNormalExit(int v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_METHOD_NORMAL_EXIT_INT, v, locationId);
	}
	
	public static void recordNormalExit(long v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_METHOD_NORMAL_EXIT_LONG, v, locationId);
	}
	
	public static void recordNormalExit(short v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_METHOD_NORMAL_EXIT_SHORT, v, locationId);
	}
	
	public static void recordNormalExit(boolean v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_METHOD_NORMAL_EXIT_BOOLEAN, v, locationId);
	}
	
	public static void recordReturnValueAfterCall(long locationId) {
		LogWriter.INSTANCE.writeEventWithValueVoid(EventId.EVENT_RETURN_VALUE_AFTER_CALL_VOID, locationId);
	}
	
	public static void recordReturnValueAfterCall(Object o, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_RETURN_VALUE_AFTER_CALL_OBJECT, o, locationId);
	}
	
	public static void recordReturnValueAfterCall(byte v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_RETURN_VALUE_AFTER_CALL_BYTE, v, locationId);
	}
	
	public static void recordReturnValueAfterCall(char v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_RETURN_VALUE_AFTER_CALL_CHAR, v, locationId);
	}
	
	public static void recordReturnValueAfterCall(double v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_RETURN_VALUE_AFTER_CALL_DOUBLE, v, locationId);
	}
	
	public static void recordReturnValueAfterCall(float v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_RETURN_VALUE_AFTER_CALL_FLOAT, v, locationId);
	}
	
	public static void recordReturnValueAfterCall(int v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_RETURN_VALUE_AFTER_CALL_INT, v, locationId);
	}
	
	public static void recordReturnValueAfterCall(long v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_RETURN_VALUE_AFTER_CALL_LONG, v, locationId);
	}
	public static void recordReturnValueAfterCall(short v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_RETURN_VALUE_AFTER_CALL_SHORT, v, locationId);
	}
	public static void recordReturnValueAfterCall(boolean v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_RETURN_VALUE_AFTER_CALL_BOOLEAN, v, locationId);
	}
	
	public static void recordGetStaticTarget(long locationId) {
		LogWriter.INSTANCE.writeEventWithoutValue(EventId.EVENT_GET_STATIC_FIELD, locationId);
	}
	
	public static void recordGetFieldTarget(Object o, long locationId) {
		LogWriter.INSTANCE.writeObjectEvent(EventId.EVENT_GET_INSTANCE_FIELD, o, locationId);
	}
	
	public static void recordGetFieldResult(Object o, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_FIELD_RESULT_OBJECT, o, locationId);
	}

	public static void recordGetFieldResult(byte v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_FIELD_RESULT_BYTE, v, locationId);
	}
	
	public static void recordGetFieldResult(char v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_FIELD_RESULT_CHAR, v, locationId);
	}
	
	public static void recordGetFieldResult(double v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_FIELD_RESULT_DOUBLE, v, locationId);
	}
	
	public static void recordGetFieldResult(float v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_FIELD_RESULT_FLOAT, v, locationId);
	}
	
	public static void recordGetFieldResult(int v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_FIELD_RESULT_INT, v, locationId);
	}
	
	public static void recordGetFieldResult(long v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_FIELD_RESULT_LONG, v, locationId);
	}
	
	public static void recordGetFieldResult(short v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_FIELD_RESULT_SHORT, v, locationId);
	}

	public static void recordGetFieldResult(boolean v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_FIELD_RESULT_BOOLEAN, v, locationId);
	}
	
	public static void recordPutStatic(Object o, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_STATIC_FIELD_OBJECT, o, locationId);
	}

	public static void recordPutStatic(byte v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_STATIC_FIELD_BYTE, v, locationId);
	}
	
	public static void recordPutStatic(char v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_STATIC_FIELD_CHAR, v, locationId);
	}
	
	public static void recordPutStatic(double v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_STATIC_FIELD_DOUBLE, v, locationId);
	}
	
	public static void recordPutStatic(float v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_STATIC_FIELD_FLOAT, v, locationId);
	}
	
	public static void recordPutStatic(int v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_STATIC_FIELD_INT, v, locationId);
	}
	
	public static void recordPutStatic(long v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_STATIC_FIELD_LONG, v, locationId);
	}
	
	public static void recordPutStatic(short v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_STATIC_FIELD_SHORT, v, locationId);
	}
	
	public static void recordPutStatic(boolean v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_STATIC_FIELD_BOOLEAN, v, locationId);
	}

	public static void recordPutField(Object target, Object value, long locationId) {
		LogWriter.INSTANCE.writePutInstanceFieldValue(target, value, locationId);
	}
	public static void recordPutField(Object target, byte value, long locationId) {
		LogWriter.INSTANCE.writePutInstanceFieldValue(target, value, locationId);
	}
	public static void recordPutField(Object target, char value, long locationId) {
		LogWriter.INSTANCE.writePutInstanceFieldValue(target, value, locationId);
	}
	public static void recordPutField(Object target, double value, long locationId) {
		LogWriter.INSTANCE.writePutInstanceFieldValue(target, value, locationId);
	}
	public static void recordPutField(Object target, float value, long locationId) {
		LogWriter.INSTANCE.writePutInstanceFieldValue(target, value, locationId);
	}
	public static void recordPutField(Object target, int value, long locationId) {
		LogWriter.INSTANCE.writePutInstanceFieldValue(target, value, locationId);
	}
	public static void recordPutField(Object target, long value, long locationId) {
		LogWriter.INSTANCE.writePutInstanceFieldValue(target, value, locationId);
	}
	public static void recordPutField(Object target, short value, long locationId) {
		LogWriter.INSTANCE.writePutInstanceFieldValue(target, value, locationId);
	}
	public static void recordPutField(Object target, boolean value, long locationId) {
		LogWriter.INSTANCE.writePutInstanceFieldValue(target, value, locationId);
	}

	public static void recordPutFieldBeforeInit(Object value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION_OBJECT, value, locationId);
	}

	public static void recordPutFieldBeforeInit(byte value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION_BYTE, value, locationId);
	}
	
	public static void recordPutFieldBeforeInit(char value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION_CHAR, value, locationId);
	}
	
	public static void recordPutFieldBeforeInit(double value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION_DOUBLE, value, locationId);
	}
	
	public static void recordPutFieldBeforeInit(float value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION_FLOAT, value, locationId);
	}

	public static void recordPutFieldBeforeInit(int value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION_INT, value, locationId);
	}
	
	public static void recordPutFieldBeforeInit(long value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION_LONG, value, locationId);
	}
	
	public static void recordPutFieldBeforeInit(short value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION_SHORT, value, locationId);
	}
	
	public static void recordPutFieldBeforeInit(boolean value, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION_BOOLEAN, value, locationId);
	}

}
