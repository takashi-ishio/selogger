package selogger.logging;

import java.lang.reflect.Array;

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
		if (array != null) {
			LogWriter.INSTANCE.writeArrayLength(array, Array.getLength(array), locationId);
		} else {
			LogWriter.INSTANCE.writeArrayLength(array, 0, locationId);
		}
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
		if (array instanceof byte[]) {
			byte[] bytearray = (byte[])array;
			if (bytearray != null && 0 <= index && index < bytearray.length) {
				LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_LOAD_BYTE, array, index, bytearray[index], locationId);
			} else {
				LogWriter.INSTANCE.writeArrayLoad(array, index, locationId);
			}
		} else {
			boolean[] booleanarray = (boolean[])array;
			if (booleanarray != null && 0 <= index && index < booleanarray.length) {
				LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_LOAD_BOOLEAN, array, index, booleanarray[index], locationId);
			} else {
				LogWriter.INSTANCE.writeArrayLoad(array, index, locationId);
			}
		}
	}
	public static void recordArrayLoad(char[] array, int index, long locationId) {
		if (array != null && 0 <= index && index < array.length) {
			LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_LOAD_CHAR, array, index, array[index], locationId);
		} else {
			LogWriter.INSTANCE.writeArrayLoad(array, index, locationId);
		}
	}
	public static void recordArrayLoad(double[] array, int index, long locationId) {
		if (array != null && 0 <= index && index < array.length) {
			LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_LOAD_DOUBLE, array, index, array[index], locationId);
		} else {
			LogWriter.INSTANCE.writeArrayLoad(array, index, locationId);
		}
	}
	public static void recordArrayLoad(float[] array, int index, long locationId) {
		if (array != null && 0 <= index && index < array.length) {
			LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_LOAD_FLOAT, array, index, array[index], locationId);
		} else {
			LogWriter.INSTANCE.writeArrayLoad(array, index, locationId);
		}
	}
	public static void recordArrayLoad(int[] array, int index, long locationId) {
		if (array != null && 0 <= index && index < array.length) {
			LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_LOAD_INT, array, index, array[index], locationId);
		} else {
			LogWriter.INSTANCE.writeArrayLoad(array, index, locationId);
		}
	}
	public static void recordArrayLoad(long[] array, int index, long locationId) {
		if (array != null && 0 <= index && index < array.length) {
			LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_LOAD_LONG, array, index, array[index], locationId);
		} else {
			LogWriter.INSTANCE.writeArrayLoad(array, index, locationId);
		}
	}
	public static void recordArrayLoad(short[] array, int index, long locationId) {
		if (array != null && 0 <= index && index < array.length) {
			LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_LOAD_SHORT, array, index, array[index], locationId);
		} else {
			LogWriter.INSTANCE.writeArrayLoad(array, index, locationId);
		}
	}
	public static void recordArrayLoad(Object[] array, int index, long locationId) {
		if (array != null && 0 <= index && index < array.length) {
			LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_LOAD_OBJECT, array, index, array[index], locationId);
		} else {
			LogWriter.INSTANCE.writeArrayLoad(array, index, locationId);
		}
	}
	
	
	public static void recordArrayStore(Object array, int index, byte value, long locationId) {
		int eventType = (array instanceof byte[]) ? EventId.EVENT_ARRAY_STORE_BYTE: EventId.EVENT_ARRAY_STORE_BOOLEAN; 
		LogWriter.INSTANCE.writeArrayAccess(eventType, array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, char value, long locationId) {
		LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_STORE_CHAR, array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, double value, long locationId) {
		LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_STORE_DOUBLE, array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, float value, long locationId) {
		LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_STORE_FLOAT, array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, int value, long locationId) {
		LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_STORE_INT, array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, long value, long locationId) {
		LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_STORE_LONG, array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, short value, long locationId) {
		LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_STORE_SHORT, array, index, value, locationId);
	}
	public static void recordArrayStore(Object array, int index, Object value, long locationId) {
		LogWriter.INSTANCE.writeArrayAccess(EventId.EVENT_ARRAY_STORE_OBJECT, array, index, value, locationId);
	}

	public static void recordLabel(long locationId) {
		LogWriter.INSTANCE.writeEventWithoutValue(EventId.EVENT_LABEL, locationId);
	}

	public static void recordMethodEntry(long locationId) {
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
	
	public static void recordGetInstanceFieldTarget(Object o, long locationId) {
		if (o == null) {
			LogWriter.INSTANCE.writeGetInstanceFieldFail(locationId);
		}
	}

	public static void recordGetInstanceField(Object target, Object v, long locationId) {
		LogWriter.INSTANCE.writeGetInstanceFieldValue(target, v, locationId);
	}

	public static void recordGetInstanceField(Object target, boolean v, long locationId) {
		LogWriter.INSTANCE.writeGetInstanceFieldValue(target, v, locationId);
	}

	public static void recordGetInstanceField(Object target, byte v, long locationId) {
		LogWriter.INSTANCE.writeGetInstanceFieldValue(target, v, locationId);
	}

	public static void recordGetInstanceField(Object target, char v, long locationId) {
		LogWriter.INSTANCE.writeGetInstanceFieldValue(target, v, locationId);
	}

	public static void recordGetInstanceField(Object target, double v, long locationId) {
		LogWriter.INSTANCE.writeGetInstanceFieldValue(target, v, locationId);
	}

	public static void recordGetInstanceField(Object target, float v, long locationId) {
		LogWriter.INSTANCE.writeGetInstanceFieldValue(target, v, locationId);
	}

	public static void recordGetInstanceField(Object target, int v, long locationId) {
		LogWriter.INSTANCE.writeGetInstanceFieldValue(target, v, locationId);
	}

	public static void recordGetInstanceField(Object target, long v, long locationId) {
		LogWriter.INSTANCE.writeGetInstanceFieldValue(target, v, locationId);
	}

	public static void recordGetInstanceField(Object target, short v, long locationId) {
		LogWriter.INSTANCE.writeGetInstanceFieldValue(target, v, locationId);
	}

	public static void recordGetStaticField(boolean v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_STATIC_FIELD_BOOLEAN, v, locationId);
	}

	public static void recordGetStaticField(byte v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_STATIC_FIELD_BYTE, v, locationId);
	}
	
	public static void recordGetStaticField(char v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_STATIC_FIELD_CHAR, v, locationId);
	}

	public static void recordGetStaticField(double v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_STATIC_FIELD_DOUBLE, v, locationId);
	}

	public static void recordGetStaticField(float v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_STATIC_FIELD_FLOAT, v, locationId);
	}
	
	public static void recordGetStaticField(int v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_STATIC_FIELD_INT, v, locationId);
	}
	
	public static void recordGetStaticField(long v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_STATIC_FIELD_LONG, v, locationId);
	}
	
	public static void recordGetStaticField(short v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_STATIC_FIELD_SHORT, v, locationId);
	}

	public static void recordGetStaticField(Object v, long locationId) {
		LogWriter.INSTANCE.writeEventWithValue(EventId.EVENT_GET_STATIC_FIELD_OBJECT, v, locationId);
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
