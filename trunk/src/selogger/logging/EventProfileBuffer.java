package selogger.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class EventProfileBuffer implements IEventWriter {
	
	private static final int MAX_EVENT = 120;
	private static final int HEADER_SIZE = 2 + 4 + 8 + 4;
	
	private long[] eventCounter; 
	private long size;
	
	private File outputDir; 
	private IOException lastException;
	private long time;
	
	public EventProfileBuffer(File outputDir) {
		this.outputDir = outputDir;
		eventCounter = new long[MAX_EVENT];
		size = 0;
		time = System.currentTimeMillis();
	}
	
	@Override
	public void registerEventWithoutData(int eventType, long eventId,
			int threadId, long locationId) {
		eventCounter[eventType]++;
		size += HEADER_SIZE;
	}
	
	@Override
	public void registerIntValue(int eventType, long eventId, int threadId,
			long locationId, int intData, double value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 12;
	}
	
	@Override
	public void registerIntValue(int eventType, long eventId, int threadId,
			long locationId, int intData, float value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 8;
	}
	
	@Override
	public void registerIntValue(int eventType, long eventId, int threadId,
			long locationId, int intData, int value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 8;
	}
	
	@Override
	public void registerIntValue(int eventType, long eventId, int threadId,
			long locationId, int intData, long value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 12;
	}
	
	@Override
	public void registerLong(int eventType, long eventId, int threadId,
			long locationId, long longData) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 8;
	}
	
	@Override
	public void registerLongInt(int eventType, long eventId, int threadId,
			long locationId, long longData, int intData) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 12;
	}
	
	@Override
	public void registerLongIntValue(int eventType, long eventId,
			int threadId, long locationId, long longData, int intData,
			double value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 20;
	}
	
	@Override
	public void registerLongIntValue(int eventType, long eventId,
			int threadId, long locationId, long longData, int intData,
			float value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 16;
	}
	
	@Override
	public void registerLongIntValue(int eventType, long eventId,
			int threadId, long locationId, long longData, int intData,
			int value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 16;
	}
	
	@Override
	public void registerLongIntValue(int eventType, long eventId,
			int threadId, long locationId, long longData, int intData,
			long value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 20;
	}
	
	@Override
	public void registerLongValue(int eventType, long eventId, int threadId,
			long locationId, long longData, double value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 16;
	}
	
	@Override
	public void registerLongValue(int eventType, long eventId, int threadId,
			long locationId, long longData, float value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 12;
	}
	
	@Override
	public void registerLongValue(int eventType, long eventId, int threadId,
			long locationId, long longData, int value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 12;
	}
	
	@Override
	public void registerLongValue(int eventType, long eventId, int threadId,
			long locationId, long longData, long value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 16;
	}
	
	@Override
	public void registerValue(int eventType, long eventId, int threadId,
			long locationId, double value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 8;
	}
	
	@Override
	public void registerValue(int eventType, long eventId, int threadId,
			long locationId, float value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 4;
	}
	
	@Override
	public void registerValue(int eventType, long eventId, int threadId,
			long locationId, int value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 4;
	}
	
	@Override
	public void registerValue(int eventType, long eventId, int threadId,
			long locationId, long value) {
		eventCounter[eventType]++;
		size += HEADER_SIZE + 8;
	}
	
	@Override
	public void registerValueVoid(int eventType, long eventId, int threadId,
			long locationId) {
		eventCounter[eventType]++;
		size += HEADER_SIZE;
	}
	
	@Override
	public boolean hasError() {
		return lastException != null;
	}
	
	@Override
	public String getErrorMessage() {
		return lastException.getLocalizedMessage();
	}
	
	@Override
	public void close() {
		try {
			FileWriter w = new FileWriter(new File(outputDir, "events.tsv"));
			w.write("EVENT\tCOUNT\n");
			long total = 0;
			for (int i=0; i<eventCounter.length; ++i) {
				StringBuilder b = new StringBuilder(128);
				b.append(i).append("\t").append(eventCounter[i]).append("\n");
				w.write(b.toString());
				total += eventCounter[i];
			}
			w.write("TOTAL\t" + Long.toString(total) + "\n");
			w.write("SIZE\t" + Long.toString(size) + "\n");
			w.write("TIME\t" + Long.toString(System.currentTimeMillis() - time) + "\n");
			w.close();
		} catch (IOException e) {
			lastException = e;
		}
	}
	
}


