package selogger.weaver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;

import selogger.logging.Logging;
import selogger.logging.IEventLogger;

/**
 * This class is the main program of SELogger as a javaagent.
 */
public class RuntimeWeaver implements ClassFileTransformer {

	/**
	 * The entry point of the agent. 
	 * This method initializes the Weaver instance and setup a shutdown hook 
	 * for releasing resources on the termination of a target program.
	 * @param agentArgs comes from command line.
	 * @param inst
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		
		final RuntimeWeaver runtimeWeaver = new RuntimeWeaver(agentArgs);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				runtimeWeaver.close();
			}
		}));
		
		if (runtimeWeaver.isValid()) {
			inst.addTransformer(runtimeWeaver);
		}
	}
	
	/**
	 * The weaver injects logging instructions into target classes.
	 */
	private Weaver weaver;
	
	/**
	 * The logger receives method calls from injected instructions via selogger.logging.Logging class.
	 */
	private IEventLogger logger;
	
	
	

	public enum Mode { Stream, Frequency, FixedSize, Discard };
	
	
	private RuntimeWeaverParameters params;

	/**
	 * Process command line arguments and prepare an output directory
	 * @param params
	 */
	public RuntimeWeaver(String args) {
		
		params = new RuntimeWeaverParameters(args);
		
		File outputDir = new File(params.getOutputDirname());
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		
		if (outputDir.isDirectory() && outputDir.canWrite()) {
			WeaveConfig config = new WeaveConfig(params.getWeaveOption());
			if (config.isValid()) {
				weaver = new Weaver(outputDir, config);
				weaver.setDumpEnabled(params.isDumpClassEnabled());
				
				switch (params.getMode()) {
				case FixedSize:
					logger = Logging.initializeLatestEventTimeLogger(outputDir, params.getBufferSize(), params.getObjectRecordingStrategy(), params.isRecordingString(), params.isRecordingExceptions(), params.isOutputJsonEnabled());
					break;
				
				case Frequency:
					logger = Logging.initializeFrequencyLogger(outputDir);
					break;
					
				case Stream:
					logger = Logging.initializeStreamLogger(outputDir, params.isRecordingString(), params.isRecordingExceptions(), weaver);
					break;
					
				case Discard:
					logger = Logging.initializeDiscardLogger();
					break;
				}
			} else {
				System.out.println("No weaving option is specified.");
				weaver = null;
			}
		} else {
			System.out.println("ERROR: " + outputDir.getAbsolutePath() + " is not writable.");
			weaver = null;
		}
	}
	
	/**
	 * @return true if the logging is executable
	 */
	public boolean isValid() {
		return weaver != null && logger != null;
	}

	/**
	 * Close data streams if necessary 
	 */
	public void close() {
		logger.close();
		weaver.close();
	}
	
	/**
	 * This method checks whether a given class is a logging target or not. 
	 * @param className specifies a class.  A package separator is "/".
	 * @return true if it is excluded from logging.
	 */
	public boolean isExcludedFromLogging(String className) {
		if (className.startsWith("selogger/") && !className.startsWith("selogger/testdata/")) return true;
		for (String ex: params.getExcludedNames()) {
			if (className.startsWith(ex)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method checks whether a given class is a logging target or not. 
	 * @param location is a loaded location (e.g. JAR or file path). 
	 * @return true if it is excluded from logging.
	 */
	public boolean isExcludedLocation(String location) {
		for (String ex: params.getExcludedLocations()) {
			if (location.contains(ex)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method is called from JVM when loading a class.
	 * This agent injects logging instructions here.
	 */
	@Override
	public synchronized byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
			
	    if (isExcludedFromLogging(className)) {
		    weaver.log("Excluded by name filter: " + className);
			return null;
		}
		
		if (protectionDomain != null) {
			CodeSource s = protectionDomain.getCodeSource();
			String l;
			if (s != null) {
				 l = s.getLocation().toExternalForm();
			} else {
				l = "(Unknown Source)";
			}

			if (isExcludedLocation(l)) {
			    weaver.log("Excluded by location filter: " + className + " loaded from " + l);
				return null;
			}
			
			if (isSecurityManagerClass(className, loader) && !params.isWeaveSecurityManagerClassEnabled()) {
				weaver.log("Excluded security manager subclass: " + className);
				return null;
			}
			
			weaver.log("Weaving executed: " + className + " loaded from " + l);
			byte[] buffer = weaver.weave(l, className, classfileBuffer, loader);

			return buffer;
		} else {
			return null;
		}
	}
	
	/**
	 * Check whether a given class is inherited from java.lang.SecurityManager or not.
	 * @param className specifies a class name.
	 * @param loader specifies a class loader.
	 * @return true if the class is a subclass of SecurityManaer.
	 */
	private boolean isSecurityManagerClass(String className, ClassLoader loader) {
		while (className != null) {
			if (className.equals("java/lang/SecurityManager")) {
				return true;
			} else if (className.equals("java/lang/Object")) {
				return false;
			}
			className = getSuperClass(className, loader);
		}
		return false;
	}
	
	/**
	 * Get a super class name of a given class
	 * @param className specifies a class name
	 * @param loader specifies a class loader to load class information
	 * @return the super class name.  
	 * Null is returnd if this method fails to load the class information
	 */
	private String getSuperClass(String className, ClassLoader loader) {
		while(loader != null) {
			InputStream is = loader.getResourceAsStream(className + ".class");
			if(is != null) {
				try {
					ClassReader r = new ClassReader(is);
					is.close();
					return r.getSuperName();
				} catch (IOException e) {
					try {
						is.close();
					} catch (IOException e2) {
					}
				}
				return null;
			}
			
			loader = loader.getParent();
		}
		return null;
	}

}
