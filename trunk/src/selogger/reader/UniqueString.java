package selogger.reader;

import java.util.HashMap;

public class UniqueString {

	private HashMap<String, String> map;
	
	public UniqueString() {
		map = new HashMap<String, String>(65536);	
	}
	
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
