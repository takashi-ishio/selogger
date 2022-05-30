package selogger.weaver;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import selogger.EventType;
import selogger.logging.ILoggingTarget;

public class DataInfoPattern implements ILoggingTarget {

	private static final String SEPARATOR = "#"; 
	private static final String EVENT_SEPARATOR = ";"; 

	private String classNamePattern;
	private String methodNamePattern;
	private String methodDescPattern;
	private Set<EventType> eventTypes;
	
	private BitSet targetIds;
	
	
	public DataInfoPattern(String patternText) {
		String[] tokens = patternText.split(SEPARATOR);
		if (tokens.length >= 1) {
			classNamePattern = tokens[0];
		} 
		if (tokens.length >= 2) {
			methodNamePattern = tokens[1];
		}
		if (tokens.length >= 3) {
			methodDescPattern = tokens[2];
		}
		if (tokens.length >= 4) {
			eventTypes = new HashSet<>();
			String[] types = tokens[3].split(EVENT_SEPARATOR);
			for (String t: types) {
				eventTypes.add(EventType.valueOf(t));
			}
		}
		targetIds = new BitSet(65536);
	}
	
	public boolean isTarget(String className, String methodName, String methodDesc, EventType type) {
		return (classNamePattern == null || className.matches(classNamePattern)) &&
			(methodNamePattern == null || methodName.matches(methodNamePattern)) &&
			(methodDescPattern == null || methodDesc.matches(methodDescPattern)) &&
			(eventTypes == null || eventTypes.isEmpty() || eventTypes.contains(type)); 
	}
	
	public void register(MethodInfo method, DataInfo item) {
		if (isTarget(method.getClassName(), method.getMethodName(), method.getMethodDesc(), item.getEventType())) {
			targetIds.set(item.getDataId());
		}
	}
	
	@Override
	public boolean isTarget(int dataid) {
		return targetIds.get(dataid);
	}
	
	
}
