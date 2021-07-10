package selogger.weaver;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;

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
	
	/**
	 * Package/class names (prefix) excluded from logging
	 */
	private ArrayList<String> exclusion;
	
	/**
	 * Location names (substring) excluded from logging
	 */
	private ArrayList<String> excludedLocations;
	
	private static final String[] SYSTEM_PACKAGES =  { "sun/", "com/sun/", "java/", "javax/" };
	private static final String ARG_SEPARATOR = ",";
	private static final String SELOGGER_DEFAULT_OUTPUT_DIR = "selogger-output";

	public enum Mode { Stream, Frequency, FixedSize, FixedSizeTimestamp, Discard };

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
		exclusion = new ArrayList<String>();
		excludedLocations = new ArrayList<String>();
		for (String pkg: SYSTEM_PACKAGES) exclusion.add(pkg);

		int bufferSize = 32;
		boolean keepObject = true;
		Mode mode = Mode.FixedSizeTimestamp;
		for (String arg: a) {
			if (arg.startsWith("output=")) {
				dirname = arg.substring("output=".length());
			} else if (arg.startsWith("weave=")) {
				weaveOption = arg.substring("weave=".length());
			} else if (arg.startsWith("dump=")) {
				classDumpOption = arg.substring("dump=".length());
			} else if (arg.startsWith("size=")) {
				bufferSize = Integer.parseInt(arg.substring("size=".length()));
				if (bufferSize < 4) bufferSize = 4;
			} else if (arg.startsWith("keepobj=")) {
				keepObject = Boolean.parseBoolean(arg.substring("keepobj=".length()));
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
					mode = Mode.FixedSizeTimestamp;
				} else if (opt.startsWith("latest-simple")||opt.startsWith("fixed")) {
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
					logger = Logging.initializeLatestDataLogger(outputDir, bufferSize, keepObject);
					break;
					
				case FixedSizeTimestamp:
					logger = Logging.initializeLatestEventTimeLogger(outputDir, bufferSize, keepObject);
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

			weaver.log("Weaving executed: " + className + " loaded from " + l);
			byte[] buffer = weaver.weave(l, className, classfileBuffer, loader);

			return buffer;
		} else {
			return null;
		}
	}

}
