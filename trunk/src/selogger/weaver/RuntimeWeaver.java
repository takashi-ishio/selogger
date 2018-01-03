package selogger.weaver;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class RuntimeWeaver {

	public static void premain(String agentArgs, Instrumentation inst) {
		
		final Weaver w = parseArgs(agentArgs);
		if (w != null) {
			
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					w.close();
				}
			}));
			
			inst.addTransformer(new ClassFileTransformer() {
				@Override
				public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
						ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
					
					if (className.startsWith("selogger/")) return null;
					
					if (protectionDomain != null) {
						CodeSource s = protectionDomain.getCodeSource();
						String l;
						if (s != null) {
							 l = s.getLocation().toExternalForm();
						} else {
							l = "(Unknown Source)";
						}

						byte[] buffer = w.weave(l, className, classfileBuffer, loader);

						return buffer;
					} else {
						return null;
					}
				}
			});
		}
	}
	
	public static Weaver parseArgs(String args) {
		if (args == null) args = "";
		String[] a = args.split(",");
		String dirname = ".";
		String weaveOption = "";
		String verifyOption = "false";
		for (String arg: a) {
			if (arg.startsWith("output=")) {
				dirname = arg.substring("output=".length());
			} else if (arg.startsWith("weave=")) {
				weaveOption = arg.substring("weave=".length());
			} else if (arg.startsWith("verify=")) {
				verifyOption = arg.substring("verify=".length());
			}
		}
		
		Weaver weavingInfo = new Weaver(new File(dirname));
		weavingInfo.setIgnoreError(true);
		weavingInfo.setWeaveInternalJAR(false);
		weavingInfo.setJDK17(true);
		weavingInfo.setWeaveJarsInDir(false);
		weavingInfo.setVerifierEnabled(verifyOption.equalsIgnoreCase("true"));
		
		// Set a global property for logger
		System.setProperty("selogger.dir", dirname);
		System.setProperty("selogger.errorlog", new File(dirname, "err.txt").getAbsolutePath());

		boolean success = weavingInfo.setWeaveInstructions(weaveOption);
		if (!success) {
			System.out.println("No weaving option is specified.");
			return null;
		}

		return weavingInfo;
		
	}
}
