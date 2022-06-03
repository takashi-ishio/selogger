package selogger.logging.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import selogger.logging.IErrorLogger;
import selogger.logging.IEventLogger;
import selogger.logging.util.ObjectIdFile;
import selogger.logging.util.TypeIdMap;
import selogger.logging.util.ObjectIdFile.ExceptionRecording;
import selogger.logging.util.ThreadId;

/**
 * This class is an implementation of IEventLogger that 
 * records a near-omniscient execution trace including 
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
	private File outputDir;
	
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
	 * If OutOfMemory occurred, in other words, if this object could 
	 * not keep the near-omniscient execution trace on memory,
	 * this flag becomes true and discard events in buffers.
	 * As a result, the trace file (recentdata.txt) becomes empty. 
	 */
	private boolean disabledByOutOfMemory;
	
	/**
	 * For id-based object recoding. 
	 */
	private TypeIdMap objectTypes;

	/**
	 * For id-based object recoding. 
	 */
	private ObjectIdFile objectIDs;


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
	 * @param keepObject specifies how the buffers keep Java objects.  
	 * @param recordString specifies whether the logger records String contents or not.
	 * @param recordExceptions specifies whether the logger records Exception contents or not.
	 * @param outputJson specifies whether the logger uses a json format or not.
	 */
	public LatestEventLogger(File outputDir, int bufferSize, ObjectRecordingStrategy keepObject, boolean recordString, ExceptionRecording recordExceptions, boolean outputJson, IErrorLogger errorLogger) {
		this.outputDir = outputDir;
		this.bufferSize = bufferSize;
		this.buffers = new ArrayList<>();
		this.keepObject = keepObject;
		this.outputJson = outputJson;
		this.logger = errorLogger;
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
	public synchronized void close() {
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
					LatestEventBuffer b = buffers.get(i);
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
					LatestEventBuffer b = buffers.get(i);
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
	protected synchronized LatestEventBuffer prepareBuffer(Class<?> type, String typename, int dataId) {
		if (!disabledByOutOfMemory) {
			try {
				while (buffers.size() <= dataId) {
					buffers.add(null);
				}
				LatestEventBuffer b = buffers.get(dataId);
				if (b == null) {
					b = new LatestEventBuffer(type, typename, bufferSize, keepObject);
					buffers.set(dataId, b);
				}
				return b;
			} catch (OutOfMemoryError e) {
				// release the entire buffers
				disabledByOutOfMemory = true;
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
		if (keepObject == ObjectRecordingStrategy.Id) {
			LatestEventBuffer b = prepareBuffer(long.class, "objectid", dataId); 
			if (b != null) {
				b.addLong(objectIDs.getId(value), seqnum.getAndIncrement(), ThreadId.get());
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

}
