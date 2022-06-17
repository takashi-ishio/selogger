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
 * The generated "eventfreq.txt" file is a CSV file.
 * Each line shows a pair of dataId and the number of occurrences of the event. 
 */
public class EventFrequencyLogger implements IEventLogger {

	/**
	 * Array of counter objects.  dataId is used as an index for this array.
	 */
	private ArrayList<AtomicLong> counters;
	
	/**
	 * A directory where a resultant file is stored
	 */
	private File traceFile;
	
	/**
	 * A field to record intermediate file name
	 */
	private int saveCount;
	
	/**
	 * Create the logger object.
	 * @param outputDir specifies a directory where a resultant file is stored
	 */
	public EventFrequencyLogger(File traceFile) {
		this.traceFile = traceFile;
		counters = new ArrayList<>();
		saveCount = 0;
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
	
	private void saveCurrentCounters(File file, boolean resetTrace) {
		try (PrintWriter w = new PrintWriter(new FileWriter(file))) {
			int countersLength;
			synchronized (counters) {
				countersLength = counters.size();
			}
			for (int i=0; i<countersLength; i++) {
				long count;
				AtomicLong c = counters.get(i);
				if (resetTrace) count = c.getAndSet(0);
				else count = c.get();
				if (count > 0) {
					w.println(i + "," + count);
				}
			}
		} catch (IOException e) {
		}
	}
	
	@Override
	public synchronized void save(boolean resetTrace) {
		saveCount++;
		String filename = traceFile.getAbsolutePath() + "." + Integer.toString(saveCount) + ".txt";
		saveCurrentCounters(new File(filename), resetTrace);
	}
	
	/**
	 * Write the event count into files when terminated 
	 */
	@Override
	public synchronized void close() {
		saveCurrentCounters(traceFile, false);
	}
	
	
}
