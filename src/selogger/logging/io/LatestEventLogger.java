package selogger.logging.io;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import selogger.logging.IErrorLogger;
import selogger.logging.IEventLogger;
import selogger.logging.util.JsonBuffer;
import selogger.logging.util.ObjectIdMap;
import selogger.logging.util.ObjectIdFile.ExceptionRecording;
import selogger.weaver.DataInfo;
import selogger.weaver.method.Descriptor;
import selogger.logging.util.ThreadId;

/**
 * This class is an implementation of IEventLogger that 
 * records a near-omniscient execution trace including 
 * only the latest k events for each data ID.
 * If OutOfMemory occurred, in other words, if this object could 
 * not keep the near-omniscient execution trace on memory,
 * all events are discarded.
 */
public class LatestEventLogger extends AbstractEventLogger implements IEventLogger {

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
		 * String and exception messages are recorded with the ID.
		 */
		Id,
		/**
		 * The buffers keep objects using Object ID.
		 */
		IdOnly
	}

	
	/**
	 * The number of events to be recorded for each event location
	 */
	private int bufferSize;
	
	/**
	 * Buffers to record events 
	 */
	private ArrayList<LatestEventBuffer> buffers;
	
	/**
	 * The directory to store execution traces
	 */
	private File traceFile;
	
	/**
	 * A strategy to keep (or discard) object references
	 */
	private ObjectRecordingStrategy keepObject;
	
	/**
	 * Use a JSON format or not 
	 */
	private boolean outputJson;
	
	/**
	 * Object to record error messages 
	 */
	private IErrorLogger logger;
	
	/**
	 */
	private boolean closed;
	
	/**
	 * For id-based object recoding. 
	 */
	private ObjectIdMap objectIDs;

	/**
	 * Record the number of partial trace files
	 */
	private int saveCount;

	/**
	 * This object generates a sequence number for each event.
	 * Each event has a sequence number from 1 representing 
	 * the order of event occurrence.  
	 */
	private static AtomicLong seqnum = new AtomicLong(0);

	public static long getSeqnum() {
		return seqnum.get();
	}

	/**
	 * Create an instance of this logger.
	 * @param outputDir specifies a directory for output files.
	 * @param bufferSize specifies the size of buffer ("k" in Near-Omniscient Debugging)
	 * @param keepObject specifies how the buffers keep Java objects.  
	 * @param outputJson specifies whether the logger uses a json format or not.
	 */
	public LatestEventLogger(File traceFile, int bufferSize, ObjectRecordingStrategy keepObject, boolean outputJson, IErrorLogger errorLogger) {
		super("nearomni");
		this.traceFile = traceFile;
		this.bufferSize = bufferSize;
		this.buffers = new ArrayList<>();
		this.keepObject = keepObject;
		this.outputJson = outputJson;
		this.logger = errorLogger;
		if (this.keepObject == ObjectRecordingStrategy.IdOnly || this.keepObject == ObjectRecordingStrategy.Id) {
			objectIDs = new ObjectIdMap(65536);
		}
	}
	
	/**
	 * Save the recorded trace
	 */
	@Override
	public synchronized void save(boolean resetTrace) {
		saveCount++;
		long t = System.currentTimeMillis();
		File f = new File(traceFile.getAbsolutePath() + "." + Integer.toString(saveCount) + (outputJson? ".json": ".txt"));
		try (PrintWriter w = new PrintWriter(new FileWriter(f))){
			if (outputJson) {
				saveJson(w);
			} else {
				saveText(w);
			}
		} catch (Throwable e) {
			if (logger != null) logger.log(e);
		}
		if (logger != null) {
			logger.log(Long.toString(System.currentTimeMillis() - t) + "ms used to save a trace");
		}
		buffers = null;
		buffers = new ArrayList<>();
	}



	/**
	 * Close the logger and save the contents into a file naemd "recentdata.txt".
	 */
	@Override
	public synchronized void close() {
		closed = true; 
		if (objectIDs != null) {
			objectIDs.close();
		}
		long t = System.currentTimeMillis();
		try (PrintWriter w = new PrintWriter(new FileWriter(traceFile))){
			if (outputJson) {
				saveJson(w);
			} else {
				saveText(w);
			}
		} catch (Throwable e) {
			if (logger != null) logger.log(e);
		}
		if (logger != null) {
			logger.log(Long.toString(System.currentTimeMillis() - t) + "ms used to save a trace");
		}
	}
		
	/**
	 * This method creates a buffer for a particular data ID if such a buffer does not exist.
	 * @param type specifies a value type.
	 * @param dataId specifies the data ID.
	 * @return a buffer for the data ID.
	 */
	protected synchronized LatestEventBuffer prepareBuffer(Class<?> type, String typename, int dataId) {
		if (!closed) {
			try {
				while (buffers.size() <= dataId) {
					buffers.add(null);
				}
				LatestEventBuffer b = buffers.get(dataId);
				if (b == null) {
					b = new LatestEventBuffer(type, bufferSize, keepObject);
					buffers.set(dataId, b);
				}
				return b;
			} catch (OutOfMemoryError e) {
				// release the entire buffers
				closed = true;
				buffers = null;
				buffers = new ArrayList<>();
				logger.log("OutOfMemoryError: Logger discarded internal buffers to continue the current execution.");
			}
		}
		return null;
	}

	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, boolean value) {
		LatestEventBuffer b = prepareBuffer(boolean.class, "boolean", dataId);
		if (b != null) {
			b.addBoolean(value, seqnum.getAndIncrement(), ThreadId.get());
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, byte value) {
		LatestEventBuffer b = prepareBuffer(byte.class, "byte", dataId);
		if (b != null) {
			b.addByte(value, seqnum.getAndIncrement(), ThreadId.get());
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, char value) {
		LatestEventBuffer b = prepareBuffer(char.class, "char", dataId);
		if (b != null) {
			b.addChar(value, seqnum.getAndIncrement(), ThreadId.get());
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, double value) {
		LatestEventBuffer b = prepareBuffer(double.class, "double", dataId);
		if (b != null) {
			b.addDouble(value, seqnum.getAndIncrement(), ThreadId.get());
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, float value) {
		LatestEventBuffer b = prepareBuffer(float.class, "float", dataId);
		if (b != null) {
			b.addFloat(value, seqnum.getAndIncrement(), ThreadId.get());
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, int value) {
		LatestEventBuffer b = prepareBuffer(int.class, "int", dataId);
		if (b != null) {
			b.addInt(value, seqnum.getAndIncrement(), ThreadId.get());
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, long value) {
		LatestEventBuffer b = prepareBuffer(long.class, "long", dataId);
		if (b != null) {
			b.addLong(value, seqnum.getAndIncrement(), ThreadId.get());
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, Object value) {
		if (keepObject == ObjectRecordingStrategy.IdOnly ||
			keepObject == ObjectRecordingStrategy.Id) {
			LatestEventBuffer b = prepareBuffer(String.class, "objectid", dataId);
			if (b != null) {
				String id = getObjectId(value, keepObject == ObjectRecordingStrategy.Id);
				b.addObjectId(id, seqnum.getAndIncrement(), ThreadId.get());
			}				
		} else {
			LatestEventBuffer b = prepareBuffer(Object.class, "object", dataId);
			if (b != null) {
				b.addObject(value, seqnum.getAndIncrement(), ThreadId.get());
			}
		}
	}
	
	/**
	 * Record the event and the observed value.
	 */
	@Override
	public void recordEvent(int dataId, short value) {
		LatestEventBuffer b = prepareBuffer(short.class, "short", dataId);
		if (b != null) {
			b.addShort(value, seqnum.getAndIncrement(), ThreadId.get());
		}
	}	
	
	/**
	 * Create a string representation for an object ID
	 * @param value specifies an object
	 * @param includeTextValue If this parameter is true, the string content is included  
	 * @return
	 */
	public String getObjectId(Object value, boolean includeTextValue) {
		String id = null;
		if (value != null) {
			id = value.getClass().getName() + "@" + objectIDs.getId(value);
			if (includeTextValue) {
				if (value instanceof String) {
					id = id + ":" + value;
				} else if (value instanceof Throwable) {
					id = id + ":" + ((Throwable)value).getMessage();
				}
			}
		}
		return id;
	}

	/**
	 * @return true if there exists an event
	 */
	@Override
	protected boolean isRecorded(int dataid) {
		return dataid < buffers.size() && buffers.get(dataid) != null;
	}

	/**
	 * Write attributes in a JSON format
	 */
	@Override
	protected void writeAttributes(JsonBuffer buf, DataInfo d) {
		LatestEventBuffer b = buffers.get(d.getDataId());
		if (b != null) {
			b.writeJson(buf, d.getValueDesc() == Descriptor.Void);
		}
	}	
	
	/**
	 * Provide columns for a CSV format
	 */
	@Override
	protected String getColumnNames() {
		return LatestEventBuffer.getColumnNames(bufferSize);
	}
	
	/**
	 * Write attributes in a CSV format
	 */
	@Override
	protected void writeAttributes(StringBuilder builder, DataInfo d) {
		LatestEventBuffer b = buffers.get(d.getDataId());
		if (b != null) {
			builder.append(b.toString());
		} else {
			builder.append(LatestEventBuffer.getEmptyColumns(bufferSize));
		}
	}
}
