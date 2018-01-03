package selogger.weaver;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

import selogger.EventType;
import selogger.logging.IErrorLogger;
import selogger.weaver.method.Descriptor;

public class Weaver implements IErrorLogger {

	public static final String PROPERTY_FILE = "weaving.properties";
	public static final String SEPARATOR = ",";
	public static final char SEPARATOR_CHAR = ',';
	public static final String CLASS_ID_FILE = "classes.txt";
	public static final String METHOD_ID_FILE = "methods.txt";
	public static final String DATA_ID_FILE = "dataids.txt";
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
	private boolean weaveLocalAccess = true;
	private boolean ignoreError = true;
	private boolean weaveInternalJAR = true;
	private boolean weaveJarsInDir = false;
	private boolean verify = false;
	
	private Writer dataIdWriter;
	private String lineSeparator = "\n";
	private int dataId;
	private PrintStream logger;
	private int confirmedDataId;
	private ArrayList<String> locationIdBuffer;
	private int methodId;
	private int confirmedMethodId;
	private ArrayList<String> methodIdBuffer;
	private Writer methodIdWriter;

	private Writer classIdWriter;
	private int classId;
	
	private boolean dumpOption;
	
	private MessageDigest digest;


	/**
	 * Set up the object to manage a weaving process. 
	 * This constructor creates files to store the information.
	 * @param outputDir
	 */
	public Weaver(File outputDir) {
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
		
		try {
			classIdWriter = new BufferedWriter(new FileWriter(new File(outputDir, CLASS_ID_FILE)));
			methodIdWriter = new BufferedWriter(new FileWriter(new File(outputDir, METHOD_ID_FILE)));
			dataIdWriter = new BufferedWriter(new FileWriter(new File(outputDir, DATA_ID_FILE)));
		} catch (IOException e) {
			e.printStackTrace(logger);
		}
		
		try {
			this.digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			this.digest = null;
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

	public void log(Throwable e) {
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
				classIdWriter.flush();
			} catch (IOException e) {
				e.printStackTrace(logger);
				classIdWriter = null;
			}
		}
		classId++;

		// Commit location IDs to the final output 
		confirmedDataId = dataId;
		try {
			if (dataIdWriter != null) {
				for (String loc: locationIdBuffer) {
					dataIdWriter.write(loc.toString());
				}
				dataIdWriter.flush();
			}
		} catch (IOException e) {
			e.printStackTrace(logger);
			dataIdWriter = null;
		}
		locationIdBuffer.clear();
		
		// Commit method IDs to the final output
		confirmedMethodId = methodId;
		if (methodIdWriter != null) {
			try {
				for (String method: methodIdBuffer) {
					methodIdWriter.write(method);
				}
				methodIdWriter.flush();
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
		try {
			if (dataIdWriter != null) dataIdWriter.close();
		} catch (IOException e) {
			e.printStackTrace(logger);
		}
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
	
	public boolean recordLocalAccess() {
		return weaveLocalAccess;
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
	
	public void setDumpEnabled(boolean dump) {
		this.dumpOption = dump;
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
			opt = KEY_RECORD_EXEC + KEY_RECORD_CALL + KEY_RECORD_FIELD + KEY_RECORD_ARRAY + KEY_RECORD_MISC + KEY_RECORD_PARAMETERS + KEY_RECORD_LABEL + KEY_RECORD_LOCAL;
		} else if (opt.equals(KEY_RECORD_DEFAULT)) {
			opt = KEY_RECORD_EXEC + KEY_RECORD_CALL + KEY_RECORD_FIELD + KEY_RECORD_ARRAY + KEY_RECORD_MISC + KEY_RECORD_PARAMETERS;
		}
		weaveExec = opt.contains(KEY_RECORD_EXEC);
		weaveMethodCall = opt.contains(KEY_RECORD_CALL);
		weaveFieldAccess = opt.contains(KEY_RECORD_FIELD);
		weaveArray = opt.contains(KEY_RECORD_ARRAY);
		weaveMisc = opt.contains(KEY_RECORD_MISC);
		weaveLabel = opt.contains(KEY_RECORD_LABEL);
		weaveParameters = opt.contains(KEY_RECORD_PARAMETERS);
		weaveLocalAccess = opt.contains(KEY_RECORD_LOCAL);
		return weaveExec || weaveMethodCall || weaveFieldAccess || weaveArray || weaveMisc || weaveParameters || weaveLocalAccess || weaveLabel;
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
	private static final String KEY_RECORD_LOCAL = "LOCAL";
	
	public void save(File propertyFile) {
		ArrayList<String> events = new ArrayList<String>();
		if (weaveExec) events.add(KEY_RECORD_EXEC);
		if (weaveMethodCall) events.add(KEY_RECORD_CALL);
		if (weaveFieldAccess) events.add(KEY_RECORD_FIELD);
		if (weaveArray) events.add(KEY_RECORD_ARRAY);
		if (weaveMisc) events.add(KEY_RECORD_MISC);
		if (weaveLabel) events.add(KEY_RECORD_LABEL);
		if (weaveParameters) events.add(KEY_RECORD_PARAMETERS);
		if (weaveLocalAccess) events.add(KEY_RECORD_LOCAL);
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
	
	public byte[] weave(String container, String filename, byte[] target, ClassLoader loader) {
		assert container != null;
		
		String hash = getClassHash(target);
		LogLevel level = LogLevel.Normal;
		try {
			ClassTransformer c;
			try {
				c = new ClassTransformer(this, target, level, loader);
			} catch (RuntimeException e) {
				if ("Method code too large!".equals(e.getMessage())) {
					// Retry to generate a smaller bytecode by ignoring a large array init block
					try {
						rollback();
						level = LogLevel.IgnoreArrayInitializer;
						c = new ClassTransformer(this, target, level, loader);
					    log("LogLevel.IgnoreArrayInitializer: " + container + "/" + filename);
					} catch (RuntimeException e2) {
						if ("Method code too large!".equals(e.getMessage())) {
							// Retry to generate further smaller bytecode by ignoring except for entry and exit events
							rollback();
							level = LogLevel.OnlyEntryExit;
							c = new ClassTransformer(this, target, level, loader);
						    log("LogLevel.OnlyEntryExit: " + container + "/" + filename);
						} else {
							throw e2;
						}
					}
				} else {
					throw e;
				}
			}
			
			finishClassProcess(container, filename, c.getFullClassName(), level, hash);
			if (dumpOption) doSave(filename, c.getWeaveResult());
			doVerification(filename, c.getWeaveResult());
			return c.getWeaveResult();
			
		} catch (Throwable e) { 
			rollback();
			if (container != null && container.length() > 0) {
				log("Failed to weave " + filename + " in " + container);
			} else {
				log("Failed to weave " + filename);
			}
			log(e);
			finishClassProcess(container, filename, "", LogLevel.Failed, hash);
			return null;
		}
	}

	
	private String getClassHash(byte[] targetClass) {
		if (digest != null) {
			byte[] hash = digest.digest(targetClass);
			StringBuilder hex = new StringBuilder(hash.length * 2);
			for (byte b: hash) {
				String l = "0" + Integer.toHexString(b);
				hex.append(l.substring(l.length() - 2));
			}
			return hex.toString();
		} else {
			return "";
		}
	}

	private void doSave(String name, byte[] b) {
		try {
			File classDir = new File(outputDir, "woven-classes");
			File classFile = new File(classDir, name + ".class");
			classFile.getParentFile().mkdirs();
			Files.write(classFile.toPath(), b, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			log("Saved " + name + " to " + classFile.getAbsolutePath());
		} catch (IOException e) {
			log(e);
		}
	}
	
	private void doVerification(String name, byte[] b) {
		if (isVerifierEnabled()) {
			StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
		    CheckClassAdapter.verify(new ClassReader(b), true, pw); // this method is not expected to throw an exception
		    log("VERIFICATION " + name);
		    log(sw.toString());
		}
	}

	
}
