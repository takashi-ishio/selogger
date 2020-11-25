package selogger.reader;

import java.util.HashMap;

/**
 * This object is to replace string objects having the same content with a single instance.
 */
public class UniqueString {

	private HashMap<String, String> map;
	
	/**
	 * Create an instance to manage string objects.
	 */
	public UniqueString() {
		map = new HashMap<String, String>(65536);	
	}

	/**
	 * Replace a string instance with a shared instance.
	 * @param s string object to be replaced
	 * @return a shared instance (maybe the same instance as the parameter)
	 */
	public String getSharedInstance(String s) {
		if (s != null) {
			String result = map.get(s);
			if (result == null) {
				result = s;
				map.put(s, s);
			}
			return result;
		} else {
			return null;
		}
	}
}
