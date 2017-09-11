package selogger.weaver;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import selogger.EventType;
import selogger.logging.SequentialFileName;
import selogger.logging.StringFileListStream;
import selogger.weaver.method.Descriptor;

public class WeavingInfo {

	public static final String PROPERTY_FILE = "weaving.properties";
	public static final String LOCATION_ID_PREFIX = "Location";
	public static final String LOCATION_ID_SUFFIX = ".txt";
	public static final String SEPARATOR = ",";
	public static final char SEPARATOR_CHAR = ',';
	public static final String CLASS_ID_FILE = "classes.txt";
	public static final String METHOD_ID_FILE = "methods.txt";
	public static final String ERROR_LOG_FILE = "log.txt";
	
	private File outputDir;
	private boolean stackMap = false;
	private boolean weaveExec = true;
	private boolean weaveMethodCall = true;
	private boolean weaveFieldAccess = true;
	private boolean weaveArray = true;
	private boolean weaveLabel = true;
	private boolean weaveMisc = true;
	private boolean weaveParameters = true;
	private boolean ignoreError = true;
	private boolean weaveInternalJAR = true;
	private boolean weaveJarsInDir = false;
	private boolean verify = false;
	
	private StringFileListStream stream;
	private String lineSeparator = "\n";
	private int dataId;
	private PrintStream logger;
	private int confirmedDataId;
	private ArrayList<String> locationIdBuffer;
	private int methodId;
	private int confirmedMethodId;
	private ArrayList<String> methodIdBuffer;
	private FileWriter methodIdWriter;

	private FileWriter classIdWriter;
	private int classId;
	


	/**
	 * Set up the object to manage a weaving process. 
	 * This constructor creates files to store the information.
	 * @param outputDir
	 */
	public WeavingInfo(File outputDir) {
		assert outputDir.isDirectory() && outputDir.canWrite();
		
		this.outputDir = outputDir;
		dataId = 1;
		confirmedDataId = 1;
		locationIdBuffer = new ArrayList<String>();
		methodId = 1;
		confirmedMethodId = 1;
		methodIdBuffer = new ArrayList<String>(); 
		classId = 1;
		
		try {
			logger = new PrintStream(new File(outputDir, ERROR_LOG_FILE)); 
		} catch (FileNotFoundException e) {
			logger = System.err;
			logger.println("Failed to open " + ERROR_LOG_FILE + " in " + outputDir.getAbsolutePath());
			logger.println("Use System.err instead.");
		}
		
		deleteExistingLocationFiles(outputDir);
		
		stream = new StringFileListStream(new SequentialFileName(outputDir, LOCATION_ID_PREFIX, LOCATION_ID_SUFFIX, 5), 1000000, 1024 * 1024, false);
		try {
			classIdWriter = new FileWriter(new File(outputDir, CLASS_ID_FILE));
			methodIdWriter = new FileWriter(new File(outputDir, METHOD_ID_FILE));
		} catch (IOException e) {
			e.printStackTrace(logger);
		}
		
	}
	
