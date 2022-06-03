package selogger.logging.util;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import selogger.logging.IErrorLogger;

/**
 * This class is a stream specialized to write a sequence of events into files.
 * A triple of data ID, thread ID, and a value observed in the event is recorded.
 *
 * While a regular stream like FileOutputStream generates a single file,
 * this stream creates a number of files whose size is limited by the number of events
 * (MAX_EVENTS_PER_FILE field).
 */
public class EventDataStream {
	
	/**
	 * The number of events stored in a single file.
	 */
	public static final int MAX_EVENTS_PER_FILE = 10000000;
	
	/**
	 * The data size of an event.
	 */
	public static final int BYTES_PER_EVENT = 16;
	
	private FileNameGenerator files;
	private DataOutputStream out;
	private IErrorLogger err;
	private int count;

	
	/**
	 * Create an instance of stream.
	 * @param target is an object generating file names.
	 * @param logger is to report errors that occur in this class.
	 */
	public EventDataStream(FileNameGenerator target, IErrorLogger logger) {
		try {
			files = target;
			err = logger;
			out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(target.getNextFile())));
			count = 0;
		} catch (IOException e) {
			err.log(e);
		}
	}
	
	/**
	 * Write an event data into a file.  The thread ID is also recorded. 
	 * @param dataId specifies an event and its bytecode location.
	 * @param value specifies a data value observed in the event.
	 */
	public synchronized void write(int dataId, long value) {
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
	 * Close the stream.
	 */
	public synchronized void close() {
		try {
			out.close();
			out = null;
		} catch (IOException e) {
			out = null;
			err.log(e);
		}
	}

}
