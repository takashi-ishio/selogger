package selogger.logging.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.JsonStringEncoder;

import selogger.logging.IEventLogger;
import selogger.logging.util.ObjectIdFile;
import selogger.logging.util.TypeIdMap;
import selogger.logging.util.ObjectIdFile.ExceptionRecording;

/**
 * This class is an implementation of IEventLogger that records
 * only the latest k events for each data ID.
 */
public class LatestEventLogger implements IEventLogger {

	/**
	 * Enum object to specify how to record objects in an execution trace
	 */
	public enum ObjectRecordingStrategy {
		/**
		 * The buffers keep direct object references.
		 * This option keeps objects from GC.
		 */
		Strong,
		/**
		 * The buffers keep objects using WeakReference. 
		 * Objects in the buffer may be garbage-collected; 
		 * such garbage-collected objects are not recorded in an execution trace. 
		 */
		Weak,
		/**
		 * The buffers keep objects using Object ID.
		 */
		Id
	}

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

		private AtomicLong count = new AtomicLong();
		private int bufferSize;
		private Object array;
		private long[] seqnums;
		private int[] threads;
		private String typename;

		/**
		 * Create a buffer.
		 * @param type specifies a value type stored to the buffer.
		 * @param bufferSize specifies the size of this buffer.
		 */
		public Buffer(Class<?> type, String typename, int bufferSize) {
			this.bufferSize = bufferSize;
			this.typename = typename;
			this.array = Array.newInstance(type, bufferSize);
			this.seqnums = new long[bufferSize];
			this.threads = new int[bufferSize];
		}
		
