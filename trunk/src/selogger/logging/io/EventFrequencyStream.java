package selogger.logging.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class EventFrequencyStream implements IEventStream {

	private static class Counter {
		private int count = 0;
		public void increment() {
			count++;
		}
	}
	
	private ArrayList<Counter> counters;
	private File outputDir;
	
	public EventFrequencyStream(File outputDir) {
		this.outputDir = outputDir;
		counters = new ArrayList<>();
	}
	
	@Override
	public synchronized void write(int dataId, long value) {
		while (counters.size() <= dataId) {
			counters.add(null);
		}
		Counter c = counters.get(dataId); 
		if (c == null) {
			c = new Counter();
			counters.set(dataId, c);
		}
		c.increment();
	}
	
	@Override
	public synchronized void close() {
		// TODO Save counters
		try (PrintWriter w = new PrintWriter(new FileWriter(new File(outputDir, "eventfreq.txt")))) {
			for (int i=0; i<counters.size(); i++) {
				Counter c = counters.get(i);
				int count = c != null? c.count: 0;
				w.println(i + "," + count);
			}
		} catch (IOException e) {
		}
	}
	
	
}
