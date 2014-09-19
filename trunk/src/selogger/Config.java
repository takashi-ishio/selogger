package selogger;

import java.io.File;

public class Config {

	public static class OutputOption { 
		
		public enum Format { Normal, FixedRecord, Profile; }

		public static String FILENAME_EVENT_PREFIX = "LOG$Events";

		private Format format;
		private boolean compress;
		private boolean discard;
		
		public OutputOption(Format f) {
			this.format = f;
		}
		
		public String getSuffix() {
			switch (format) {
			case Normal:
				return compress ?  ".binz" : ".bin";
			case FixedRecord:
				return compress ? ".fbinz" : ".fbin";
			case Profile:
				return ".txt";
			}
			assert false: "Unknown format";
			return "";
		}
		public void setCompressEnabled(boolean value) {
			compress = value; 
		}
		public boolean isCompressEnabled() {
			return compress;
		}
		public void setDiscardEnabled(boolean value) {
			discard = value; 
		}
		public boolean isDiscardEnabled() {
			return discard;
		}
		
		public Format getFormat() {
			return format;
		}
	}
	
	private static final String SELOGGER_OPTION = "selogger.";
	private final String lineSeparator;
	private File outputDir;
	private String errorLogFile = null;
	private String configLoadErrorMessage = null;
	private boolean outputDisabled = false;
	private int logwriterThreads;
	private boolean outputString = false;
	private OutputOption output;
	
	public Config() {
		lineSeparator = System.getProperty("line.separator");
		errorLogFile = System.getProperty(SELOGGER_OPTION + "errorlog");
		
		String stringOption = System.getProperty(SELOGGER_OPTION + "string");
		outputString = (stringOption != null) && (stringOption.equalsIgnoreCase("true") || stringOption.equalsIgnoreCase("yes"));

		String outputOption = System.getProperty(SELOGGER_OPTION +"output", "fixed-compress");

		if (outputOption != null) {
			if (outputOption.contains("fixed")) {
				output = new OutputOption(OutputOption.Format.FixedRecord);
			} else if (outputOption.contains("variable")) {			
				output = new OutputOption(OutputOption.Format.Normal);
			} else if (outputOption.contains("profile")) {
				output = new OutputOption(OutputOption.Format.Profile);
			} else {
				output = new OutputOption(OutputOption.Format.Normal);
			}
			
			if (outputOption.contains("compress")) {
				output.setCompressEnabled(true);
			} else if (outputOption.contains("discard")) {
				output.setDiscardEnabled(true);
			}
		} else {
			output = new OutputOption(OutputOption.Format.Normal);
		}
		
		String logwriterThreadString = System.getProperty(SELOGGER_OPTION + "threads");
		
		try {
			logwriterThreads = Integer.parseInt(logwriterThreadString);
			if (logwriterThreads < 0) logwriterThreads = 0;
		} catch (NumberFormatException e) {
			logwriterThreads = 0;
		}
		
		outputDir = new File(".");
		String outputPath = System.getProperty(SELOGGER_OPTION + "dir");
		if (outputPath != null) {
			outputDir = new File(outputPath);
			if (!outputDir.exists()) {
				outputDir.mkdirs();
			}
			if (!outputDir.isDirectory() || !outputDir.canWrite()) {
				configLoadErrorMessage = outputPath + " is not a writable directory.  Current directory is used for output.";
				outputDir = new File(".");
			}
		}
	}
	
	public String getLineSeparator() {
		return lineSeparator;
	}
	
	/**
	 * @return a file name.  null may be returned.
	 */
	public String getErrorLogFile() {
		return errorLogFile; 
	}
	
	/**
	 * @return null if no errors.
	 */
	public String getConfigLoadError() {
		return configLoadErrorMessage;
	}
	
	public File getOutputDir() {
		return outputDir;
	}
	
	public OutputOption getOutputOption() {
		return output;
	}
	
	public int getWriterThreadCount() {
		return logwriterThreads;
	}
	
	public boolean isLogOutputDisabled() {
		return outputDisabled;
	}
	
	public boolean isStringOutputEnabled() {
		return outputString;
	}
	
}
