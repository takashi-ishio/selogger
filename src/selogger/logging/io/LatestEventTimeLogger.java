package selogger.logging.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.io.JsonStringEncoder;

import selogger.logging.IEventLogger;

/**
 * This class is an implementation of IEventLogger that records
 * only the latest k events for each data ID.
 */
public class LatestEventTimeLogger implements IEventLogger {

	/**
	 * An object to assign an integer for each thread.
	 */
	private static final AtomicInteger nextThreadId = new AtomicInteger(0);

	/**
	 * This object keeps thread IDs for each thread.
	 */
	private static ThreadLocal<Integer> threadId = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return nextThreadId.getAndIncrement();
		}
	};

	/**
	 * A ring buffer to record the latest k events for a data ID.
	 */
	protected class Buffer {

		private int bufferSize;
		private int nextPos = 0;
		private int count = 0;
		private Object array;
		private long[] seqnums;
		private int[] threads;

		/**
		 * Create a buffer.
		 * @param type specifies a value type stored to the buffer.
		 * @param bufferSize specifies the size of this buffer.
		 */
		public Buffer(Class<?> type, int bufferSize) {
			this.bufferSize = bufferSize;
			array = Array.newInstance(type, bufferSize);
			seqnums = new long[bufferSize];
			threads = new int[bufferSize];
		}
		
		/**
		 * @return index to which the next value is written.   
		 */
		private int getNextIndex() {
			count++;
			int next = nextPos++;
			if (nextPos >= bufferSize) {
				nextPos = 0;
			}
			return next;
		}
		
		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public synchronized void addBoolean(boolean value) {
			int index = getNextIndex();
			((boolean[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}

		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public synchronized void addByte(byte value) {
			int index = getNextIndex();
			((byte[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}

		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public synchronized void addChar(char value) {
			int index = getNextIndex();
			((char[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}

		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public synchronized void addInt(int value) {
			int index = getNextIndex();
			((int[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}

		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public synchronized void addDouble(double value) {
			int index = getNextIndex();
			((double[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}

		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public synchronized void addFloat(float value) {
			int index = getNextIndex();
			((float[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}
		
		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public synchronized void addLong(long value) {
			int index = getNextIndex();
			((long[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}
		
		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public synchronized void addShort(short value) {
			int index = getNextIndex();
			((short[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}

		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 * If keepObject is true, this buffer directly stores the object reference.
		 * Otherwise, the buffer uses a weak reference to store the reference.
		 */
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
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}
		
		/**
		 * Generate a string representation that is written to a trace file.
		 * @return A line of CSV string.  The first column is the number of events recorded in the buffer.
		 * The other columns are the event data recorded in a trace.
		 * The oldest event is written first. 
		 * the latest one is written at last.
		 * For each event, the observed value, the sequence number, and the thread ID are written.
		 * In case of a string object, the content is written with the object ID.  
		 */
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
								buf.append(id);
								buf.append(":\"");
								JsonStringEncoder.getInstance().quoteAsString((String)o, buf);
								buf.append("\"");
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
								buf.append(id);
								buf.append(":\"");
								JsonStringEncoder.getInstance().quoteAsString((String)o, buf);
								buf.append("\"");
							} else {
								buf.append(id);
							}
						}
					}
				}
				buf.append(",");
				buf.append(seqnums[idx]);
				buf.append(",");
				buf.append(threads[idx]);
			}
			return buf.toString();
		}
		
		/**
		 * @return the number of event occurrences
		 */
		public synchronized int count() {
			return count;
		}

		/**
		 * @return the number of event data recorded in this buffer.
		 * The maximum value is the buffer size.
		 */
		public synchronized int size() {
			return Math.min(count, bufferSize); 
		}

	}
	
	private int bufferSize;
	private ArrayList<Buffer> buffers;
	private File outputDir;
	private boolean keepObject;
	
	/**
	 * This object generates a sequence number for each event.
	 * Each event has a sequence number from 1 representing 
	 * the order of event occurrence.  
	 */
	private static AtomicLong seqnum = new AtomicLong(0);
	
	/**
	 * Create an instance of this logger.
	 * @param outputDir specifies a directory for output files.
	 * @param bufferSize specifies the size of buffer ("k" in Near-Omniscient Debugging)
	 * @param keepObject If true, the buffers keep Java objects in order to avoid GC.  
	 * If false, objects in the buffer may be garbage collected. 
	 */
	public LatestEventTimeLogger(File outputDir, int bufferSize, boolean keepObject) {
		this.outputDir = outputDir;
		this.bufferSize = bufferSize;
		this.buffers = new ArrayList<>();
		this.keepObject = keepObject;
	}

	/**
	 * Close the logger and save the contents into a file naemd "recentdata.txt".
	 */
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
	
	/**
	 * This method creates a buffer for a particular data ID if such a buffer does not exist.
	 * @param type specifies a value type.
	 * @param dataId specifies the data ID.
	 * @return a buffer for the data ID.
	 */
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

	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, boolean value) {
		Buffer b = prepareBuffer(boolean.class, dataId);
		b.addBoolean(value);
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, byte value) {
		Buffer b = prepareBuffer(byte.class, dataId);
		b.addByte(value);
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, char value) {
		Buffer b = prepareBuffer(char.class, dataId);
		b.addChar(value);
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, double value) {
		Buffer b = prepareBuffer(double.class, dataId);
		b.addDouble(value);
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, float value) {
		Buffer b = prepareBuffer(float.class, dataId);
		b.addFloat(value);
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, int value) {
		Buffer b = prepareBuffer(int.class, dataId);
		b.addInt(value);
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, long value) {
		Buffer b = prepareBuffer(long.class, dataId);
		b.addLong(value);
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, Object value) {
		Buffer b = prepareBuffer(Object.class, dataId);
		b.addObject(value);
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, short value) {
		Buffer b = prepareBuffer(short.class, dataId);
		b.addShort(value);
	}
	

}
