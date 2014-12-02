package selogger.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import selogger.Config;
import selogger.logging.BinaryFileListStream;
import selogger.logging.FixedSizeEventStream;
import selogger.logging.LogWriter;
import selogger.logging.VariableSizeEventStream;

public class LogDirectory {
	
	private static String suffix_variable_raw;
	private static String suffix_variable_compressed;
	private static String suffix_fixed_raw;
	private static String suffix_fixed_compressed;
	
	static {
		Config.OutputOption variable = new Config.OutputOption(Config.OutputOption.Format.Normal);
		suffix_variable_raw = variable.getSuffix();
		variable.setCompressEnabled(true);
		suffix_variable_compressed = variable.getSuffix();

		Config.OutputOption fixed = new Config.OutputOption(Config.OutputOption.Format.FixedRecord);
		suffix_fixed_raw = fixed.getSuffix();
		fixed.setCompressEnabled(true);
		suffix_fixed_compressed = fixed.getSuffix();
	}

	private File baseDir;
	private File[] logFiles;
	private EventReader reader;
	private boolean decompress;
	private int bufsize;
	private int threadCount;

	public LogDirectory(File dir) throws FileNotFoundException {
		assert dir.isDirectory(): dir.getAbsolutePath() + " is not a directory.";
		this.baseDir = dir;
		
		// Check log files in the specified directory
		SequentialFileList f = new SequentialFileList(dir, Config.OutputOption.FILENAME_EVENT_PREFIX, suffix_variable_raw);
		if (f.getFiles().length > 0) {
			logFiles = f.getFiles();
			decompress = false;
			bufsize = VariableSizeEventStream.BYTES_PER_EVENT * BinaryFileListStream.EVENTS_PER_FILE;
			reader = new VariableSizeEventReader(this);
		} else {
			f = new SequentialFileList(dir, Config.OutputOption.FILENAME_EVENT_PREFIX, suffix_variable_compressed);
			if (f.getFiles().length > 0) {
				logFiles = f.getFiles();
				decompress = true;
				bufsize = VariableSizeEventStream.BYTES_PER_EVENT * BinaryFileListStream.EVENTS_PER_FILE;
				reader = new VariableSizeEventReader(this);
			} else {
				f = new SequentialFileList(dir, Config.OutputOption.FILENAME_EVENT_PREFIX, suffix_fixed_raw);
				if (f.getFiles().length > 0) {
					logFiles = f.getFiles();
					decompress = false;
					bufsize = FixedSizeEventStream.BYTES_PER_EVENT * BinaryFileListStream.EVENTS_PER_FILE;
					reader = new FixedSizeEventReader(this);
				} else {
					f = new SequentialFileList(dir, Config.OutputOption.FILENAME_EVENT_PREFIX, suffix_fixed_compressed);
					if (f.getFiles().length > 0) {
						logFiles = f.getFiles();
						decompress = true;
						bufsize = FixedSizeEventStream.BYTES_PER_EVENT * BinaryFileListStream.EVENTS_PER_FILE;
						reader = new FixedSizeEventReader(this);
					} else {
						logFiles = new File[0];
						reader = null;
						bufsize = 0;
						throw new FileNotFoundException("No log files are found in " + dir.getAbsolutePath());
					}
				}
			}
		}

		try {
			File threadFile = new File(baseDir, LogWriter.FILENAME_THREADID);
			LineNumberReader reader = new LineNumberReader(new FileReader(threadFile));
			threadCount = Integer.parseInt(reader.readLine());
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
