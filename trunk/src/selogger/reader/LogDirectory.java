package selogger.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import selogger.logging.EventLogger;
import selogger.logging.io.EventDataStream;
import selogger.logging.io.FileNameGenerator;

public class LogDirectory {
	

	private File baseDir;
	private File[] logFiles;
	private EventReader reader;
	private boolean decompress;
	private int bufsize;
	private int threadCount;

	public LogDirectory(File dir, LocationIdMap idmap) throws FileNotFoundException {
		assert dir.isDirectory(): dir.getAbsolutePath() + " is not a directory.";
		this.baseDir = dir;
		
		// Check log files in the specified directory
		SequentialFileList f =  new SequentialFileList(dir, FileNameGenerator.FILE_PREFIX, FileNameGenerator.FILE_SUFFIX);
		if (f.getFiles().length > 0) {
			logFiles = f.getFiles();
			decompress = false;
			bufsize = EventDataStream.BYTES_PER_EVENT * EventDataStream.MAX_EVENTS_PER_FILE;
			reader = new EventReader(this, idmap);
		} else {
			logFiles = new File[0];
			reader = null;
			bufsize = 0;
			throw new FileNotFoundException("No log files are found in " + dir.getAbsolutePath());
		}

		try (LineNumberReader reader = new LineNumberReader(new FileReader(new File(baseDir, EventLogger.FILENAME_THREADID)))) {
			threadCount = Integer.parseInt(reader.readLine());
			reader.close();
		} catch (IOException e) {
			threadCount = 0;
		} catch (NumberFormatException e) {
			threadCount = 0;
		}
		
	}
	
	public File getDirectory() {
		return baseDir;
	}
	
	public EventReader getReader() {
		return reader;
	}
	
	public int getLogFileCount() {
		return logFiles.length;
	}
	
	public File getLogFile(int index) {
		return logFiles[index];
	}
	
	public int getBufferSize() {
		return bufsize;
	}
	
	public boolean doDecompress() {
		return decompress;
	}
	
	/**
	 * @return the number of threads in an execution trace.
	 * Return 0 if the information is not available.
	 */
	public int getThreadCount() {
		return threadCount;
	}

}
