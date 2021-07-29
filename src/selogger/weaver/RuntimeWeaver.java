package selogger.weaver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.objectweb.asm.ClassReader;

import selogger.logging.Logging;
import selogger.logging.io.LatestEventLogger.ObjectRecordingStrategy;
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
	
	/**
	 * Package/class names (prefix) excluded from logging
	 */
	private ArrayList<String> exclusion;
	
	/**
	 * Disable automatic filtering for security manager classes
	 */
	private boolean weaveSecurityManagerClass;
	
	/**
	 * Location names (substring) excluded from logging
	 */
	private ArrayList<String> excludedLocations;
	
	private static final String[] SYSTEM_PACKAGES =  { "sun/", "com/sun/", "java/", "javax/" };
	private static final String ARG_SEPARATOR = ",";
	private static final String SELOGGER_DEFAULT_OUTPUT_DIR = "selogger-output";

	public enum Mode { Stream, Frequency, FixedSize, Discard };

	/**
	 * Process command line arguments and prepare an output directory
	 * @param args
	 */
	public RuntimeWeaver(String args) {
		if (args == null) args = "";
		String[] a = args.split(ARG_SEPARATOR);
		String dirname = SELOGGER_DEFAULT_OUTPUT_DIR;
		String weaveOption = WeaveConfig.KEY_RECORD_ALL;
		String classDumpOption = "false";
		boolean outputJson = false;
		exclusion = new ArrayList<String>();
		excludedLocations = new ArrayList<String>();
		for (String pkg: SYSTEM_PACKAGES) exclusion.add(pkg);

		int bufferSize = 32;
		ObjectRecordingStrategy keepObject = ObjectRecordingStrategy.Strong;
		Mode mode = Mode.FixedSize;
		for (String arg: a) {
			if (arg.startsWith("output=")) {
				dirname = arg.substring("output=".length());
				if (dirname.contains("{time}")) {
					SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHHmmssSSS");
					dirname = dirname.replace("{time}", f.format(new Date()));
				}
			} else if (arg.startsWith("weave=")) {
				weaveOption = arg.substring("weave=".length());
			} else if (arg.startsWith("dump=")) {
				classDumpOption = arg.substring("dump=".length());
			} else if (arg.startsWith("size=")) {
				bufferSize = Integer.parseInt(arg.substring("size=".length()));
				if (bufferSize < 4) bufferSize = 4;
			} else if (arg.startsWith("weavesecuritymanager=")) {
				weaveSecurityManagerClass = Boolean.parseBoolean(arg.substring("weavesecuritymanager=".length()));
			} else if (arg.startsWith("json=")) {
				String param = arg.substring("json=".length());
				outputJson = param.equalsIgnoreCase("true");
			} else if (arg.startsWith("keepobj=")) {
				String param = arg.substring("keepobj=".length());
				if (param.equalsIgnoreCase("true") || param.equalsIgnoreCase("strong")) {
					keepObject = ObjectRecordingStrategy.Strong;
				} else if (param.equalsIgnoreCase("false") || param.equalsIgnoreCase("weak")) {
					keepObject = ObjectRecordingStrategy.Weak;
				} else if (param.equalsIgnoreCase("id")) {
					keepObject = ObjectRecordingStrategy.Id;
				}
			} else if (arg.startsWith("e=")) {
				String prefix = arg.substring("e=".length());
				if (prefix.length() > 0) {
					prefix = prefix.replace('.', '/');
					exclusion.add(prefix);
				}
			} else if (arg.startsWith("exlocation=")) {
				String location = arg.substring("exlocation=".length());
				if (location.length() > 0) {
					excludedLocations.add(location);
				}
			} else if (arg.startsWith("format=")) {
				String opt = arg.substring("format=".length()).toLowerCase(); 
				if (opt.startsWith("freq")) {
					mode = Mode.Frequency;
				} else if (opt.startsWith("discard")) {
					mode = Mode.Discard;
				} else if (opt.startsWith("omni")||opt.startsWith("stream")) {
					mode = Mode.Stream;
				} else if (opt.startsWith("latest")||opt.startsWith("nearomni")||opt.startsWith("near-omni")) {
					mode = Mode.FixedSize;
				}
			}
		}
		
		File outputDir = new File(dirname);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		
		if (outputDir.isDirectory() && outputDir.canWrite()) {
			WeaveConfig config = new WeaveConfig(weaveOption);
			if (config.isValid()) {
				weaver = new Weaver(outputDir, config);
				weaver.setDumpEnabled(classDumpOption.equalsIgnoreCase("true"));
				
				switch (mode) {
				case FixedSize:
					logger = Logging.initializeLatestEventTimeLogger(outputDir, bufferSize, keepObject, outputJson);
					break;
				
				case Frequency:
					logger = Logging.initializeFrequencyLogger(outputDir);
					break;
					
				case Stream:
					logger = Logging.initializeStreamLogger(outputDir, true, weaver);
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
		for (String ex: exclusion) {
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
		for (String ex: excludedLocations) {
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
			
			if (isSecurityManagerClass(className, loader) && !weaveSecurityManagerClass) {
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
