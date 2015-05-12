package selogger.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import selogger.Config;
import selogger.EventId;

public class LogWriter {

	public static final String FILENAME_TYPEID = "LOG$Types.txt";
	public static final String FILENAME_THREADID = "LOG$Threads.txt";

	static LogWriter INSTANCE = new LogWriter();
	
	private static final AtomicInteger nextThreadId = new AtomicInteger(0);
	private static ThreadLocal<Integer> threadId = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return nextThreadId.getAndIncrement();
		}
	};
	
	private TypeIdMap typeToId;
	private ObjectIdFile objectIdMap;
	private boolean blockRecursive = false;
	private FileWriter writer;
	
	private IEventWriter buffer;

	private LogWriter() {
		blockRecursive = true;
		try {
			final Config config = new Config();
			try {
				if (config.getErrorLogFile() != null) {
					writer = new FileWriter(new File(config.getErrorLogFile()));
				}
			} catch (IOException e) {
				writer = null;
			}
			recordError(config.getConfigLoadError());
			
			SequentialFileName eventFileNames = new SequentialFileName(config.getOutputDir(), Config.OutputOption.FILENAME_EVENT_PREFIX, config.getOutputOption().getSuffix(), 5);
			
			if (config.getOutputOption().getFormat() == Config.OutputOption.Format.FixedRecord) {
				buffer = new FixedSizeEventStream(eventFileNames, config.getOutputOption(), config.getWriterThreadCount());
			} else if (config.getOutputOption().getFormat() != Config.OutputOption.Format.Profile) {
				buffer = new VariableSizeEventStream(eventFileNames, config.getOutputOption(), config.getWriterThreadCount());
			} else {
				buffer = new EventProfileBuffer(config.getOutputDir());
			}
			try {
				typeToId = new TypeIdMap();
				objectIdMap = new ObjectIdFile(config, typeToId);
			} catch (IOException e) {
				recordError("We cannot record runtime information: " + e.getLocalizedMessage());
			}
	
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					synchronized (INSTANCE) {
						try {
							buffer.close();
							objectIdMap.close();
							typeToId.save(new File(config.getOutputDir(), FILENAME_TYPEID));
							if (buffer.hasError()) {
								recordError(buffer.getErrorMessage());
							}
							FileWriter threads = new FileWriter(new File(config.getOutputDir(), FILENAME_THREADID));
							threads.write(Integer.toString(nextThreadId.get()));
							threads.close();
							if (writer != null) writer.close();
						} catch (Throwable e) {
						}
					}
				}
			}));
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}
		blockRecursive = false;
	}
	
	private void recordError(String message) {
		try {
			if (writer != null && message != null) {
				writer.write(message);
				writer.flush();
			}
		} catch (IOException e) {
		}
	}

	public synchronized long getObjectId(Object o) {
		return objectIdMap.getId(o);
	}
	
	public synchronized void writeNewArray(Object array, int size, long locationId) {
		if (blockRecursive) return;
		if (buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongInt(EventId.EVENT_NEW_ARRAY, threadId.get(), locationId, objectIdMap.getId(array), size);
		blockRecursive = false;
	}
	
	public synchronized void writeMultiNewArray(Object array, long locationId) {
		if (blockRecursive) return;
		if (buffer.hasError()) return;
		blockRecursive = true;
		int myThreadId = threadId.get();
		buffer.registerLong(EventId.EVENT_MULTI_NEW_ARRAY, myThreadId, locationId, objectIdMap.getId(array));
		assert (array != null) && (array instanceof Object[]): "A multi-dimensional array must be an array of objects.";
		writeMultiNewArrayContent((Object[])array, myThreadId, locationId);
		blockRecursive = false;
	}
	
	private void writeMultiNewArrayContent(Object[] baseArray, int myThreadId, long locationId) {
		LinkedList<Object[]> arrays = new LinkedList<Object[]>();
		arrays.addFirst(baseArray);
		while (!arrays.isEmpty()) {
			Object[] asArray = arrays.removeFirst();
			for (int index=0; index<asArray.length; ++index) {
				Object element = asArray[index];
				Class<?> elementType = element.getClass();
				if (element != null && elementType.isArray()) {
					buffer.registerLongIntValue(EventId.EVENT_MULTI_NEW_ARRAY_CONTENT, myThreadId, locationId, objectIdMap.getId(asArray), index, objectIdMap.getId(element));
					if (elementType.getComponentType().isArray()) {
						arrays.addLast((Object[])element);
					}
				}
			}
		}
	}
	
	public synchronized void writeObjectEvent(int eventType, Object target, long locationId) {
		if (blockRecursive) return;
		if (buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLong(eventType, threadId.get(), locationId, objectIdMap.getId(target));
		blockRecursive = false;
	}
	
	public synchronized void writeArrayLoad(Object array, int index, long locationId) {
		if (blockRecursive) return;
		if (buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongInt(EventId.EVENT_ARRAY_LOAD, threadId.get(), locationId, objectIdMap.getId(array), index);
		blockRecursive = false;
	}

	public synchronized void writeGetInstanceFieldFail(long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerEventWithoutData(EventId.EVENT_GET_INSTANCE_FIELD, threadId.get(),  locationId);
		blockRecursive = false;
	}

	public synchronized void writeGetInstanceFieldValue(Object target, byte value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_GET_INSTANCE_FIELD_BYTE, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writeGetInstanceFieldValue(Object target, char value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_GET_INSTANCE_FIELD_CHAR, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writeGetInstanceFieldValue(Object target, double value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_GET_INSTANCE_FIELD_DOUBLE, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writeGetInstanceFieldValue(Object target, float value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_GET_INSTANCE_FIELD_FLOAT, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writeGetInstanceFieldValue(Object target, int value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_GET_INSTANCE_FIELD_INT, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writeGetInstanceFieldValue(Object target, long value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_GET_INSTANCE_FIELD_LONG, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writeGetInstanceFieldValue(Object target, short value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_GET_INSTANCE_FIELD_SHORT, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writeGetInstanceFieldValue(Object target, boolean value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_GET_INSTANCE_FIELD_BOOLEAN, threadId.get(),  locationId, objectIdMap.getId(target), (value ? 1: 0));
		blockRecursive = false;
	}

	public synchronized void writeGetInstanceFieldValue(Object target, Object value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_GET_INSTANCE_FIELD_OBJECT, threadId.get(),  locationId, objectIdMap.getId(target), objectIdMap.getId(value));
		blockRecursive = false;
	}

	
	public synchronized void writePutInstanceFieldValue(Object target, byte value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_PUT_INSTANCE_FIELD_BYTE, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writePutInstanceFieldValue(Object target, char value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_PUT_INSTANCE_FIELD_CHAR, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writePutInstanceFieldValue(Object target, double value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_PUT_INSTANCE_FIELD_DOUBLE, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writePutInstanceFieldValue(Object target, float value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_PUT_INSTANCE_FIELD_FLOAT, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writePutInstanceFieldValue(Object target, int value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_PUT_INSTANCE_FIELD_INT, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writePutInstanceFieldValue(Object target, long value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_PUT_INSTANCE_FIELD_LONG, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writePutInstanceFieldValue(Object target, short value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_PUT_INSTANCE_FIELD_SHORT, threadId.get(),  locationId, objectIdMap.getId(target), value);
		blockRecursive = false;
	}

	public synchronized void writePutInstanceFieldValue(Object target, boolean value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_PUT_INSTANCE_FIELD_BOOLEAN, threadId.get(),  locationId, objectIdMap.getId(target), (value ? 1: 0));
		blockRecursive = false;
	}

	public synchronized void writePutInstanceFieldValue(Object target, Object value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_PUT_INSTANCE_FIELD_OBJECT, threadId.get(),  locationId, objectIdMap.getId(target), objectIdMap.getId(value));
		blockRecursive = false;
	}

	public synchronized void writeInstanceOf(Object target, boolean result, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_INSTANCEOF, threadId.get(), locationId, objectIdMap.getId(target), (result ? 1: 0));
		blockRecursive = false;
	}

	public synchronized void writeMethodParameter(int eventType, int index, byte value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerIntValue(eventType, threadId.get(),  locationId, index, value);
		blockRecursive = false;
	}

	public synchronized void writeMethodParameter(int eventType, int index, char value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerIntValue(eventType, threadId.get(),  locationId, index, value);
		blockRecursive = false;
	}
	public synchronized void writeMethodParameter(int eventType, int index, double value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerIntValue(eventType, threadId.get(),  locationId, index, value);
		blockRecursive = false;
	}
	public synchronized void writeMethodParameter(int eventType, int index, float value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerIntValue(eventType, threadId.get(),  locationId, index, value);
		blockRecursive = false;
	}
	public synchronized void writeMethodParameter(int eventType, int index, int value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerIntValue(eventType, threadId.get(),  locationId, index, value);
		blockRecursive = false;
	}
	public synchronized void writeMethodParameter(int eventType, int index, long value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerIntValue(eventType, threadId.get(),  locationId, index, value);
		blockRecursive = false;
	}
	public synchronized void writeMethodParameter(int eventType, int index, short value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerIntValue(eventType, threadId.get(),  locationId, index, value);
		blockRecursive = false;
	}
	public synchronized void writeMethodParameter(int eventType, int index, boolean value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerIntValue(eventType, threadId.get(),  locationId, index, (value ? 1: 0));
		blockRecursive = false;
	}
	public synchronized void writeMethodParameter(int eventType, int index, Object value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerIntValue(eventType, threadId.get(),  locationId, index, objectIdMap.getId(value));
		blockRecursive = false;
	}
	
	
	public synchronized void writeArrayAccess(int eventType, Object target, int index, byte value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongIntValue(eventType, threadId.get(),  locationId, objectIdMap.getId(target), index, value);
		blockRecursive = false;
	}

	public synchronized void writeArrayAccess(int eventType, Object target, int index, char value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongIntValue(eventType, threadId.get(),  locationId, objectIdMap.getId(target), index, value);
		blockRecursive = false;
	}
	
	public synchronized void writeArrayAccess(int eventType, Object target, int index, double value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongIntValue(eventType, threadId.get(),  locationId, objectIdMap.getId(target), index, value);
		blockRecursive = false;
	}
	
	public synchronized void writeArrayAccess(int eventType, Object target, int index, float value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongIntValue(eventType, threadId.get(),  locationId, objectIdMap.getId(target), index, value);
		blockRecursive = false;
	}
	
	public synchronized void writeArrayAccess(int eventType, Object target, int index, int value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongIntValue(eventType, threadId.get(),  locationId, objectIdMap.getId(target), index, value);
		blockRecursive = false;
	}
	
	public synchronized void writeArrayAccess(int eventType, Object target, int index, long value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongIntValue(eventType, threadId.get(),  locationId, objectIdMap.getId(target), index, value);
		blockRecursive = false;
	}
	
	public synchronized void writeArrayAccess(int eventType, Object target, int index, short value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongIntValue(eventType, threadId.get(),  locationId, objectIdMap.getId(target), index, value);
		blockRecursive = false;
	}
	
	public synchronized void writeArrayAccess(int eventType, Object target, int index, Object value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongIntValue(eventType, threadId.get(),  locationId, objectIdMap.getId(target), index, objectIdMap.getId(value));
		blockRecursive = false;
	}
	
	public synchronized void writeArrayLength(Object array, int length, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerLongValue(EventId.EVENT_ARRAY_LENGTH, threadId.get(),  locationId, objectIdMap.getId(array), length);
		blockRecursive = false;
	}


	public synchronized void writeEventWithValue(int eventType, byte value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerValue(eventType, threadId.get(),  locationId, value);
		blockRecursive = false;
	}

	public synchronized void writeEventWithValue(int eventType, char value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerValue(eventType, threadId.get(),  locationId, value);
		blockRecursive = false;
	}
	
	public synchronized void writeEventWithValue(int eventType, double value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerValue(eventType, threadId.get(),  locationId, value);
		blockRecursive = false;
	}
	
	public synchronized void writeEventWithValue(int eventType, float value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerValue(eventType, threadId.get(),  locationId, value);
		blockRecursive = false;
	}
	
	public synchronized void writeEventWithValue(int eventType, int value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerValue(eventType, threadId.get(),  locationId, value);
		blockRecursive = false;
	}
	
	public synchronized void writeEventWithValue(int eventType, long value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerValue(eventType, threadId.get(),  locationId, value);
		blockRecursive = false;
	}
	
	public synchronized void writeEventWithValue(int eventType, short value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerValue(eventType, threadId.get(),  locationId, value);
		blockRecursive = false;
	}

	public synchronized void writeEventWithValue(int eventType, boolean value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerValue(eventType, threadId.get(),  locationId, (value ? 1: 0));
		blockRecursive = false;
	}
	public synchronized void writeEventWithValue(int eventType, Object value, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerValue(eventType, threadId.get(),  locationId, objectIdMap.getId(value));
		blockRecursive = false;
	}

	public synchronized void writeEventWithValueVoid(int eventType, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerValueVoid(eventType, threadId.get(), locationId);
		blockRecursive = false;
	}
	
	
	public synchronized void writeEventWithoutValue(int eventType, long locationId) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerEventWithoutData(eventType, threadId.get(), locationId);
		blockRecursive = false;
	}

	public synchronized void writeParams(int eventType, long locationId, int types, int param1, int param2, int param3) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerParams(eventType, threadId.get(), locationId, types, param1, param2, param3);
		blockRecursive = false;
	}

	public synchronized void writeParams(int eventType, long locationId, int types, int param1, int param2, long param3) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerParams(eventType, threadId.get(), locationId, types, param1, param2, param3);
		blockRecursive = false;
	}

	public synchronized void writeParams(int eventType, long locationId, int types, int param1, long param2, int param3) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerParams(eventType, threadId.get(), locationId, types, param1, param2, param3);
		blockRecursive = false;
	}

	public synchronized void writeParams(int eventType, long locationId, int types, long param1, int param2, int param3) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerParams(eventType, threadId.get(), locationId, types, param1, param2, param3);
		blockRecursive = false;
	}

	public synchronized void writeParams(int eventType, long locationId, int types, long param1, long param2) {
		if (blockRecursive || buffer.hasError()) return;
		blockRecursive = true;
		buffer.registerParams(eventType, threadId.get(), locationId, types, param1, param2);
		blockRecursive = false;
	}


}
