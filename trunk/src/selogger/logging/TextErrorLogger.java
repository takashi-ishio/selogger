package selogger.logging;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import selogger.logging.io.IErrorLogger;

public class TextErrorLogger implements IErrorLogger {

	private PrintStream err;
	
	/**
	 * The created logger simply ignores all errors.
	 */
	public TextErrorLogger() {
	}
	
	public TextErrorLogger(File outputLogFile) {
		try {
			err = new PrintStream(outputLogFile);
		} catch (IOException e) {
		}
	}
	
	@Override
	public synchronized void record(Throwable t) {
		if (err != null) {
			t.printStackTrace(err);
		}
	}
	
	@Override
	public synchronized void record(String msg) {
		if (err != null) {
			err.println(msg);
		}
	}
	
	public synchronized void close() {
		if (err != null) err.close();
		err = null;
	}
}
