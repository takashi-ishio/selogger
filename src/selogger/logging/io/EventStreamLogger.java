package selogger.logging.io;

import java.io.File;
import java.io.IOException;

import selogger.logging.IErrorLogger;
import selogger.logging.IEventLogger;
import selogger.logging.util.EventDataStream;
import selogger.logging.util.FileNameGenerator;
import selogger.logging.util.ObjectIdFile;
import selogger.logging.util.TypeIdMap;
import selogger.logging.util.ObjectIdFile.ExceptionRecording;

/**
 * This class is an implementation of IEventLogger that records
 * a sequence of runtime events in files.
 * This object creates three types of files:
 * 1. log-*.slg files recording a sequence of events,
 * 2. LOG$Types.txt recording a list of type IDs and their corresponding type names,
 * 3. ObjectIdMap recording a list of object IDs and their type IDs.
 * Using the second and third files, a user can know classes in an execution trace.
 */
public class EventStreamLogger implements IEventLogger {

	public static final String FILENAME_TYPEID = "LOG$Types.txt";

	public static final String LOG_PREFIX = "log-";
	public static final String LOG_SUFFIX = ".slg";
	
	private File outputDir;
	private IErrorLogger errorLogger; 
	private EventDataStream stream;
	
	private TypeIdMap typeToId;
	private ObjectIdFile objectIdMap;

	/**
	 * Create an instance of logging object.
	 * @param logger specifies an object to record errors that occur in this class
	 * @param outputDir specifies a directory for output files.
	 * @param recordString If this is set to true, the object also records contents of string objects.
	 * @param recordExceptions specifies whether the logger records Exception contents or not.
	 */
	public EventStreamLogger(IErrorLogger logger, File outputDir, boolean recordString, ExceptionRecording recordExceptions) {
		try {
			this.outputDir = outputDir;
			this.errorLogger = logger;
			stream = new EventDataStream(new FileNameGenerator(outputDir, LOG_PREFIX, LOG_SUFFIX), errorLogger);
			typeToId = new TypeIdMap();
			objectIdMap = new ObjectIdFile(outputDir, recordString, recordExceptions, typeToId);
		} catch (IOException e) {
			errorLogger.log("We cannot record runtime information: " + e.getLocalizedMessage());
			errorLogger.log(e);
		}
	}
	
	/**
	 * Close all file streams used by the object.
	 */
	public void close() {
		stream.close();
		objectIdMap.close();
		typeToId.save(new File(outputDir, FILENAME_TYPEID));
	}
	
	/**
	 * Record an event and an object.
	 * The object is translated into an object ID. 
	 */
	public void recordEvent(int dataId, Object value) {
		stream.write(dataId, objectIdMap.getId(value));
	}

	/**
	 * Record an event and an integer value.
	 * To simplify the file writing process, the value is translated into a long value.  
	 */
	public void recordEvent(int dataId, int value) {
		stream.write(dataId, value);
	}

	/**
	 * Record an event and an integer value.
	 */
	public void recordEvent(int dataId, long value) {
		stream.write(dataId, value);
	}

	/**
	 * Record an event and an integer value.
	 * To simplify the file writing process, the value is translated into a long value.  
	 */
	public void recordEvent(int dataId, byte value) {
		stream.write(dataId, value);
	}

	/**
	 * Record an event and an integer value.
	 * To simplify the file writing process, the value is translated into a long value.  
	 */
	public void recordEvent(int dataId, short value) {
		stream.write(dataId, value);
	}

	/**
	 * Record an event and an integer value.
	 * To simplify the file writing process, the value is translated into a long value.  
	 */
	public void recordEvent(int dataId, char value) {
		stream.write(dataId, value);
	}

	/**
	 * Record an event and an integer value.
	 * To simplify the file writing process, the value is translated into a long value (true = 1, false = 0).  
	 */
	public void recordEvent(int dataId, boolean value) {
		stream.write(dataId, value ? 1: 0);
	}

	/**
	 * Record an event and an integer value.
	 * To simplify the file writing process, the value is translated into a long value preserving the information.
	 */
	public void recordEvent(int dataId, double value) {
		stream.write(dataId, Double.doubleToRawLongBits(value));
	}

	/**
	 * Record an event and an integer value.
	 * To simplify the file writing process, the value is translated into a long value preserving the information.
	 */
	public void recordEvent(int dataId, float value) {
		stream.write(dataId, Float.floatToRawIntBits(value));
	}

}
