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

	private static class Buffer {
		private int start = 0;
		private int end = 0;
		private int count = 0;
		private Object array;

		public Buffer(Class<?> type, int bufferSize) {
			array = Array.newInstance(type, bufferSize);
		}
		
		private int getNextIndex() {
			count++;
			int next = end++;
			if (end >= Array.getLength(array)) {
				end = 0;
			}
			if (end == start) {
				start++;
				if (start >= Array.getLength(array)) {
					start = 0;
				}
			}
			return next;
		}
		
		public void addBoolean(boolean value) {
			Array.setBoolean(array, getNextIndex(), value);
		}

		public void addByte(byte value) {
			Array.setByte(array, getNextIndex(), value);
		}

		public void addChar(char value) {
			Array.setChar(array, getNextIndex(), value);
		}

		public void addInt(int value) {
			Array.setInt(array, getNextIndex(), value);
		}

		public void addDouble(double value) {
			Array.setDouble(array, getNextIndex(), value);
		}

		public void addFloat(float value) {
			Array.setFloat(array, getNextIndex(), value);
		}
		
		public void addLong(long value) {
			Array.setLong(array, getNextIndex(), value);
		}
		
		public void addShort(short value) {
			Array.setShort(array, getNextIndex(), value);
		}

		public void addObject(Object value) {
			if (value != null) {
				WeakReference<?> ref = new WeakReference<>(value);
				Array.set(array, getNextIndex(), ref);
			} else {
				Array.set(array, getNextIndex(), null);
			}
		}
		
		@Override
		public String toString() {
			int len = Array.getLength(array);
			StringBuilder buf = new StringBuilder();
			if (array instanceof int[]) {
				for (int i=0; i<len; i++) {
					int idx = (start + i) % len;
					if (idx == end) break;
					if (i>0) buf.append(",");
					buf.append(((int[])array)[idx]);
				}
			} else if (array instanceof long[]) {
				for (int i=0; i<len; i++) {
					int idx = (start + i) % len;
					if (idx == end) break;
					if (i>0) buf.append(",");
					buf.append(((long[])array)[idx]);
				}
			} else if (array instanceof float[]) {
				for (int i=0; i<len; i++) {
					int idx = (start + i) % len;
					if (idx == end) break;
					if (i>0) buf.append(",");
					buf.append(((float[])array)[idx]);
				}
			} else if (array instanceof double[]) {
				for (int i=0; i<len; i++) {
					int idx = (start + i) % len;
					if (idx == end) break;
					if (i>0) buf.append(",");
					buf.append(((double[])array)[idx]);
				}
			} else if (array instanceof char[]) {
				for (int i=0; i<len; i++) {
					int idx = (start + i) % len;
					if (idx == end) break;
					if (i>0) buf.append(",");
					buf.append((int)((char[])array)[idx]);
				}
			} else if (array instanceof short[]) {
				for (int i=0; i<len; i++) {
					int idx = (start + i) % len;
					if (idx == end) break;
					if (i>0) buf.append(",");
					buf.append(((short[])array)[idx]);
				}
			} else if (array instanceof byte[]) {
				for (int i=0; i<len; i++) {
					int idx = (start + i) % len;
					if (idx == end) break;
					if (i>0) buf.append(",");
					buf.append(((byte[])array)[idx]);
				}
			} else if (array instanceof boolean[]) {
				for (int i=0; i<len; i++) {
					int idx = (start + i) % len;
					if (idx == end) break;
					if (i>0) buf.append(",");
					buf.append(((boolean[])array)[idx]);
				}
			} else {
				for (int i=0; i<len; i++) {
					int idx = (start + i) % len;
					if (idx == end) break;
					if (i>0) buf.append(",");
					WeakReference<?> ref = (WeakReference<?>)Array.get(array, idx);
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

		public int size() {
			if (start == end) return 0;
			else if (start < end) {
				return end - start;
			} else {
				return (Array.getLength(array) - start) + end; 
			}
		}


	}
	

	
	private int bufferSize;
	private ArrayList<Buffer> buffers;
	private File outputDir;
	
	public LatestEventLogger(File outputDir, int bufferSize) {
		this.outputDir = outputDir;
		this.bufferSize = bufferSize;
		buffers = new ArrayList<>();
	}

	@Override
	public synchronized void close() {
		try (PrintWriter w = new PrintWriter(new FileWriter(new File(outputDir, "recentdata.txt")))) {
			for (int i=0; i<buffers.size(); i++) {
				Buffer b = buffers.get(i);
				if (b != null) {
					w.println(i + "," + b.count + "," + b.size() + "," + b.toString());
				}
			}
		} catch (IOException e) {
		}

	}
	
	private Buffer prepareBuffer(Class<?> type, int dataId) {
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
	public synchronized void recordEvent(int dataId, boolean value) {
		Buffer b = prepareBuffer(boolean.class, dataId);
		b.addBoolean(value);
	}
	
	@Override
	public synchronized void recordEvent(int dataId, byte value) {
		Buffer b = prepareBuffer(byte.class, dataId);
		b.addByte(value);
	}
	
	@Override
	public synchronized void recordEvent(int dataId, char value) {
		Buffer b = prepareBuffer(char.class, dataId);
		b.addChar(value);
	}
	
	@Override
	public synchronized void recordEvent(int dataId, double value) {
		Buffer b = prepareBuffer(double.class, dataId);
		b.addDouble(value);
	}
	
	@Override
	public synchronized void recordEvent(int dataId, float value) {
		Buffer b = prepareBuffer(float.class, dataId);
		b.addFloat(value);
	}
	
	@Override
	public synchronized void recordEvent(int dataId, int value) {
		Buffer b = prepareBuffer(int.class, dataId);
		b.addInt(value);
	}
	
	@Override
	public synchronized void recordEvent(int dataId, long value) {
		Buffer b = prepareBuffer(long.class, dataId);
		b.addLong(value);
	}
	
	@Override
	public synchronized void recordEvent(int dataId, Object value) {
		Buffer b = prepareBuffer(WeakReference.class, dataId);
		b.addObject(value);
	}
	
	@Override
	public synchronized void recordEvent(int dataId, short value) {
		Buffer b = prepareBuffer(short.class, dataId);
		b.addShort(value);
	}
}