		/**
		 * @return index to which the next value is written.   
		 */
		private int getNextIndex() {
			long value = count.getAndIncrement();
			int next = (int)(value % bufferSize);
			return next;
		}
		
		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public void addBoolean(boolean value) {
			int index = getNextIndex();
			((boolean[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}

		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public void addByte(byte value) {
			int index = getNextIndex();
			((byte[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}

		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public void addChar(char value) {
			int index = getNextIndex();
			((char[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}

		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public void addInt(int value) {
			int index = getNextIndex();
			((int[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}

		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public void addDouble(double value) {
			int index = getNextIndex();
			((double[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}

		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public void addFloat(float value) {
			int index = getNextIndex();
			((float[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}
		
		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public void addLong(long value) {
			int index = getNextIndex();
			((long[])array)[index] = value;
			seqnums[index] = seqnum.getAndIncrement();
			threads[index] = threadId.get();
		}
		
		/**
		 * Write a value to the next position.
		 * If the buffer is already full, it overwrites the oldest one.
		 */
		public void addShort(short value) {
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
		public void addObject(Object value) {
			int index = getNextIndex();
			assert keepObject != ObjectRecordingStrategy.Id;
			if (keepObject == ObjectRecordingStrategy.Strong) {
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
		public String toString() {
			StringBuilder buf = new StringBuilder();
			int len = (int)Math.min(count.get(), bufferSize);
			for (int i=0; i<len; i++) {
				if (i>0) buf.append(",");
				int idx = getPos(i);

				// Write a value depending on a type
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
					String msg = "null";
					Object o = ((Object[])array)[idx];
					if (keepObject == ObjectRecordingStrategy.Weak) {
						WeakReference<?> ref = (WeakReference<?>)o;
						o = ref.get();
						if (o == null) {
							msg = "<GC>";
						}
					}
					if (o == null) {
						buf.append(msg);
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
		public long count() {
			return count.get();
		}

		/**
		 * @return the number of event data recorded in this buffer.
		 * The maximum value is the buffer size.
		 */
		public int size() {
			return (int)Math.min(count.get(), bufferSize); 
		}
		
		/**
		 * Calculate the i-th event data location in the buffer. 
		 * @param i specifies an event.  0 indicates the oldest event in the buffer.
		 * @return index for an array
		 */
		private int getPos(int i) {
			return (count.get() >= bufferSize) ? (int)((count.get() + i) % bufferSize) : i;
		}
		
		private void writeJson(JsonGenerator gen) throws IOException { 
			int len = (int)Math.min(count.get(), bufferSize);
			
			gen.writeStringField("type", typename);
			gen.writeArrayFieldStart("value");
			for (int i=0; i<len; i++) {
				int idx = getPos(i);
				// Write a value depending on a type
				if (array instanceof int[]) {
					gen.writeNumber(((int[])array)[idx]);
				} else if (array instanceof long[]) {
					gen.writeNumber(((long[])array)[idx]);
				} else if (array instanceof float[]) {
					gen.writeNumber(((float[])array)[idx]);
				} else if (array instanceof double[]) {
					gen.writeNumber(((double[])array)[idx]);
				} else if (array instanceof char[]) {
					gen.writeNumber((int)((char[])array)[idx]);
				} else if (array instanceof short[]) {
					gen.writeNumber(((short[])array)[idx]);
				} else if (array instanceof byte[]) {
					gen.writeNumber(((byte[])array)[idx]);
				} else if (array instanceof boolean[]) {
					gen.writeBoolean(((boolean[])array)[idx]);
				} else {
					String id = "null";
					Object o = ((Object[])array)[idx];
					if (keepObject == ObjectRecordingStrategy.Weak) {
						WeakReference<?> ref = (WeakReference<?>)o;
						o = ref.get();
						if (o == null) {
							id = "<GC>";
						}
					}
					gen.writeStartObject();
					id = o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
					gen.writeStringField("id", id);
					if (o instanceof String) {
						gen.writeStringField("string", (String)o);
					}
					gen.writeEndObject();
				}
			}
			gen.writeEndArray();
			gen.writeArrayFieldStart("seqnum");
			for (int i=0; i<len; i++) {
				gen.writeNumber(seqnums[getPos(i)]);
			}
			gen.writeEndArray();
			gen.writeArrayFieldStart("thread");
			for (int i=0; i<len; i++) {
				gen.writeNumber(threads[getPos(i)]);
			}
			gen.writeEndArray();
		}

	}
	
	private int bufferSize;
	private ArrayList<Buffer> buffers;
	private File outputDir;
	private ObjectRecordingStrategy keepObject;
	private boolean outputJson;
	
	/**
	 * This object generates a sequence number for each event.
	 * Each event has a sequence number from 1 representing 
	 * the order of event occurrence.  
	 */
	private static AtomicLong seqnum = new AtomicLong(0);

	private TypeIdMap objectTypes;
	private ObjectIdFile objectIDs;
	
	private boolean closed = false;

	/**
	 * Create an instance of this logger.
	 * @param outputDir specifies a directory for output files.
	 * @param bufferSize specifies the size of buffer ("k" in Near-Omniscient Debugging)
	 * @param keepObject specifies how the buffers keep Java objects.  
	 * @param recordString specifies whether the logger records String contents or not.
	 * @param recordExceptions specifies whether the logger records Exception contents or not.
	 * @param outputJson specifies whether the logger uses a json format or not.
	 */
	public LatestEventLogger(File outputDir, int bufferSize, ObjectRecordingStrategy keepObject, boolean recordString, ExceptionRecording recordExceptions, boolean outputJson) {
		this.outputDir = outputDir;
		this.bufferSize = bufferSize;
		this.buffers = new ArrayList<>();
		this.keepObject = keepObject;
		this.outputJson = outputJson;
		if (this.keepObject == ObjectRecordingStrategy.Id) {
			objectTypes = new TypeIdMap();
			try {
				objectIDs = new ObjectIdFile(outputDir, recordString, recordExceptions, objectTypes);
			} catch (IOException e) {
				// Try to record objectIds using Weak 
				this.keepObject = ObjectRecordingStrategy.Weak;
				objectIDs = null;
				objectTypes = null;
			}
		}
	}

	/**
	 * Close the logger and save the contents into a file naemd "recentdata.txt".
	 */
	@Override
	public void close() {
		closed = true;
		if (objectTypes != null) {
			objectTypes.save(new File(outputDir, EventStreamLogger.FILENAME_TYPEID));
		}
		if (objectIDs != null) {
			objectIDs.close();
		}
		if (outputJson) {
			try (FileOutputStream w = new FileOutputStream(new File(outputDir, "recentdata.json"))) {
				JsonFactory factory = new JsonFactory();
				JsonGenerator gen = factory.createGenerator(w);
				gen.writeStartObject();
				gen.writeArrayFieldStart("events");
				for (int i=0; i<buffers.size(); i++) {
					Buffer b = buffers.get(i);
					if (b != null) {
						gen.writeStartObject();
						gen.writeNumberField("dataid", i);
						gen.writeNumberField("freq", b.count());
						gen.writeNumberField("record", b.size());
						b.writeJson(gen);
						gen.writeEndObject();
					}
				}
				gen.writeEndArray();
				gen.writeEndObject();
				gen.close();
			} catch (IOException e) {
			}
		} else {
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
	}
	
	/**
	 * This method creates a buffer for a particular data ID if such a buffer does not exist.
	 * @param type specifies a value type.
	 * @param dataId specifies the data ID.
	 * @return a buffer for the data ID.
	 */
	protected Buffer prepareBuffer(Class<?> type, String typename, int dataId) {
		synchronized (buffers) {
			if (buffers.size() <= dataId) {
				while (buffers.size() <= dataId) {
					buffers.add(null);
				}
			}
			Buffer b = buffers.get(dataId);
			if (b == null) {
				b = new Buffer(type, typename, bufferSize);
				buffers.set(dataId, b);
			}
			return b;
		}
	}

	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, boolean value) {
		if (!closed) {
			Buffer b = prepareBuffer(boolean.class, "boolean", dataId);
			b.addBoolean(value);
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, byte value) {
		if (!closed) {
			Buffer b = prepareBuffer(byte.class, "byte", dataId);
			b.addByte(value);
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, char value) {
		if (!closed) {
			Buffer b = prepareBuffer(char.class, "char", dataId);
			b.addChar(value);
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, double value) {
		if (!closed) {
			Buffer b = prepareBuffer(double.class, "double", dataId);
			b.addDouble(value);
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, float value) {
		if (!closed) {
			Buffer b = prepareBuffer(float.class, "float", dataId);
			b.addFloat(value);
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, int value) {
		if (!closed) {
			Buffer b = prepareBuffer(int.class, "int", dataId);
			b.addInt(value);
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, long value) {
		if (!closed) {
			Buffer b = prepareBuffer(long.class, "long", dataId);
			b.addLong(value);
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, Object value) {
		if (!closed) {
			if (keepObject == ObjectRecordingStrategy.Id) {
				Buffer b = prepareBuffer(long.class, "objectid", dataId); 
				b.addLong(objectIDs.getId(value));
			} else {
				Buffer b = prepareBuffer(Object.class, "object", dataId);
				b.addObject(value);
			}
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, short value) {
		if (!closed) {
			Buffer b = prepareBuffer(short.class, "short", dataId);
			b.addShort(value);
		}
	}	

}

