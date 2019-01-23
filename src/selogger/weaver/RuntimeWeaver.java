package selogger.weaver;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import selogger.logging.EventLogger;
import selogger.logging.IEventLogger;

public class RuntimeWeaver implements ClassFileTransformer {

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
	
	private Weaver weaver;
	private IEventLogger logger;

	public enum Mode { Stream, Frequency, FixedSize, FixedSizeTimestamp, Discard };
	
	public RuntimeWeaver(String args) {
		if (args == null) args = "";
		String[] a = args.split(",");
		String dirname = ".";
		String weaveOption = "";
		String classDumpOption = "false";
		int bufferSize = 32;
		boolean keepObject = false;
		Mode mode = Mode.Stream;
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
			} else if (arg.startsWith("format=")) {
				String opt = arg.substring("format=".length()).toLowerCase(); 
				if (opt.startsWith("freq")) {
					mode = Mode.Frequency;
				} else if (opt.startsWith("discard")) {
					mode = Mode.Discard;
				} else if (opt.startsWith("latesttime")) {
					mode = Mode.FixedSizeTimestamp;
				} else if (opt.startsWith("latest")) {
					mode = Mode.FixedSize;
				} else if (opt.startsWith("fixed")) {
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
					logger = EventLogger.initializeLatestDataLogger(outputDir, bufferSize, keepObject);
					break;
					
				case FixedSizeTimestamp:
					logger = EventLogger.initializeLatestEventTimeLogger(outputDir, bufferSize, keepObject);
					break;
				
				case Frequency:
					logger = EventLogger.initializeFrequencyLogger(outputDir);
					break;
					
				case Stream:
					logger = EventLogger.initialize(outputDir, true, weaver);
					break;
					
				case Discard:
					logger = EventLogger.initializeDiscardLogger();
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
	
	public boolean isValid() {
		return weaver != null && logger != null;
	}

	
	public void close() {
		logger.close();
		weaver.close();
	}

	@Override
	public synchronized byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		
		if (className.startsWith("selogger/") && !className.startsWith("selogger/testdata/")) return null;
		if (className.startsWith("sun/")) return null;
		if (className.startsWith("com/sun/")) return null;
		if (className.startsWith("java/")) return null;
		if (className.startsWith("javax/")) return null;
		
		if (protectionDomain != null) {
			CodeSource s = protectionDomain.getCodeSource();
			String l;
			if (s != null) {
				 l = s.getLocation().toExternalForm();
			} else {
				l = "(Unknown Source)";
			}

			byte[] buffer = weaver.weave(l, className, classfileBuffer, loader);

			return buffer;
		} else {
			return null;
		}
	}

}
