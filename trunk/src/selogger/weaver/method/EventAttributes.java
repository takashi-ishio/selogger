package selogger.weaver.method;

import java.util.HashMap;
import java.util.Map;

public class EventAttributes {

	public static final String EventType = "Event"; 
	public static final String ClassName = "ClassName"; 
	public static final String MethodName = "MethodName"; 
	public static final String MethodDesc = "MethodDesc"; 
	
	
	private HashMap<String, String> attributes;
	
	public EventAttributes() {
		attributes = new HashMap<>();
	}
	
	public void put(String key, int value) {
		attributes.put(key, Integer.toString(value));
	}

	public void put(String key, String value) {
		attributes.put(key, value);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, String> keyvalue: attributes.entrySet()) {
			builder.append(keyvalue.getKey());
			builder.append(": ");
			builder.append(keyvalue.getValue());
			builder.append("; ");
		}
		return builder.toString();
	}
}
