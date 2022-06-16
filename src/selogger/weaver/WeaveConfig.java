package selogger.weaver;

import java.util.ArrayList;

/**
 * This object manages options passed to the weaver.
 * This configuration controls the entire weaving process. 
 */
public class WeaveConfig {

	private boolean weaveExec = true;
	private boolean weaveMethodCall = true;
	private boolean weaveFieldAccess = true;
	private boolean weaveArray = true;
	private boolean weaveLabel = true;
	private boolean weaveSynchronization = true;
	private boolean weaveParameters = true;
	private boolean weaveLocalAccess = true;
	private boolean weaveObject = true;
	private boolean weaveLineNumber = true;
	private boolean ignoreArrayInitializer = false;

	private boolean weaveNone = false;

	public static final String KEY_RECORD_DEFAULT = "";
	public static final String KEY_RECORD_ALL = "ALL";
	public static final String KEY_RECORD_DEFAULT_PLUS_LOCAL = "EXEC+CALL+FIELD+ARRAY+SYNC+OBJECT+PARAM+LOCAL";
	private static final String KEY_RECORD_SEPARATOR = ",";
	public static final String KEY_RECORD_NONE = "NONE";
	
	public static final String KEY_RECORD_EXEC = "EXEC";
	public static final String KEY_RECORD_CALL = "CALL";
	public static final String KEY_RECORD_FIELD = "FIELD";
	public static final String KEY_RECORD_ARRAY = "ARRAY";
	public static final String KEY_RECORD_SYNC = "SYNC";
	public static final String KEY_RECORD_OBJECT = "OBJECT";
	public static final String KEY_RECORD_LABEL = "LABEL";
	public static final String KEY_RECORD_PARAMETERS = "PARAM";
	public static final String KEY_RECORD_LOCAL = "LOCAL";
	public static final String KEY_RECORD_LINE = "LINE";
	
	/**
	 * Construct a configuration from string
	 * @param options specify a string including: EXEC, CALL, FIELD, ARRAY, SYNC, OBJECT, LABEL, PARAM, LOCAL, and NONE.
	 * @return true if at least one weaving option is enabled (except for parameter recording).
	 */
	public WeaveConfig(String options) {
		String opt = options.toUpperCase();
		if (opt.equals(KEY_RECORD_ALL)) {
			opt = KEY_RECORD_EXEC + KEY_RECORD_CALL + KEY_RECORD_FIELD + KEY_RECORD_ARRAY + KEY_RECORD_SYNC + KEY_RECORD_OBJECT + KEY_RECORD_PARAMETERS + KEY_RECORD_LABEL + KEY_RECORD_LOCAL + KEY_RECORD_LINE;
		} else if (opt.equals(KEY_RECORD_DEFAULT)) {
			opt = KEY_RECORD_EXEC + KEY_RECORD_CALL + KEY_RECORD_FIELD + KEY_RECORD_ARRAY + KEY_RECORD_SYNC + KEY_RECORD_OBJECT + KEY_RECORD_PARAMETERS;
		} else if (opt.equals(KEY_RECORD_NONE)) {
			opt = "";
			weaveNone = true;
		}
		weaveExec = opt.contains(KEY_RECORD_EXEC);
		weaveMethodCall = opt.contains(KEY_RECORD_CALL);
		weaveFieldAccess = opt.contains(KEY_RECORD_FIELD);
		weaveArray = opt.contains(KEY_RECORD_ARRAY);
		weaveSynchronization = opt.contains(KEY_RECORD_SYNC);
		weaveLabel = opt.contains(KEY_RECORD_LABEL);
		weaveParameters = opt.contains(KEY_RECORD_PARAMETERS);
		weaveLocalAccess = opt.contains(KEY_RECORD_LOCAL);
		weaveObject = opt.contains(KEY_RECORD_OBJECT);
		weaveLineNumber = opt.contains(KEY_RECORD_LINE);
		ignoreArrayInitializer = false;
	}

