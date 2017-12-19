package selogger.weaver;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class RuntimeWeaver {

	public static void premain(String agentArgs, Instrumentation inst) {
		
		WeavingInfo info = parseArgs(agentArgs);
		if (info != null) {
			final Weaver w = new Weaver(info);
			
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
					
					if (protectionDomain != null) {
						CodeSource s = protectionDomain.getCodeSource();
						String l;
						if (s != null) {
							 l = s.getLocation().toExternalForm();
						} else {
							l = "(Unknown Source)";
						}

						byte[] buffer = w.weave(l, className, classfileBuffer);
						
						return buffer;
					} else {
						return null;
					}
				}
			});
		}
	}
	
	public static WeavingInfo parseArgs(String args) {
		if (args == null) args = "";
		String[] a = args.split(",");
		String dirname = ".";
		String weaveOption = "";
		for (String arg: a) {
			if (arg.startsWith("output=")) {
				dirname = arg.substring("output=".length());
			} else if (arg.startsWith("weave=")) {
				weaveOption = arg.substring("weave=".length());
			}
		}
		
		WeavingInfo weavingInfo = new WeavingInfo(new File(dirname));
		weavingInfo.setIgnoreError(true);
		weavingInfo.setWeaveInternalJAR(false);
		weavingInfo.setJDK17(true);
		weavingInfo.setWeaveJarsInDir(false);
		weavingInfo.setVerifierEnabled(false);
		
		// Set a global property for logger
		System.setProperty("selogger.dir", dirname);

		boolean success = weavingInfo.setWeaveInstructions(weaveOption);
		if (!success) {
			System.out.println("No weaving option is specified.");
			return null;
		}

		return weavingInfo;
		
	}
}
