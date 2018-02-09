package selogger.logging;

import java.io.File;

import selogger.logging.io.DiscardLogger;
import selogger.logging.io.EventFrequencyLogger;
import selogger.logging.io.EventStreamLogger;
import selogger.logging.io.LatestEventLogger;
import selogger.logging.io.LatestEventTimeLogger;
import selogger.logging.io.MemoryLogger;

public class EventLogger {
	
	static IEventLogger INSTANCE;
	
	public static IEventLogger initialize(File outputDir, boolean recordString, IErrorLogger errorLogger) {
		try {
			INSTANCE = new EventStreamLogger(errorLogger, outputDir, recordString);
			return INSTANCE;
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public static IEventLogger initializeFrequencyLogger(File outputDir) {
		INSTANCE = new EventFrequencyLogger(outputDir);
		return INSTANCE;
	}
	
	public static IEventLogger initializeLatestDataLogger(File outputDir, int bufferSize, boolean keepObject) {
		INSTANCE = new LatestEventLogger(outputDir, bufferSize, keepObject);
		return INSTANCE;
	}

	public static IEventLogger initializeLatestEventTimeLogger(File outputDir, int bufferSize, boolean keepObject) {
		INSTANCE = new LatestEventTimeLogger(outputDir, bufferSize, keepObject);
		return INSTANCE;
	}
	
	public static IEventLogger initializeDiscardLogger() {
		INSTANCE = new DiscardLogger();
		return INSTANCE;
	}

	public static MemoryLogger initializeForTest() {
		MemoryLogger m = new MemoryLogger(); 
		INSTANCE = m;
		return m;
	}
	
}
