package selogger.logging;

import java.io.File;
import java.io.IOException;

import selogger.logging.io.EventDataStream;
import selogger.logging.io.FileNameGenerator;
import selogger.logging.io.IErrorLogger;

public class EventLogger {

	public static final String FILENAME_TYPEID = "LOG$Types.txt";
	public static final String FILENAME_THREADID = "LOG$Threads.txt";

	static EventLogger INSTANCE;
	
	private File outputDir;
	private IErrorLogger errorLogger; 
	private EventDataStream stream;
	
	private TypeIdMap typeToId;
	private ObjectIdFile objectIdMap;
	
	public static EventLogger initialize(File outputDir, boolean recordString, IErrorLogger errorLogger) {
		try {
			INSTANCE = new EventLogger(errorLogger, outputDir, recordString);
			return INSTANCE;
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	private EventLogger(IErrorLogger logger, File outputDir, boolean recordString) {
		try {
			this.outputDir = outputDir;
			this.errorLogger = logger;
			stream = new EventDataStream(new FileNameGenerator(outputDir), errorLogger);
			typeToId = new TypeIdMap();
			objectIdMap = new ObjectIdFile(outputDir, recordString, typeToId);
		} catch (IOException e) {
			errorLogger.log("We cannot record runtime information: " + e.getLocalizedMessage());
			errorLogger.log(e);
		}
	}
	
	public void close() {
		stream.close();
		objectIdMap.close();
		typeToId.save(new File(outputDir, FILENAME_TYPEID));
	}
	
	public void recordEvent(int dataId, Object value) {
		stream.write(dataId, objectIdMap.getId(value));
	}

	public void recordEvent(int dataId, int value) {
		stream.write(dataId, value);
	}

	public void recordEvent(int dataId, long value) {
		stream.write(dataId, value);
	}

	public void recordEvent(int dataId, byte value) {
		stream.write(dataId, value);
	}

	public void recordEvent(int dataId, short value) {
		stream.write(dataId, value);
	}

	public void recordEvent(int dataId, char value) {
		stream.write(dataId, value);
	}

	public void recordEvent(int dataId, boolean value) {
		stream.write(dataId, value ? 1: 0);
	}

	public void recordEvent(int dataId, double value) {
		stream.write(dataId, Double.doubleToRawLongBits(value));
	}

	public void recordEvent(int dataId, float value) {
		stream.write(dataId, Float.floatToRawIntBits(value));
	}

}
