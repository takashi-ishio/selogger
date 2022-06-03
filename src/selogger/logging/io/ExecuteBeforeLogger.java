package selogger.logging.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import selogger.logging.IErrorLogger;
import selogger.logging.IEventLogger;
import selogger.logging.ILoggingTarget;

/**
 * Count the number of events for each thread.
 * Record an event frequency vector for each first occurrence of an event
 * so that a user can analyze Execute-Before relation. 
 */
public class ExecuteBeforeLogger implements IEventLogger {
	
	public static final String FIELD_FORMAT = "format";
	public static final String FIELD_DATA_ID = "dataId";
	public static final String FIELD_THREAD_ID = "threadId";
	public static final String FIELD_VECTOR_LENGTH = "vectorLength";
	public static final String FIELD_STATE = "state";
	public static final String FIELD_RECORDS = "records";
	public static final String FIELD_FINAL_RECORDS = "finalRecords";
	
	/**
	 * A vector of event occurrences
	 */
	public static class EventCounter {
		
		private long threadId;
		private long[] counters;
		private int maxId;
		
		/**
		 * Create a zero vector 
		 * @param threadId
		 */
		public EventCounter(long threadId) {
			counters = new long[65536];
			this.threadId = threadId;
		}
		
		/**
		 * @param dataId specifies an event
		 * @return true if it is the first occurrence of the event 
		 */
		public boolean isFirst(int dataId) {
			return counters.length <= dataId || counters[dataId] == 0;
		}
		
		/**
		 * Count the event occurrence
		 * @param dataId
		 */
		public void increment(int dataId) {
			// Enlarge the vector if dataId is too large 
			if (counters.length <= dataId) {
				counters = Arrays.copyOf(counters, Math.max(counters.length * 2, (int)(dataId * 1.1)));
			}
			counters[dataId]++;
			maxId = Math.max(maxId, dataId);
		}
		
		/**
		 * @param dataId specifies an event
		 * @return the number of occurrences of the event
		 */
		public long getFrequency(int dataId) {
			if (counters.length <= dataId) {
				return 0;
			}
			return counters[dataId];
		}
		
		/**
		 * @return the maximum value of observed dataIds. 
		 */
		public int getMaxId() {
			return maxId;
		}
		
		/**
		 * @return the id of a thread represented by this object 
		 */
		public long getThreadId() {
			return threadId;
		}
	}
	
	/**
	 * A class to manage EventCounter for each thread
	 */
	private static class EventCounters extends ThreadLocal<EventCounter> {
		
		/**
		 * This keeps objects in a list so that close() can 
		 * record the final state of all threads 
		 */
		private ArrayList<EventCounter> counters = new ArrayList<>();
		
		@Override
		protected synchronized EventCounter initialValue() {
			EventCounter c = new EventCounter(Thread.currentThread().getId());
			counters.add(c);
			return c;
		}
		
		public synchronized ArrayList<EventCounter> getCounters() {
			// Return a copy to avoid ConcurrentModificationException 
			return new ArrayList<>(counters);
		}
	}
	
	private static EventCounters executed = new EventCounters();
	private JsonGenerator generator;
	private ILoggingTarget target;
	private IErrorLogger logger;
	
	
	/**
	 * Construct a logger.
	 * @param outputStream specifies a stream for output 
	 * @param target specifies a set of dataid whose first occurrences are interesting  
	 * @param logger will be used to record runtime exceptions 
	 */
	public ExecuteBeforeLogger(OutputStream outputStream, ILoggingTarget target, IErrorLogger logger) {
		try {
			this.target = target;
			
			JsonFactory factory = new JsonFactory();
			generator = factory.createGenerator(outputStream, JsonEncoding.UTF8);
			generator.writeStartObject();
			generator.writeStringField(FIELD_FORMAT, "execute-before");
			generator.writeArrayFieldStart(FIELD_RECORDS);
			
		} catch (IOException e) {
			if (logger != null) logger.log(e);
			generator = null;
		}
	}
	