	private void deleteExistingLocationFiles(File outputDir) { 
		File[] files = outputDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(LOCATION_ID_PREFIX) && name.endsWith(LOCATION_ID_SUFFIX);
			}
		});
		for (File f: files) {
			f.delete();
		}
	}

	public void setJDK17(boolean value) {
		this.stackMap = value;
	}
	
	
	public void setLogger(PrintStream stream) {
		this.logger = stream;
	}
	
	public void log(String msg) {
		logger.println(msg);
	}

	public void log(Exception e) {
		e.printStackTrace(logger);
	}
	
	public int getClassId() {
		return classId;
	}
	
	public void finishClassProcess(String container, String filename, String className, LogLevel level, String md5) {
		if (classIdWriter != null) {
			StringBuilder buf = new StringBuilder();
			buf.append(classId);
			buf.append(SEPARATOR);
			buf.append(container);
			buf.append(SEPARATOR);
			buf.append(filename);
			buf.append(SEPARATOR);
			buf.append(className);
			buf.append(SEPARATOR);
			buf.append(level);
			buf.append(SEPARATOR);
			buf.append(md5);
			buf.append(lineSeparator);
			try {
				classIdWriter.write(buf.toString());
			} catch (IOException e) {
				e.printStackTrace(logger);
				classIdWriter = null;
			}
		}
		classId++;

		// Commit location IDs to the final output 
		confirmedDataId = dataId;
		for (String loc: locationIdBuffer) {
			stream.write(loc.toString());
		}
		locationIdBuffer.clear();
		
		// Commit method IDs to the final output
		confirmedMethodId = methodId;
		if (methodIdWriter != null) {
			try {
				for (String method: methodIdBuffer) {
					methodIdWriter.write(method);
				}
			} catch (IOException e) {
				e.printStackTrace(logger);
				methodIdWriter = null;
			}
		}
		methodIdBuffer.clear();
		
	}
	
	public void rollback() {
		dataId = confirmedDataId;
		locationIdBuffer.clear();
		methodId = confirmedMethodId;
		methodIdBuffer.clear();
	}
	
	public void startMethod(String className, String methodName, String methodDesc, int access, String sourceFileName) {
		StringBuilder buf = new StringBuilder();
		buf.append(classId);  
		buf.append(SEPARATOR);
		buf.append(methodId);  
		buf.append(SEPARATOR);
		buf.append(className);
		buf.append(SEPARATOR);
		buf.append(methodName);
		buf.append(SEPARATOR);
		buf.append(methodDesc);
		buf.append(SEPARATOR);
		buf.append(access);
		buf.append(SEPARATOR);
		if (sourceFileName != null) buf.append(sourceFileName);
		buf.append(lineSeparator);
		methodIdBuffer.add(buf.toString());
	}
	
	public void finishMethod() {
		methodId++;
	}
	
	public int nextDataId(int line, int instructionIndex, EventType eventType, Descriptor valueDesc, String attributes) {
		StringBuilder buf = new StringBuilder();
		buf.append(dataId);
		buf.append(SEPARATOR);
		buf.append(classId);
		buf.append(SEPARATOR);
		buf.append(methodId); 
		buf.append(SEPARATOR);
		buf.append(line);
		buf.append(SEPARATOR);
		buf.append(instructionIndex);
		buf.append(SEPARATOR);
		buf.append(eventType.ordinal());
		buf.append(SEPARATOR);
		buf.append(valueDesc.getNormalizedString());
		buf.append(SEPARATOR);
		buf.append(attributes);
		buf.append(lineSeparator);
		locationIdBuffer.add(buf.toString());
		return dataId++;
	}
	
	public void close() {
		try {
			if (classIdWriter != null) classIdWriter.close();
		} catch (IOException e) {
			e.printStackTrace(logger);
		}
		try {
			if (methodIdWriter != null) methodIdWriter.close();
		} catch (IOException e) {
			e.printStackTrace(logger);
		}
		stream.close();
		logger.close();
		save(new File(outputDir, PROPERTY_FILE));
	}
	
	public boolean createStackMap() {
		return stackMap;
	}
	
	public boolean recordExecution() {
		return weaveExec;
	}
	
	public boolean recordFieldAccess() {
		return weaveFieldAccess;
	}
	
	public boolean recordMiscInstructions() {
		return weaveMisc;
	}
	
	public boolean recordMethodCall() {
		return weaveMethodCall;
	}
	
	public boolean recordArrayInstructions() {
		return weaveArray;
	}
	
	public boolean recordLabel() {
		return weaveLabel;
	}
	
	public boolean recordParameters() {
		return weaveParameters;
	}
	
	public File getOutputDir() {
		return outputDir;
	}
	
	public boolean ignoreError() {
		return ignoreError;
	}
	
	public void setIgnoreError(boolean value) {
		this.ignoreError = value;
	}
	
	public void setWeaveInternalJAR(boolean value) {
		this.weaveInternalJAR = value;
	}
	
	public boolean weaveInternalJAR() {
		return weaveInternalJAR;
	}
	
	public void setWeaveJarsInDir(boolean weaveJarsInDir) {
		this.weaveJarsInDir = weaveJarsInDir;
	}
	
	public boolean weaveJarsInDir() {
		return weaveJarsInDir;
	}
	
	public void setVerifierEnabled(boolean verify) {
		this.verify = verify;
	}
	
	public boolean isVerifierEnabled() {
		return verify;
	}
	
	/**
	 * @param options
	 * @return true if at least one weaving option is enabled (except for parameter recording).
	 */
	public boolean setWeaveInstructions(String options) {
		String opt = options.toUpperCase();
		if (opt.equals(KEY_RECORD_ALL)) {
			opt = KEY_RECORD_EXEC + KEY_RECORD_CALL + KEY_RECORD_FIELD + KEY_RECORD_ARRAY + KEY_RECORD_MISC + KEY_RECORD_PARAMETERS + KEY_RECORD_LABEL;
		} else if (opt.equals(KEY_RECORD_DEFAULT)) {
			opt = KEY_RECORD_EXEC + KEY_RECORD_CALL + KEY_RECORD_FIELD + KEY_RECORD_ARRAY + KEY_RECORD_MISC + KEY_RECORD_PARAMETERS + KEY_RECORD_LABEL;
		}
		weaveExec = opt.contains(KEY_RECORD_EXEC);
		weaveMethodCall = opt.contains(KEY_RECORD_CALL);
		weaveFieldAccess = opt.contains(KEY_RECORD_FIELD);
		weaveArray = opt.contains(KEY_RECORD_ARRAY);
		weaveMisc = opt.contains(KEY_RECORD_MISC);
		weaveLabel = opt.contains(KEY_RECORD_LABEL);
		weaveParameters = opt.contains(KEY_RECORD_PARAMETERS);
		return weaveExec || weaveMethodCall || weaveFieldAccess || weaveArray || weaveMisc || weaveLabel;
	}

	public static final String KEY_RECORD_DEFAULT = "";
	public static final String KEY_RECORD_ALL = "ALL";

	private static final String KEY_LOCATION_ID = "NextLocationId"; 
	private static final String KEY_STACKMAP = "StackMap";
	private static final String KEY_LOGGER = "Logger";
	private static final String VALUE_LOGGER_STDOUT = "#STDOUT";
	private static final String VALUE_LOGGER_STDERR = "#STDERR";
	private static final String KEY_RECORD = "Events";
	private static final String KEY_RECORD_SEPARATOR = ",";
	private static final String KEY_RECORD_EXEC = "EXEC";
	private static final String KEY_RECORD_CALL = "CALL";
	private static final String KEY_RECORD_FIELD = "FIELD";
	private static final String KEY_RECORD_ARRAY = "ARRAY";
	private static final String KEY_RECORD_MISC = "MISC";
	private static final String KEY_RECORD_LABEL = "LABEL";
	private static final String KEY_RECORD_PARAMETERS = "PARAM";
	
	public void save(File propertyFile) {
		ArrayList<String> events = new ArrayList<String>();
		if (weaveExec) events.add(KEY_RECORD_EXEC);
		if (weaveMethodCall) events.add(KEY_RECORD_CALL);
		if (weaveFieldAccess) events.add(KEY_RECORD_FIELD);
		if (weaveArray) events.add(KEY_RECORD_ARRAY);
		if (weaveMisc) events.add(KEY_RECORD_MISC);
		if (weaveLabel) events.add(KEY_RECORD_LABEL);
		if (weaveParameters) events.add(KEY_RECORD_PARAMETERS);
		StringBuilder eventsString = new StringBuilder();
		for (int i=0; i<events.size(); ++i) {
			if (i>0) eventsString.append(KEY_RECORD_SEPARATOR);
			eventsString.append(events.get(i));
		}
		
		Properties prop = new Properties();
		prop.setProperty(KEY_LOCATION_ID, Long.toString(dataId));
		if (logger == System.out) prop.setProperty(KEY_LOGGER, VALUE_LOGGER_STDOUT);
		if (logger == System.err) prop.setProperty(KEY_LOGGER, VALUE_LOGGER_STDERR); // if out==err then use err.
		prop.setProperty(KEY_RECORD, eventsString.toString());
		prop.setProperty(KEY_STACKMAP, Boolean.toString(stackMap));
		
		try {
			FileOutputStream out = new FileOutputStream(propertyFile);
			prop.storeToXML(out, "Generated: " + new Date().toString(), "UTF-8");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
