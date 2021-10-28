package selogger.weaver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import selogger.logging.io.LatestEventLogger.ObjectRecordingStrategy;
import selogger.weaver.RuntimeWeaver.Mode;


/**
 * Parameters for controlling the behavior of a runtime weaver.
 */
public class RuntimeWeaverParameters {

	
	private static final String[] SYSTEM_PACKAGES =  { "sun/", "com/sun/", "java/", "javax/" };
	private static final String ARG_SEPARATOR = ",";
	private static final String SELOGGER_DEFAULT_OUTPUT_DIR = "selogger-output";

	private static Pattern timePattern = Pattern.compile(".*(\\{time:([^}]+)\\}).*"); 

	private String output_dirname = SELOGGER_DEFAULT_OUTPUT_DIR;

	private String weaveOption = WeaveConfig.KEY_RECORD_ALL;

	private boolean outputJson = false;

	/**
	 * Dump woven class files (mainly for debugging) 
	 */
	private boolean dumpClass = false;

	/**
	 * The number of events recorded per code location
	 */
	private int bufferSize = 32;
	
	/**
	 * Strategy to keep objects on memory
	 */
	private ObjectRecordingStrategy keepObject = ObjectRecordingStrategy.Strong;

	/**
	 * If true, automatic filtering for security manager classes is disabled
	 */
	private boolean weaveSecurityManagerClass = false;
	

	/**
	 * Package/class names (prefix) excluded from logging
	 */
	private ArrayList<String> excludedNames;

	/**
	 * Location names (substring) excluded from logging
	 */
	private ArrayList<String> excludedLocations;

	
	private Mode mode = Mode.FixedSize;

	public RuntimeWeaverParameters(String args) {
		if (args == null) args = "";
		String[] a = args.split(ARG_SEPARATOR);
		excludedNames = new ArrayList<String>();
		excludedLocations = new ArrayList<String>();
		for (String pkg: SYSTEM_PACKAGES) excludedNames.add(pkg);

		for (String arg: a) {
			if (arg.startsWith("output=")) {
				output_dirname = arg.substring("output=".length());
				if (output_dirname.contains("{time}")) {
					SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd-HHmmssSSS");
					output_dirname = output_dirname.replace("{time}", f.format(new Date()));
				} else {
					Matcher m = timePattern.matcher(output_dirname);
					if (m.matches()) {
						SimpleDateFormat f = new SimpleDateFormat(m.group(2));
						output_dirname = output_dirname.replace(m.group(1), f.format(new Date()));
					}
				}
			} else if (arg.startsWith("weave=")) {
				weaveOption = arg.substring("weave=".length());
			} else if (arg.startsWith("dump=")) {
				String classDumpOption = arg.substring("dump=".length());
				dumpClass = classDumpOption.equalsIgnoreCase("true");
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
					excludedNames.add(prefix);
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
	}
	
	public String getOutputDirname() {
		return output_dirname;
	}
	
	public String getWeaveOption() {
		return weaveOption;
	}
	
	public boolean isDumpClassEnabled() {
		return dumpClass;
	}
	
	public Mode getMode() {
		return mode;
	}
	
	public int getBufferSize() {
		return bufferSize;
	}
	
	public ObjectRecordingStrategy getObjectRecordingStrategy() {
		return keepObject;
	}
	
	public boolean isOutputJsonEnabled() {
		return outputJson;
	}
	
	public boolean isWeaveSecurityManagerClassEnabled() {
		return weaveSecurityManagerClass;
	}
	
	public ArrayList<String> getExcludedNames() {
		return excludedNames;
	}
	
	public ArrayList<String> getExcludedLocations() {
		return excludedLocations;
	}

}