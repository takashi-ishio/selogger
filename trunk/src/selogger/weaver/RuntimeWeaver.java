package selogger.weaver;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import selogger.logging.EventLogger;

public class RuntimeWeaver {

	public static void premain(String agentArgs, Instrumentation inst) {
		
		final RuntimeWeaver weaver = new RuntimeWeaver(agentArgs);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				weaver.close();
			}
		}));
		
		if (weaver.isValid()) {
			inst.addTransformer(new ClassFileTransformer() {
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
			});
		}
	}
	
	private Weaver weaver;
	private EventLogger logger;
	
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
				if (arg.substring("format=".length()).equalsIgnoreCase("freq")) {
					mode = EventLogger.Mode.Frequency;
				}
			}
		}
		
		weaver = new Weaver(new File(dirname));
		weaver.setIgnoreError(true);
		weaver.setWeaveInternalJAR(false);
		weaver.setJDK17(true);
		weaver.setWeaveJarsInDir(false);
		weaver.setVerifierEnabled(false);
		weaver.setDumpEnabled(classDumpOption.equalsIgnoreCase("true"));

		boolean success = weaver.setWeaveInstructions(weaveOption);
		if (!success) {
			System.out.println("No weaving option is specified.");
			weaver = null;
		}
		
		logger = EventLogger.initialize(weaver.getOutputDir(), true, weaver, mode);
	}
	
	public boolean isValid() {
		return weaver != null && logger != null;
	}
	
	public byte[] weave(String container, String className, byte[] bytecode, ClassLoader loader) {
		return weaver.weave(container, className, bytecode, loader);
	}
	
	public void close() {
		logger.close();
		weaver.close();
	}
}
