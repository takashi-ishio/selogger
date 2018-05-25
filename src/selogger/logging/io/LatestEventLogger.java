package selogger.logging.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;


import selogger.logging.IEventLogger;

public class LatestEventLogger implements IEventLogger {

	protected class Buffer {

		private int bufferSize;
		private int nextPos = 0;
		private int count = 0;
		private Object array;

		public Buffer(Class<?> type, int bufferSize) {
			this.bufferSize = bufferSize;
			array = Array.newInstance(type, bufferSize);
		}
		
		private int getNextIndex() {
			count++;
			int next = nextPos++;
			if (nextPos >= bufferSize) {
				nextPos = 0;
			}
			return next;
		}
		
		public synchronized void addBoolean(boolean value) {
			((boolean[])array)[getNextIndex()] = value;
		}

		public synchronized void addByte(byte value) {
			((byte[])array)[getNextIndex()] = value;
		}

		public synchronized void addChar(char value) {
			((char[])array)[getNextIndex()] = value;
		}

		public synchronized void addInt(int value) {
			((int[])array)[getNextIndex()] = value;
		}

		public synchronized void addDouble(double value) {
			((double[])array)[getNextIndex()] = value;
		}

		public synchronized void addFloat(float value) {
			((float[])array)[getNextIndex()] = value;
		}
		
		public synchronized void addLong(long value) {
			((long[])array)[getNextIndex()] = value;
		}
		
		public synchronized void addShort(short value) {
			((short[])array)[getNextIndex()] = value;
		}

		public synchronized void addObject(Object value) {
			int index = getNextIndex();
			if (keepObject) {
				((Object[])array)[index] = value;
			} else {
				if (value != null) {
					WeakReference<?> ref = new WeakReference<>(value);
					((Object[])array)[index] = ref;
				} else {
					((Object[])array)[index] = null;
				}
			}
		}
		
		@Override
		public synchronized String toString() {
			StringBuilder buf = new StringBuilder();
			int len = Math.min(count, bufferSize);
			for (int i=0; i<len; i++) {
				if (i>0) buf.append(",");
				int idx = (count >= bufferSize) ? (nextPos + i) % bufferSize : i;
				if (array instanceof int[]) {
					buf.append(((int[])array)[idx]);
				} else if (array instanceof long[]) {
					buf.append(((long[])array)[idx]);
				} else if (array instanceof float[]) {
					buf.append(((float[])array)[idx]);
				} else if (array instanceof double[]) {
					buf.append(((double[])array)[idx]);
				} else if (array instanceof char[]) {
					buf.append((int)((char[])array)[idx]);
				} else if (array instanceof short[]) {
					buf.append(((short[])array)[idx]);
				} else if (array instanceof byte[]) {
					buf.append(((byte[])array)[idx]);
				} else if (array instanceof boolean[]) {
					buf.append(((boolean[])array)[idx]);
				} else {
					if (keepObject) {
						Object o = ((Object[])array)[idx];
						if (o == null) {
							buf.append("null");
						} else {
							String id = o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
							if (o instanceof String) {
								buf.append(id + ":" + escape((String)o));
							} else {
								buf.append(id);
							}
						}
					} else {
						WeakReference<?> ref = (WeakReference<?>)((Object[])array)[idx];
						if (ref == null) {
							buf.append("null");
						} else if (ref.get() == null) {
							buf.append("<GC>");
						} else {
							Object o = ref.get();
							String id = o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
							if (o instanceof String) {
								buf.append(id + ":" + escape((String)o));
							} else {
								buf.append(id);
							}
						}
					}
				}
			}
			return buf.toString();
		}
		
		public String escape(String original) {
			StringBuilder buf = new StringBuilder(original.length());
			buf.append('"');
			for (int i=0; i<original.length(); i++) {
				int c = Character.codePointAt(original, i);
				if (c == '\"') {
					buf.append("\"");
				} else if (c >= 32) {
					buf.appendCodePoint(c);
				} else {
					buf.append(String.format("\\u%04x", (int)c));
				}
			}
			buf.append('"');
			return buf.toString();
		}
		
		public synchronized int count() {
			return count;
		}

		public synchronized int size() {
			return Math.min(count, bufferSize); 
		}

	}
	

	
	private int bufferSize;
	private ArrayList<Buffer> buffers;
	private File outputDir;
	private boolean keepObject;
	
	public LatestEventLogger(File outputDir, int bufferSize, boolean keepObject) {
		this.outputDir = outputDir;
		this.bufferSize = bufferSize;
		buffers = new ArrayList<>();
		this.keepObject = keepObject;
	}

	@Override
	public synchronized void close() {
		try (PrintWriter w = new PrintWriter(new FileWriter(new File(outputDir, "recentdata.txt")))) {
			for (int i=0; i<buffers.size(); i++) {
				Buffer b = buffers.get(i);
				if (b != null) {
					w.println(i + "," + b.count() + "," + b.size() + "," + b.toString());
				}
			}
		} catch (IOException e) {
		}

	}
	
	protected synchronized Buffer prepareBuffer(Class<?> type, int dataId) {
		while (buffers.size() <= dataId) {
			buffers.add(null);
		}
		Buffer b = buffers.get(dataId);
		if (b == null) {
			b = new Buffer(type, bufferSize);
			buffers.set(dataId, b);
		}
		return b;
	}

	@Override
	public void recordEvent(int dataId, boolean value) {
		Buffer b = prepareBuffer(boolean.class, dataId);
		b.addBoolean(value);
	}
	
	@Override
	public void recordEvent(int dataId, byte value) {
		Buffer b = prepareBuffer(byte.class, dataId);
		b.addByte(value);
	}
	
	@Override
	public void recordEvent(int dataId, char value) {
		Buffer b = prepareBuffer(char.class, dataId);
		b.addChar(value);
	}
	
	@Override
	public void recordEvent(int dataId, double value) {
		Buffer b = prepareBuffer(double.class, dataId);
		b.addDouble(value);
	}
	
	@Override
	public void recordEvent(int dataId, float value) {
		Buffer b = prepareBuffer(float.class, dataId);
		b.addFloat(value);
	}
	
	@Override
	public void recordEvent(int dataId, int value) {
		Buffer b = prepareBuffer(int.class, dataId);
		b.addInt(value);
	}
	
	@Override
	public void recordEvent(int dataId, long value) {
		Buffer b = prepareBuffer(long.class, dataId);
		b.addLong(value);
	}
	
	@Override
	public void recordEvent(int dataId, Object value) {
		Buffer b = prepareBuffer(Object.class, dataId);
		b.addObject(value);
	}
	
	@Override
	public void recordEvent(int dataId, short value) {
		Buffer b = prepareBuffer(short.class, dataId);
		b.addShort(value);
	}
}
