package selogger.logging;

import java.util.LinkedList;



public class Logging {
	
	
	public static void recordEvent(Object value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, value);
	}

	/**
	 * This method is defined for type checking
	 * @param value
	 * @param dataId
	 */
	public static void recordEvent(Throwable value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, value);
	}

	public static void recordEvent(boolean value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, value);
	}

	public static void recordEvent(byte value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, value);
	}

	public static void recordEvent(char value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, value);
	}

	public static void recordEvent(short value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, value);
	}

	public static void recordEvent(int value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, value);
	}

	public static void recordEvent(long value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, value);
	}

	public static void recordEvent(float value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, value);
	}

	public static void recordEvent(double value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, value);
	}

	public static void recordEvent(int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, 0);
	}
	
	/**
	 * Method for byte[] and boolean[]
	 */
	public static void recordArrayLoad(Object array, int index, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		if (array instanceof byte[]) {
			byte[] bytearray = (byte[])array;
			if (bytearray != null && 0 <= index && index < bytearray.length) {
				EventLogger.INSTANCE.recordEvent(dataId+2, bytearray[index]);
			}
		} else {
			boolean[] booleanarray = (boolean[])array;
			if (booleanarray != null && 0 <= index && index < booleanarray.length) {
				EventLogger.INSTANCE.recordEvent(dataId+2, booleanarray[index]);
			}
		}
	}
	public static void recordArrayLoad(char[] array, int index, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		if (array != null && 0 <= index && index < array.length) {
			EventLogger.INSTANCE.recordEvent(dataId+2, array[index]);
		}
	}
	public static void recordArrayLoad(double[] array, int index, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		if (array != null && 0 <= index && index < array.length) {
			EventLogger.INSTANCE.recordEvent(dataId+2, array[index]);
		}
	}
	
	public static void recordArrayLoad(float[] array, int index, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		if (array != null && 0 <= index && index < array.length) {
			EventLogger.INSTANCE.recordEvent(dataId+2, array[index]);
		}
	}
	
	public static void recordArrayLoad(int[] array, int index, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		if (array != null && 0 <= index && index < array.length) {
			EventLogger.INSTANCE.recordEvent(dataId+2, array[index]);
		}
	}
	
	public static void recordArrayLoad(long[] array, int index, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		if (array != null && 0 <= index && index < array.length) {
			EventLogger.INSTANCE.recordEvent(dataId+2, array[index]);
		}
	}
	
	public static void recordArrayLoad(short[] array, int index, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		if (array != null && 0 <= index && index < array.length) {
			EventLogger.INSTANCE.recordEvent(dataId+2, array[index]);
		}
	}
	
	public static void recordArrayLoad(Object[] array, int index, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		if (array != null && 0 <= index && index < array.length) {
			EventLogger.INSTANCE.recordEvent(dataId+2, array[index]);
		}
	}

	public static void recordArrayStore(Object array, int index, byte value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		EventLogger.INSTANCE.recordEvent(dataId+2, value);
	}
	
	public static void recordArrayStore(Object array, int index, char value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		EventLogger.INSTANCE.recordEvent(dataId+2, value);
	}
	
	public static void recordArrayStore(Object array, int index, double value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		EventLogger.INSTANCE.recordEvent(dataId+2, value);
	}
	
	public static void recordArrayStore(Object array, int index, float value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		EventLogger.INSTANCE.recordEvent(dataId+2, value);
	}
	
	public static void recordArrayStore(Object array, int index, int value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		EventLogger.INSTANCE.recordEvent(dataId+2, value);
	}
	
	public static void recordArrayStore(Object array, int index, long value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		EventLogger.INSTANCE.recordEvent(dataId+2, value);
	}
	
	public static void recordArrayStore(Object array, int index, short value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		EventLogger.INSTANCE.recordEvent(dataId+2, value);
	}
	
	public static void recordArrayStore(Object array, int index, Object value, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		EventLogger.INSTANCE.recordEvent(dataId+1, index);
		EventLogger.INSTANCE.recordEvent(dataId+2, value);
	}

	public static void recordMultiNewArray(Object array, int dataId) {
		EventLogger.INSTANCE.recordEvent(dataId, array);
		recordMultiNewArrayContents((Object[])array, dataId);
	}

	private static void recordMultiNewArrayContents(Object[] array, int dataId) {
		LinkedList<Object[]> arrays = new LinkedList<Object[]>();
		arrays.addFirst(array);
		while (!arrays.isEmpty()) {
			Object[] asArray = arrays.removeFirst();
			EventLogger.INSTANCE.recordEvent(dataId+1, asArray);
			for (int index=0; index<asArray.length; ++index) {
				Object element = asArray[index];
				Class<?> elementType = element.getClass();
				if (element != null && elementType.isArray()) {
					EventLogger.INSTANCE.recordEvent(dataId+2, element);
					if (elementType.getComponentType().isArray()) {
						arrays.addLast((Object[])element);
					}
				}
			}
		}
	}
}
