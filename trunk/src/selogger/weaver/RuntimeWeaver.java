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
	
	public RuntimeWeaver(String args) {
		if (args == null) args = "";
		String[] a = args.split(",");
		String dirname = ".";
		String weaveOption = "";
		String classDumpOption = "false";
		EventLogger.Mode mode = EventLogger.Mode.Stream;
		for (String arg: a) {
			if (arg.startsWith("output=")) {
				dirname = arg.substring("output=".length());
			} else if (arg.startsWith("weave=")) {
				weaveOption = arg.substring("weave=".length());
			} else if (arg.startsWith("dump=")) {
				classDumpOption = arg.substring("dump=".length());
			} else if (arg.startsWith("format=")) {
				String opt = arg.substring("format=".length()).toLowerCase(); 
				if (opt.startsWith("freq")) {
					mode = EventLogger.Mode.Frequency;
				} else if (opt.startsWith("fixed")) {
					mode = EventLogger.Mode.FixedSize;
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
				logger = EventLogger.initialize(outputDir, true, weaver, mode);
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
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		
		if (className.startsWith("selogger/")) return null;
		if (className.startsWith("sun/")) return null;
		if (className.startsWith("java/")) return null;
		
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