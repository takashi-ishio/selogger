package selogger.logging.io;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import selogger.logging.IErrorLogger;
import selogger.logging.IEventLogger;
import selogger.logging.util.FileNameGenerator;
import selogger.logging.util.ObjectIdFile;
import selogger.logging.util.TypeIdMap;
import selogger.logging.util.ObjectIdFile.ExceptionRecording;
import selogger.logging.util.ThreadId;

/**
 * This class is an implementation of IEventLogger that records
 * a sequence of runtime events in files.
 * This object creates three types of files:
 * 1. log-*.slg files recording a sequence of events,
 * 2. LOG$Types.txt recording a list of type IDs and their corresponding type names,
 * 3. ObjectIdMap recording a list of object IDs and their type IDs.
 * Using the second and third files, a user can know classes in an execution trace.
 */
public class BinaryStreamLogger implements IEventLogger {

	public static final String FILENAME_TYPEID = "LOG$Types.txt";

	public static final String LOG_PREFIX = "log-";
	public static final String LOG_SUFFIX = ".slg";

	/**
	 * The number of events stored in a single file.
	 */
	public static final int MAX_EVENTS_PER_FILE = 10000000;
	
	/**
	 * The data size of an event.
	 */
	public static final int BYTES_PER_EVENT = 16;
	
	private File outputDir;
	private FileNameGenerator files;
	private DataOutputStream out;
	private IErrorLogger err;
	private int count;

	private TypeIdMap typeToId;
	private ObjectIdFile objectIdMap;

	/**
	 * Create an instance of logging object.
	 * @param logger specifies an object to record errors that occur in this class
	 * @param outputDir specifies a directory for output files.
	 * @param recordString If this is set to true, the object also records contents of string objects.
	 * @param recordExceptions specifies whether the logger records Exception contents or not.
	 */
	/**
	 * Create an instance of stream.
	 * @param target is an object generating file names.
	 * @param logger is to report errors that occur in this class.
	 */
	public BinaryStreamLogger(IErrorLogger logger, File outputDir, boolean recordString, ExceptionRecording recordExceptions) {
		try {
			this.outputDir = outputDir;
			files = new FileNameGenerator(outputDir, LOG_PREFIX, LOG_SUFFIX);
			err = logger;
			out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(files.getNextFile())));
			count = 0;
			typeToId = new TypeIdMap();
			objectIdMap = new ObjectIdFile(outputDir, recordString, recordExceptions, typeToId);

		} catch (IOException e) {
			err.log(e);
		}
	}
	
	
	/**
	 * Close the stream.
	 */
	public synchronized void close() {
		try {
			out.close();
			out = null;
			objectIdMap.close();
			typeToId.save(new File(outputDir, FILENAME_TYPEID));
		} catch (IOException e) {
			out = null;
			err.log(e);
		}
	}
	
	
	/**
	 * This class does not support a partial trace.
	 */
	@Override
	public void save(boolean resetTrace) {
	}

	/**
	 * Write an event data into a file.  The thread ID is also recorded. 
	 * @param dataId specifies an event and its bytecode location.
	 * @param value specifies a data value observed in the event.
	 */
	private synchronized void write(int dataId, long value) {
		if (out != null) {
			try {
				if (count >= MAX_EVENTS_PER_FILE) {
					out.close();
					out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(files.getNextFile())));
					count = 0;
				}
				out.writeInt(dataId);
				out.writeInt(ThreadId.get());
				out.writeLong(value);
				count++;
			} catch (IOException e) {
				out = null;
				err.log(e);
			}
		}
	}

	/**
	 * Record an event and an object.
	 * The object is translated into an object ID. 
	 */
	public void recordEvent(int dataId, Object value) {
		write(dataId, objectIdMap.getId(value));
	}

	/**
	 * Record an event and an integer value.
	 * To simplify the file writing process, the value is translated into a long value.  
	 */
	public void recordEvent(int dataId, int value) {
		write(dataId, value);
	}

	/**
	 * Record an event and a long integer value.
	 */
	public void recordEvent(int dataId, long value) {
		write(dataId, value);
	}

	/**
	 * Record an event and a byte value.
	 * To simplify the file writing process, the value is translated into a long value.  
	 */
	public void recordEvent(int dataId, byte value) {
		write(dataId, value);
	}

	/**
	 * Record an event and a short integer value.
	 * To simplify the file writing process, the value is translated into a long value.  
	 */
	public void recordEvent(int dataId, short value) {
		write(dataId, value);
	}

	/**
	 * Record an event and a char value.
	 * To simplify the file writing process, the value is translated into a long value.  
	 */
	public void recordEvent(int dataId, char value) {
		write(dataId, value);
	}

	/**
	 * Record an event and a boolean value.
	 * To simplify the file writing process, the value is translated into a long value (true = 1, false = 0).  
	 */
	public void recordEvent(int dataId, boolean value) {
		write(dataId, value ? 1: 0);
	}

	/**
	 * Record an event and a double number.
	 * To simplify the file writing process, the value is translated into a long value preserving the information.
	 */
	public void recordEvent(int dataId, double value) {
		write(dataId, Double.doubleToRawLongBits(value));
	}

	/**
	 * Record an event and a float value.
	 * To simplify the file writing process, the value is translated into a long value preserving the information.
	 */
	public void recordEvent(int dataId, float value) {
		write(dataId, Float.floatToRawIntBits(value));
	}


}
