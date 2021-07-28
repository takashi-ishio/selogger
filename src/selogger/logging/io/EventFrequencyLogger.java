package selogger.logging.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import selogger.logging.IEventLogger;

/**
 * This class is an implementation of IEventLogger that counts
 * the number of occurrences for each event (dataId). 
 */
public class EventFrequencyLogger implements IEventLogger {
	
	/**
	 * The name of a file created by this logger
	 */
	private static final String FILENAME = "eventfreq.txt";

	/**
	 * Array of counter objects.  dataId is used as an index for this array.
	 */
	private ArrayList<AtomicLong> counters;
	
	/**
	 * A directory where a resultant file is stored
	 */
	private File outputDir;
	
	/**
	 * Create the logger object.
	 * @param outputDir specifies a directory where a resultant file is stored
	 */
	public EventFrequencyLogger(File outputDir) {
		this.outputDir = outputDir;
		counters = new ArrayList<>();
	}
	
	/**
	 * Count the event occurrence.
	 * @param dataId specifies an event to be counted.
	 * @param value is required for interface, but the value is discarded by this logger. 
	 */
	@Override
	public void recordEvent(int dataId, boolean value) {
		countOccurrence(dataId);
	}
	
	/**
	 * Count the event occurrence.
	 * @param dataId specifies an event to be counted.
	 * @param value is required for interface, but the value is discarded by this logger. 
	 */
	@Override
	public void recordEvent(int dataId, byte value) {
		countOccurrence(dataId);
	}
	
	/**
	 * Count the event occurrence.
	 * @param dataId specifies an event to be counted.
	 * @param value is required for interface, but the value is discarded by this logger. 
	 */
	@Override
	public void recordEvent(int dataId, char value) {
		countOccurrence(dataId);
	}
	
	/**
	 * Count the event occurrence.
	 * @param dataId specifies an event to be counted.
	 * @param value is required for interface, but the value is discarded by this logger. 
	 */
	@Override
	public void recordEvent(int dataId, double value) {
		countOccurrence(dataId);
	}
	
	/**
	 * Count the event occurrence.
	 * @param dataId specifies an event to be counted.
	 * @param value is required for interface, but the value is discarded by this logger. 
	 */
	@Override
	public void recordEvent(int dataId, float value) {
		countOccurrence(dataId);
	}
	
	/**
	 * Count the event occurrence.
	 * @param dataId specifies an event to be counted.
	 * @param value is required for interface, but the value is discarded by this logger. 
	 */
	@Override
	public void recordEvent(int dataId, int value) {
		countOccurrence(dataId);
	}
	
	/**
	 * Count the event occurrence.
	 * @param dataId specifies an event to be counted.
	 * @param value is required for interface, but the value is discarded by this logger. 
	 */
	@Override
	public void recordEvent(int dataId, long value) {
		countOccurrence(dataId);
	}
	
	/**
	 * Count the event occurrence.
	 * @param dataId specifies an event to be counted.
	 * @param value is required for interface, but the value is discarded by this logger. 
	 */
	@Override
	public void recordEvent(int dataId, Object value) {
		countOccurrence(dataId);
	}
	
	/**
	 * Count the event occurrence.
	 * @param dataId specifies an event to be counted.
	 * @param value is required for interface, but the value is discarded by this logger. 
	 */
	@Override
	public void recordEvent(int dataId, short value) {
		countOccurrence(dataId);
	}
	
	/**
	 * Increment an event count.
	 * @param dataId specifies an event.
	 */
	private void countOccurrence(int dataId) {
		// Prepare a counter (if not exist)
		if (counters.size() <= dataId) {
			synchronized(counters) { 
				while (counters.size() <= dataId) {
					counters.add(new AtomicLong());
				}
			}
		}
		// Increment the counter specified by dataId
		AtomicLong c = counters.get(dataId);
		c.incrementAndGet();
	}
	
	/**
	 * Write the event count into files when terminated 
	 */
	@Override
	public synchronized void close() {
		try (PrintWriter w = new PrintWriter(new FileWriter(new File(outputDir, FILENAME)))) {
			for (int i=0; i<counters.size(); i++) {
				AtomicLong c = counters.get(i);
				long count = c.get();
				if (count > 0) {
					w.println(i + "," + count);
				}
			}
		} catch (IOException e) {
		}
	}
	
	
}
