package selogger.weaver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import selogger.logging.IErrorLogger;

public class LogMessageFile implements IErrorLogger {

	private PrintStream logger;
	
	public LogMessageFile(File errorLog) {
		try {
			if (errorLog != null) {
				logger = new PrintStream(errorLog); 
			}
		} catch (FileNotFoundException e) {
		}
	}
	
	/**
	 * Record a message.
	 */
	@Override
	public void log(String msg) {
		if (logger != null) {
			logger.println(msg);
		}
	}

	/**
	 * Record a runtime error.
	 */
	@Override
	public void log(Throwable e) {
		if (logger != null) {
			e.printStackTrace(logger);
		}
	}
	
	@Override
	public void close() {
		if (logger != null) {
			logger.close();
		}
	}
	
}
