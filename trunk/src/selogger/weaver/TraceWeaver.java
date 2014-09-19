package selogger.weaver;

import java.io.File;
import java.util.ArrayList;


/**
 * Main class for bytecode weaving.
 * @author ishio
 *
 */
public class TraceWeaver {
	
	private static final String OPTION_JDK16 = "-jdk16";
	private static final String OPTION_JDK17 = "-jdk17";
	private static final String OPTION_IGNORE_ERROR = "-ignoreError";
	private static final String OPTION_WEAVE_INNERJAR = "-innerJAR";
	private static final String OPTION_WEAVE_JARDIR = "-jardir";
	private static final String OPTION_VERIFY = "-verify";
	private static final String OPTION_OUTPUT = "-output=";
	private static final String OPTION_WEAVE_INSTRUCTIONS = "-weave=";


	public static void main(String[] args) {
		boolean jdk17 = true;
		boolean ignoreError = false;
		boolean innerJAR = false;
		boolean jardir = false;
		boolean verify = false;
		String outputDir = ".";
		String weaveInstructions = "";
		ArrayList<String> target = new ArrayList<String>();
		for (String s: args) {
			if (s.equalsIgnoreCase(OPTION_JDK16)) {
				jdk17 = false;
			} else if (s.equalsIgnoreCase(OPTION_JDK17)) {
				jdk17 = true;
			} else if (s.equalsIgnoreCase(OPTION_IGNORE_ERROR)) {
				ignoreError = true;
			} else if (s.equalsIgnoreCase(OPTION_VERIFY)) {
				verify = true;
			} else if (s.equalsIgnoreCase(OPTION_WEAVE_INNERJAR)) {
				innerJAR = true;
			} else if (s.equalsIgnoreCase(OPTION_WEAVE_JARDIR)) {
				jardir = true;
			} else if (s.startsWith(OPTION_OUTPUT)) {
				outputDir = s.substring(OPTION_OUTPUT.length());
			} else if (s.startsWith(OPTION_WEAVE_INSTRUCTIONS)) {
				weaveInstructions = s.substring(OPTION_WEAVE_INSTRUCTIONS.length());
			} else {
				target.add(s);
			}
		}
		
		File outDir = new File(outputDir);
		if (!outDir.exists()) {
			boolean success = outDir.mkdirs();
			if (!success) {
				System.out.println("TraceWeaver could not craete a directory: " + outDir.getAbsolutePath());
				return;
			}
		} 
		if (!outDir.canWrite()) {
			System.out.println(outDir.getAbsolutePath() + " is not a writable directory.");
			return;
		}
		WeavingInfo weavingInfo = new WeavingInfo(outDir);
		weavingInfo.setIgnoreError(ignoreError);
		weavingInfo.setWeaveInternalJAR(innerJAR);
		weavingInfo.setJDK17(jdk17);
		weavingInfo.setWeaveJarsInDir(jardir);
		weavingInfo.setVerifierEnabled(verify);

		boolean success = weavingInfo.setWeaveInstructions(weaveInstructions);
		if (!success) {
			System.out.println("No weaving option is specified.");
			return;
		}

		Weaver w = new Weaver(weavingInfo);
		boolean hasFile = false;
		for (String t: target) {
			File f = new File(t);
			if (f.canRead()) {
				w.addTarget(f);
				hasFile = true;
			} else {
				System.out.println(f.getAbsolutePath() + " is not a readable file.");
			}
		}
		
		if (hasFile) {
			w.weave();
			w.close();
		} else {
			System.out.println("No target file specified.");
		}
	}


}