	/**
	 * Count an event occurrence.
	 * Record the state before increment if it is the first occurrence in the execution trace.
	 * @param dataId specifies an event.
	 */
	private void recordIfFirstOccurrence(int dataId) {  
		if (generator == null) return;
		
		EventCounter executedDataId = executed.get();
		if ((target == null || target.isTarget(dataId)) && executedDataId.isFirst(dataId)) {
			try {
				synchronized (generator) {
					if (!generator.isClosed()) {
						recordCurrentState(executedDataId, dataId);
					}
				}
			} catch (IOException e) {
				if (logger != null) logger.log(e);
				generator = null;
			}
		}
		executedDataId.increment(dataId);
	}
	
	/**
	 * Record the current state of an event vector
	 */
	private void recordCurrentState(EventCounter executedDataId, int dataId) throws IOException {
		int vectorLength = executedDataId.getMaxId() + 1;
		generator.writeStartObject();
		if (dataId >= 0) {
			generator.writeNumberField(FIELD_DATA_ID, dataId);
		}
		generator.writeNumberField(FIELD_THREAD_ID, executedDataId.getThreadId());
		generator.writeNumberField(FIELD_VECTOR_LENGTH, vectorLength);
		generator.writeFieldName(FIELD_STATE);
		generator.writeArray(executedDataId.counters, 0, vectorLength);
		generator.writeEndObject();
	}

	/**
	 * Record the final state of event frequency vectors and close the stream
	 */
	@Override
	public synchronized void close() {
		if (generator != null) {
			synchronized (generator) {
				try {
					generator.writeEndArray();
					generator.writeFieldName(FIELD_FINAL_RECORDS);
					generator.writeStartArray();
					for (EventCounter c: executed.getCounters()) {
						recordCurrentState(c, -1);
					}
					generator.writeEndArray();
					generator.writeEndObject();
					generator.close();
				} catch (IOException e) {
					if (logger != null) logger.log(e);
				} finally {
					generator = null;
				}
			}
		}
	}
	
	/**
	 * TODO Implement the save method
	 */
	@Override
	public void save(boolean resetTrace) {
	}

	/**
	 * Record an occurrence of an event ignoring the data value
	 */
	@Override
	public void recordEvent(int dataId, boolean value) {
		recordIfFirstOccurrence(dataId);
	}
	
	/**
	 * Record an occurrence of an event ignoring the data value
	 */
	@Override
	public void recordEvent(int dataId, byte value) {
		recordIfFirstOccurrence(dataId);
	}
	
	/**
	 * Record an occurrence of an event ignoring the data value
	 */
	@Override
	public void recordEvent(int dataId, char value) {
		recordIfFirstOccurrence(dataId);
	}
	
	/**
	 * Record an occurrence of an event ignoring the data value
	 */
	@Override
	public void recordEvent(int dataId, double value) {
		recordIfFirstOccurrence(dataId);
	}
	
	/**
	 * Record an occurrence of an event ignoring the data value
	 */
	@Override
	public void recordEvent(int dataId, float value) {
		recordIfFirstOccurrence(dataId);
	}
	
	/**
	 * Record an occurrence of an event ignoring the data value
	 */
	@Override
	public void recordEvent(int dataId, int value) {
		recordIfFirstOccurrence(dataId);
	}
	
	/**
	 * Record an occurrence of an event ignoring the data value
	 */
	@Override
	public void recordEvent(int dataId, long value) {
		recordIfFirstOccurrence(dataId);
	}

	/**
	 * Record an occurrence of an event ignoring the data value
	 */
	@Override
	public void recordEvent(int dataId, Object value) {
		recordIfFirstOccurrence(dataId);
	}
	
	/**
	 * Record an occurrence of an event ignoring the data value
	 */
	@Override
	public void recordEvent(int dataId, short value) {
		recordIfFirstOccurrence(dataId);
	}
	
}
