package selogger.logging.io;

import java.util.ArrayList;

import selogger.logging.IEventLogger;

/**
 * A logger records all events on memory for testing SELogger features.
 */
public class MemoryLogger implements IEventLogger {

	/**
	 * An event object to keep a data ID and an observed value.
	 */
	public static class Event {
		
		private int dataId;
		private Class<?> valueType;
		private Object objectValue;
		private long value;
		
		/**
		 * Create an event instance using a primitive value.
		 * @param dataId
		 * @param type specifies the original type of the value.
		 * @param value 
		 */
		public Event(int dataId, Class<?> type, long value) {
			this.dataId = dataId;
			this.valueType = type;
			this.value = value;
		}

		/**
		 * Create an event instance using an Object value.
		 * @param dataId
		 * @param value
		 */
		public Event(int dataId, Object value) {
			this.dataId = dataId;
			if (value == null) {
				this.valueType = Object.class;
			} else {
				this.valueType = value.getClass();
			}
			this.objectValue = value;
		}
		
		/**
		 * @return data ID of the event.
		 */
		public int getDataId() {
			return dataId;
		}
		
		/**
		 * @return the data type of the recorded value.
		 */
		public Class<?> getValueType() {
			return valueType;
		}
		
		/**
		 * @return the data value as a boolean value.
		 */
		public boolean getBooleanValue() {
			assert valueType.equals(boolean.class);
			return value != 0;
		}
		
		/**
		 * @return the data value as a byte value.
		 */
		public byte getByteValue() {
			assert valueType.equals(byte.class);
			return (byte)value;
		}
		
		/**
		 * @return the data value as a char value.
		 */
		public char getCharValue() {
			assert valueType.equals(char.class);
			return (char)value;
		}
		
		/**
		 * @return the data value as a short value.
		 */
		public short getShortValue() {
			assert valueType.equals(short.class);
			return (short)value;
		}
		
		/**
		 * @return the data value as an int value.
		 */
		public int getIntValue() {
			assert valueType.equals(int.class);
			return (int)value;
		}
		
		/**
		 * @return the data value as a long value.
		 */
		public long getLongValue() {
			assert valueType.equals(long.class);
			return (long)value;
		}
		
		/**
		 * @return the data value as a float value.
		 */
		public float getFloatValue() {
			assert valueType.equals(float.class);
			return Float.intBitsToFloat((int)value);
		}

		/**
		 * @return the data value as a double value.
		 */
		public double getDoubleValue() {
			assert valueType.equals(double.class);
			return Double.longBitsToDouble(value);
		}

		public Object getObjectValue() {
			assert !valueType.isPrimitive();
			return objectValue;
		}
		
	}
	
	private ArrayList<Event> events;
	
	
	/**
	 * Create an instance of this logging class.
	 * This does not require any parameter.
	 */
	public MemoryLogger() {
		events = new ArrayList<>();
	}
	
	/**
	 * @return recorded events.
	 */
	public ArrayList<Event> getEvents() {
		return events;
	}
	
	/**
	 * Close the stream.  This method actually does nothing.
	 */
	@Override
	public void close() {
	}
	
	/**
	 * This method does nothing for the recorded trace. 
	 * @param resetTrace Discard the trace if this flag is true
	 */
	@Override
	public void save(boolean resetTrace) {
		if (resetTrace) {
			events = new ArrayList<>();
		}
	}

	/**
	 * Record an event on memory. 
	 */
	@Override
	public void recordEvent(int dataId, boolean value) {
		events.add(new Event(dataId, boolean.class, value? 1: 0));
	}
	
	/**
	 * Record an event on memory. 
	 */
	@Override
	public void recordEvent(int dataId, byte value) {
		events.add(new Event(dataId, byte.class, value));
	}
	
	/**
	 * Record an event on memory. 
	 */
	@Override
	public void recordEvent(int dataId, char value) {
		events.add(new Event(dataId, char.class, value));
	}
	
	/**
	 * Record an event on memory. 
	 */
	@Override
	public void recordEvent(int dataId, double value) {
		events.add(new Event(dataId, double.class, Double.doubleToLongBits(value)));
	}

	/**
	 * Record an event on memory. 
	 */
	@Override
	public void recordEvent(int dataId, float value) {
		events.add(new Event(dataId, float.class, Float.floatToIntBits(value)));
	}
	
	/**
	 * Record an event on memory. 
	 */
	@Override
	public void recordEvent(int dataId, int value) {
		events.add(new Event(dataId, int.class, value));
	}

	/**
	 * Record an event on memory. 
	 */
	@Override
	public void recordEvent(int dataId, long value) {
		events.add(new Event(dataId, long.class, value));
	}
	
	/**
	 * Record an event on memory. 
	 */
	@Override
	public void recordEvent(int dataId, Object value) {
		events.add(new Event(dataId, value));
	}
	
	/**
	 * Record an event on memory. 
	 */
	@Override
	public void recordEvent(int dataId, short value) {
		events.add(new Event(dataId, short.class, value));
	}
}
