package selogger.logging.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import selogger.logging.IErrorLogger;
import selogger.logging.IEventLogger;
import selogger.logging.util.FileNameGenerator;
import selogger.logging.util.ObjectIdFile;
import selogger.logging.util.ThreadId;
import selogger.logging.util.TypeIdMap;
import selogger.logging.util.ObjectIdFile.ExceptionRecording;

public class TextStreamLogger implements IEventLogger {

	public static final String LOG_PREFIX = "log-";
	public static final String LOG_SUFFIX = ".txt";

	/**
	 * The number of events stored in a single file.
	 */
	public static final int MAX_EVENTS_PER_FILE = 10000000;
	
	private File outputDir;
	private FileNameGenerator files;
	private PrintWriter out;
	private IErrorLogger err;
	private int count;
	private long seqnum;

	private TypeIdMap typeToId;
	private ObjectIdFile objectIdMap;

	
	/**
	 * Create an instance of logging object.
	 * @param logger specifies an object to record errors that occur in this class
	 * @param outputDir specifies a directory for output files.
	 * @param recordString If this is set to true, the object also records contents of string objects.
	 * @param recordExceptions specifies whether the logger records Exception contents or not.
	 */
	public TextStreamLogger(IErrorLogger logger, File outputDir, boolean recordString, ExceptionRecording recordExceptions) {
		try {
			this.outputDir = outputDir;
			files = new FileNameGenerator(outputDir, LOG_PREFIX, LOG_SUFFIX);
			err = logger;
			out = new PrintWriter(new FileWriter(files.getNextFile()));
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
		out.close();
		out = null;
		objectIdMap.close();
		typeToId.save(new File(outputDir, BinaryStreamLogger.FILENAME_TYPEID));
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
	private synchronized void write(int dataId, String value) {
		if (out != null) {
			try {
				if (count >= MAX_EVENTS_PER_FILE) {
					out.close();
					out = new PrintWriter(new FileWriter(files.getNextFile()));
					count = 0;
				}
				StringBuilder builder = new StringBuilder(128);
				builder.append(seqnum);
				builder.append(",");
				builder.append(dataId);
				builder.append(",");
				builder.append(ThreadId.get());
				builder.append(",");
				builder.append(value);
				builder.append("\n");
				out.write(builder.toString());
				count++;
				seqnum++;
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
		write(dataId, Long.toString(objectIdMap.getId(value)));
	}

	/**
	 * Record an event and an integer value.
	 * To simplify the file writing process, the value is translated into a long value.  
	 */
	public void recordEvent(int dataId, int value) {
		write(dataId, Integer.toString(value));
	}

	/**
	 * Record an event and a long integer value.
	 */
	public void recordEvent(int dataId, long value) {
		write(dataId, Long.toString(value));
	}

	/**
	 * Record an event and a byte value.
	 * To simplify the file writing process, the value is translated into a long value.  
	 */
	public void recordEvent(int dataId, byte value) {
		write(dataId, Byte.toString(value));
	}

	/**
	 * Record an event and a short integer value.
	 * To simplify the file writing process, the value is translated into a long value.  
	 */
	public void recordEvent(int dataId, short value) {
		write(dataId, Short.toString(value));
	}

	/**
	 * Record an event and a char value.
	 * To simplify the file writing process, the value is translated into a long value.  
	 */
	public void recordEvent(int dataId, char value) {
		write(dataId, Integer.toString(value));
	}

	/**
	 * Record an event and a boolean value.
	 * To simplify the file writing process, the value is translated into a long value (true = 1, false = 0).  
	 */
	public void recordEvent(int dataId, boolean value) {
		write(dataId, Boolean.toString(value));
	}

	/**
	 * Record an event and a double number.
	 * To simplify the file writing process, the value is translated into a long value preserving the information.
	 */
	public void recordEvent(int dataId, double value) {
		write(dataId, Double.toString(value));
	}

	/**
	 * Record an event and a float value.
	 * To simplify the file writing process, the value is translated into a long value preserving the information.
	 */
	public void recordEvent(int dataId, float value) {
		write(dataId, Float.toString(value));
	}


}