	/**
	 * A copy constructor with a constraint.
	 * @param config
	 */
	public WeaveConfig(WeaveConfig parent, LogLevel level) {
		this.weaveExec = parent.weaveExec;
		this.weaveMethodCall = parent.weaveMethodCall;
		this.weaveFieldAccess = parent.weaveFieldAccess;
		this.weaveArray = parent.weaveArray;
		this.weaveSynchronization = parent.weaveSynchronization;
		this.weaveLabel = parent.weaveLabel;
		this.weaveParameters = parent.weaveParameters;
		this.weaveLocalAccess = parent.weaveLocalAccess;
		this.weaveLineNumber = parent.weaveLineNumber;
		this.ignoreArrayInitializer = parent.ignoreArrayInitializer;
		this.weaveNone = parent.weaveNone;
		if (level == LogLevel.IgnoreArrayInitializer) {
			this.ignoreArrayInitializer = true;
		} else if (level == LogLevel.OnlyEntryExit) {
			this.weaveMethodCall = false;
			this.weaveFieldAccess = false;
			this.weaveArray = false;
			this.weaveSynchronization = false;
			this.weaveLabel = false;
			this.weaveParameters = false;
			this.weaveLocalAccess = false;
			this.weaveObject = false;
			this.weaveLineNumber = false;
		}
	}
	
	/**
	 * @return true if the weaver is configured to record some events or 
	 * explicitly configured to record no events.
	 */
	public boolean isValid() {
		return weaveNone || weaveExec || weaveMethodCall || weaveFieldAccess || weaveArray || weaveSynchronization || weaveParameters || weaveLocalAccess || weaveLabel || weaveLineNumber;
	}

	/**
	 * @return true if the weaver should record method execution events 
	 * such as ENTRY and EXIT observed in the callee side.
	 */
	public boolean recordExecution() {
		return weaveExec;
	}
	
	/**
	 * @return true if the weaver should record synchronized block events 
	 */
	public boolean recordSynchronization() {
		return weaveSynchronization;
	}
	
	/**
	 * @return true if the weaver should record field access events 
	 */
	public boolean recordFieldAccess() {
		return weaveFieldAccess;
	}
	
	/**
	 * @return true if the weaver should record method execution events 
	 * such as CALL observed in the caller side.
	 */
	public boolean recordMethodCall() {
		return weaveMethodCall;
	}
	
	/**
	 * @return true if the weaver should record array manipulation events.
	 */
	public boolean recordArrayInstructions() {
		return weaveArray;
	}
	
	/**
	 * @return true if the weaver should record LABEL (control-flow) events.
	 */
	public boolean recordLabel() {
		return weaveLabel;
	}
	
	/**
	 * @return true if the weaver should record method parameters.
	 */
	public boolean recordParameters() {
		return weaveParameters;
	}
	
	/**
	 * @return true if the weaver should record local access events.
	 */
	public boolean recordLocalAccess() {
		return weaveLocalAccess;
	}
	
	/**
	 * @return true if the weaver should record line number events.
	 */
	public boolean recordLineNumber() {
		return weaveLineNumber;
	}
	
	/**
	 * @return true if the weaving should ignore array initializers 
	 * (due to the size of the target class file).  
	 */
	public boolean ignoreArrayInitializer() {
		return ignoreArrayInitializer;
	}
	
	/**
	 * @return true if the weaver should record CATCH events.  
	 */
	public boolean recordCatch() {
		return recordMethodCall() || 
				recordFieldAccess() || 
				recordArrayInstructions() ||
				recordLabel() ||
				recordSynchronization();
	}
	
	/**
	 * @return true if the weaver should record OBJECT events.  
	 */
	public boolean recordObject() {
		return weaveObject;
	}
	

	/**
	 * @return a string representation of the weaving configuration
	 */
	public String toString() {
		ArrayList<String> events = new ArrayList<String>();
		if (weaveExec) events.add(KEY_RECORD_EXEC);
		if (weaveMethodCall) events.add(KEY_RECORD_CALL);
		if (weaveFieldAccess) events.add(KEY_RECORD_FIELD);
		if (weaveArray) events.add(KEY_RECORD_ARRAY);
		if (weaveSynchronization) events.add(KEY_RECORD_SYNC);
		if (weaveLabel) events.add(KEY_RECORD_LABEL);
		if (weaveParameters) events.add(KEY_RECORD_PARAMETERS);
		if (weaveLocalAccess) events.add(KEY_RECORD_LOCAL);
		if (weaveObject) events.add(KEY_RECORD_OBJECT);
		if (weaveLineNumber) events.add(KEY_RECORD_LINE);
		if (weaveNone) events.add(KEY_RECORD_NONE);
		StringBuilder eventsString = new StringBuilder();
		for (int i=0; i<events.size(); ++i) {
			if (i>0) eventsString.append(KEY_RECORD_SEPARATOR);
			eventsString.append(events.get(i));
		}
		return eventsString.toString();
	}

}
