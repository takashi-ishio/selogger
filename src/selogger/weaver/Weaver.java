package selogger.weaver;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import selogger.logging.IErrorLogger;

/**
 * This class manages bytecode injection process and weaving logs.
 */
public class Weaver implements IErrorLogger {

	public static final String PROPERTY_FILE = "weaving.properties";
	public static final String SEPARATOR = ",";
	public static final char SEPARATOR_CHAR = ',';
	public static final String CLASS_ID_FILE = "classes.txt";
	public static final String METHOD_ID_FILE = "methods.txt";
	public static final String DATA_ID_FILE = "dataids.txt";
	public static final String ERROR_LOG_FILE = "log.txt";
	
	public static final String CATEGORY_WOVEN_CLASSES = "woven-classes";
	public static final String CATEGORY_ERROR_CLASSES = "error-classes";
	
	private File outputDir;
	
	private Writer dataIdWriter;
	private String lineSeparator = "\n";
	private PrintStream logger;
	private int classId;
	private int confirmedDataId;
	private int confirmedMethodId;
	private Writer methodIdWriter;

	private Writer classIdWriter;
	private boolean dumpOption;
	
	private MessageDigest digest;
	private WeaveConfig config;


	/**
	 * Set up the object to manage a weaving process. 
	 * This constructor creates files to store the information.
	 * @param outputDir
	 */
	public Weaver(File outputDir, WeaveConfig config) {
		assert outputDir.isDirectory() && outputDir.canWrite();
		
		this.outputDir = outputDir;
		this.config = config;
		confirmedDataId = 0;
		confirmedMethodId = 0;
		classId = 0;
		
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
	
	/**
	 * Record a message.
	 */
	@Override
	public void log(String msg) {
		logger.println(msg);
	}

	/**
	 * Record a runtime error.
	 */
	@Override
	public void log(Throwable e) {
		e.printStackTrace(logger);
	}
	
	/**
	 * Close files written by the weaver.
	 */
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
		config.save(new File(outputDir, PROPERTY_FILE));
	}
	
	/**
	 * Set the bytecode dump option. 
	 * @param dump If true is set, the weaver writes the woven class files to the output directory.
	 */
	public void setDumpEnabled(boolean dump) {
		this.dumpOption = dump;
	}
	
	
	/**
	 * Execute bytecode injection for a given class.
	 * @param container specifies a location (e.g. a Jar file path) where a class is loaded.
	 * @param filename specifies a class file path.
	 * @param target is the content of the class.
	 * @param loader is a class loader that loaded the class.
	 * @return a byte array containing the woven class.  This method returns null if an error occurred.  
	 */
	public byte[] weave(String container, String filename, byte[] target, ClassLoader loader) {
		assert container != null;
		 
	    log("Weave: " + filename + " loaded from " + container);
		String hash = getClassHash(target);
		LogLevel level = LogLevel.Normal;
		WeaveLog log = new WeaveLog(classId, confirmedMethodId, confirmedDataId);
		try {
			ClassTransformer c;
			try {
				c = new ClassTransformer(log, config, target, loader);
			} catch (RuntimeException e) {
				if ("Method code too large!".equals(e.getMessage())) {
					// Retry to generate a smaller bytecode by ignoring a large array init block
					try {
						log = new WeaveLog(classId, confirmedMethodId, confirmedDataId);
						level = LogLevel.IgnoreArrayInitializer;
						WeaveConfig smallerConfig = new WeaveConfig(config, level);
						c = new ClassTransformer(log, smallerConfig, target, loader);
					    log("LogLevel.IgnoreArrayInitializer: " + container + "/" + filename);
					} catch (RuntimeException e2) {
						if ("Method code too large!".equals(e.getMessage())) {
							log = new WeaveLog(classId, confirmedMethodId, confirmedDataId);
							// Retry to generate further smaller bytecode by ignoring except for entry and exit events
							level = LogLevel.OnlyEntryExit;
							WeaveConfig smallestConfig = new WeaveConfig(config, level);
							c = new ClassTransformer(log, smallestConfig, target, loader);
						    log("LogLevel.OnlyEntryExit: " + container + "/" + filename);
						} else {
							// this jumps to catch (Throwable e) in this method
							throw e2;
						}
					}
				} else {
					// this jumps to catch (Throwable e) in this method
					throw e;
				}
			}
			
			ClassInfo classIdEntry = new ClassInfo(classId, container, filename, log.getFullClassName(), level, hash, c.getClassLoaderIdentifier());
			finishClassProcess(classIdEntry, log);
			if (dumpOption) doSave(filename, c.getWeaveResult(), CATEGORY_WOVEN_CLASSES);
			return c.getWeaveResult();
			
		} catch (Throwable e) { 
			if (container != null && container.length() > 0) {
				log("Failed to weave " + filename + " in " + container);
			} else {
				log("Failed to weave " + filename);
			}
			log(e);
			if (dumpOption) doSave(filename, target, CATEGORY_ERROR_CLASSES);
			return null;
		}
	}

	/**
	 * Write the weaving result to files.
	 * Without calling this method, this object discards data when a weaving failed. 
	 * @param c records the class information. 
	 * @param result records the state after weaving.
	 */
	private void finishClassProcess(ClassInfo c, WeaveLog result) {
		if (classIdWriter != null) {
			try {
				classIdWriter.write(c.toString());
				classIdWriter.write(lineSeparator);
				classIdWriter.flush();
			} catch (IOException e) {
				e.printStackTrace(logger);
				classIdWriter = null;
			}
		}
		classId++;

		// Commit location IDs to the final output 
		confirmedDataId = result.getNextDataId();
		try {
			if (dataIdWriter != null) {
				for (DataInfo loc: result.getDataEntries()) {
					dataIdWriter.write(loc.toString());
					dataIdWriter.write(lineSeparator);
				}
				dataIdWriter.flush();
			}
		} catch (IOException e) {
			e.printStackTrace(logger);
			dataIdWriter = null;
		}
		
		// Commit method IDs to the final output
		confirmedMethodId = result.getNextMethodId();
		if (methodIdWriter != null) {
			try {
				for (MethodInfo method: result.getMethods()) {
					methodIdWriter.write(method.toString());
					methodIdWriter.write(lineSeparator);
				}
				methodIdWriter.flush();
			} catch (IOException e) {
				e.printStackTrace(logger);
				methodIdWriter = null;
			}
		}
		
	}
	

	
	/**
	 * Compute SHA-1 Hash for logging.
	 * The hash is important to identify an exact class because 
	 * multiple versions of a class may be loaded on a Java Virtual Machine.  
	 * @param targetClass byte content of a class file.
	 * @return a string representation of SHA-1 hash.
	 */
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

	/**
	 * Write a woven class into a file for a user who 
	 * would like to see the actual file (e.g. for debugging).
	 * @param name specifies a class name.
	 * @param b is the bytecode content.
	 * @param category specifies a directory name (CATEGORY_WOVEN_CLASSES, CATEGORY_ERROR_CLASSES). 
	 */
	private void doSave(String name, byte[] b, String category) {
		try {
			File classDir = new File(outputDir, category);
			File classFile = new File(classDir, name + ".class");
			classFile.getParentFile().mkdirs();
			Files.write(classFile.toPath(), b, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			log("Saved " + name + " to " + classFile.getAbsolutePath());
		} catch (IOException e) {
			log(e);
		}
	}
	
}
