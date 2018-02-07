package selogger.logging;

import java.io.File;

import selogger.logging.io.EventFrequencyLogger;
import selogger.logging.io.EventStreamLogger;
import selogger.logging.io.LatestEventLogger;
import selogger.logging.io.MemoryLogger;

public class EventLogger {

	public enum Mode { Stream, Frequency, FixedSize };
	
	static IEventLogger INSTANCE;
	
	public static IEventLogger initialize(File outputDir, boolean recordString, IErrorLogger errorLogger, Mode mode) {
		try {
			if (mode == Mode.Frequency) {
				INSTANCE = new EventFrequencyLogger(outputDir);
			} else if (mode == Mode.FixedSize) {
				INSTANCE = new LatestEventLogger(outputDir, 32);
			} else {
				INSTANCE = new EventStreamLogger(errorLogger, outputDir, recordString);
			}
			return INSTANCE;
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public static MemoryLogger initializeForTest() {
		MemoryLogger m = new MemoryLogger(); 
		INSTANCE = m;
		return m;
	}
	
}
