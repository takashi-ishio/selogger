package selogger.logging.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import selogger.logging.IEventLogger;

/**
 * A logger to record only the numbers of each data items without data contents. 
 */
public class EventFrequencyLogger implements IEventLogger {
	
	/**
	 * The name of a file created by this logger
	 */
	private static final String FILENAME = "eventfreq.txt";

	private ArrayList<AtomicInteger> counters;
	private File outputDir;
	
	/**
	 * @param outputDir specifies a directory where a resultant file is stored
	 */
	public EventFrequencyLogger(File outputDir) {
		this.outputDir = outputDir;
		counters = new ArrayList<>();
	}
	
	@Override
	public void recordEvent(int dataId, boolean value) {
		countOccurrence(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, byte value) {
		countOccurrence(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, char value) {
		countOccurrence(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, double value) {
		countOccurrence(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, float value) {
		countOccurrence(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, int value) {
		countOccurrence(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, long value) {
		countOccurrence(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, Object value) {
		countOccurrence(dataId);
	}
	
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
					counters.add(new AtomicInteger());
				}
			}
		}
		AtomicInteger c = counters.get(dataId);
		c.incrementAndGet();
	}
	
	/**
	 * Write the event count into files when terminated 
	 */
	@Override
	public synchronized void close() {
		try (PrintWriter w = new PrintWriter(new FileWriter(new File(outputDir, FILENAME)))) {
			for (int i=0; i<counters.size(); i++) {
				AtomicInteger c = counters.get(i);
				int count = c.get();
				if (count > 0) {
					w.println(i + "," + count);
				}
			}
		} catch (IOException e) {
		}
	}
	
	
}
