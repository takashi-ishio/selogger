package selogger.logging.io;

import java.util.ArrayList;

import selogger.logging.IEventLogger;

/**
 * A logger records all events on memory for testing selogger features.
 */
public class MemoryLogger implements IEventLogger {

	public static class Event {
		private int dataId;
		private Class<?> valueType;
		private Object objectValue;
		private long value;
		
		public Event(int dataId, Class<?> type, long value) {
			this.dataId = dataId;
			this.valueType = type;
			this.value = value;
		}

		public Event(int dataId, Class<?> type, Object value) {
			this.dataId = dataId;
			this.valueType = type;
			this.objectValue = value;
		}
		
		public int getDataId() {
			return dataId;
		}
		
		public Class<?> getValueType() {
			return valueType;
		}
		
		public boolean getBooleanValue() {
			assert valueType.equals(boolean.class);
			return value != 0;
		}
		
		public byte getByteValue() {
			assert valueType.equals(byte.class);
			return (byte)value;
		}
		
		public char getCharValue() {
			assert valueType.equals(char.class);
			return (char)value;
		}
		
		public short getShortValue() {
			assert valueType.equals(short.class);
			return (short)value;
		}
		
		public int getIntValue() {
			assert valueType.equals(int.class);
			return (int)value;
		}
		
		public long getLongValue() {
			assert valueType.equals(long.class);
			return (long)value;
		}
		
		public float getFloatValue() {
			assert valueType.equals(float.class);
			return Float.intBitsToFloat((int)value);
		}

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
	
	public MemoryLogger() {
		events = new ArrayList<>();
	}
	
	public ArrayList<Event> getEvents() {
		return events;
	}
	
	@Override
	public void close() {
	}
	
	@Override
	public void recordEvent(int dataId, boolean value) {
		events.add(new Event(dataId, boolean.class, value? 1: 0));
	}
	
	@Override
	public void recordEvent(int dataId, byte value) {
		events.add(new Event(dataId, byte.class, value));
	}
	
	@Override
	public void recordEvent(int dataId, char value) {
		events.add(new Event(dataId, char.class, value));
	}
	
	public void recordEvent(int dataId, double value) {
		events.add(new Event(dataId, double.class, Double.doubleToLongBits(value)));
	}

	@Override
	public void recordEvent(int dataId, float value) {
		events.add(new Event(dataId, float.class, Float.floatToIntBits(value)));
	}
	
	@Override
	public void recordEvent(int dataId, int value) {
		events.add(new Event(dataId, int.class, value));
	}

	@Override
	public void recordEvent(int dataId, long value) {
		events.add(new Event(dataId, long.class, value));
	}
	
	@Override
	public void recordEvent(int dataId, Object value) {
		if (value == null) {
			events.add(new Event(dataId, Object.class, value));
		} else {
			events.add(new Event(dataId, value.getClass(), value));
		}
	}
	
	@Override
	public void recordEvent(int dataId, short value) {
		events.add(new Event(dataId, short.class, value));
	}
}
