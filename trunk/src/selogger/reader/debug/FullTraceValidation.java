package selogger.reader.debug;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import selogger.EventType;
import selogger.logging.io.EventDataStream;
import selogger.reader.Event;
import selogger.reader.EventReader;
import selogger.reader.LocationIdMap;
import selogger.reader.LogDirectory;
import selogger.reader.MethodInfo;

public class FullTraceValidation {

	/**
	 * Validate a sequence of events 
	 * @param args specify a directory which contains Location ID Files.
	 */
	public static void main(String[] args) {
		long time = System.currentTimeMillis();
		long events = 0;
		LogDirectory r = null;
		try {
			FullTraceValidation validator = new FullTraceValidation(args[0]);
			r = new LogDirectory(new File(args[0]), validator.locationIdMap);
			EventReader reader = r.getReader();
			reader.setProcessParams(true);
			int count = 0;
			for (Event e = reader.readEvent(); e != null; e = reader.readEvent()) {
				if (e.getEventId() % EventDataStream.MAX_EVENTS_PER_FILE == 0) System.out.print(".");
				if (e.getParams() != null) {
					count += e.getParams().size();
					assert e.getParamCount() == e.getParams().size();
					for (int i=0; i<e.getParams().size()-1; ++i) {
						assert e.getParams().get(i).getParamIndex() + 1 == e.getParams().get(i+1).getParamIndex(): "Param is incorrectly ordered."; 
					}
				}
				events++;
				validator.processNextEvent(e);
			}
			System.out.println();
			System.out.println(count);
			validator.reportResult();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Events processed: " + events);
		System.out.println("Time consumed: " + (System.currentTimeMillis() - time));
	}
	
	private ArrayList<ThreadState> threadState;
	private CallStackSet stacks;
	private LocationIdMap locationIdMap;
	
	public FullTraceValidation(String dir) throws IOException {
		threadState = new ArrayList<ThreadState>();
		stacks = new CallStackSet();
		locationIdMap = new LocationIdMap(new File(dir));
	}
	
	public void processNextEvent(Event e) {
		// Check entry-exit separately from other events.
		if (e.getEventType() == EventType.METHOD_ENTRY ||
				e.getEventType() == EventType.METHOD_NORMAL_EXIT ||
				e.getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT_LABEL) {
			
			MethodInfo m = locationIdMap.getMethodInfo(e.getLocationId());
			if (e.getEventType() == EventType.METHOD_ENTRY) {
				stacks.processEnter(e.getEventId(), e.getThreadId(), m);
			} else {
				stacks.processExit(e.getEventId(), e.getThreadId(), m);
			}
		}
		
		// Check call-return, entry-exit events.
		int thread = e.getThreadId();
		while (threadState.size() <= thread) {
			threadState.add(null);
		}
		if (threadState.get(thread) != null) {
			ThreadState s = threadState.get(thread);
			s.processEvent(e);
		} else {
			ThreadState s = new ThreadState();
			threadState.set(thread, s);
			s.processEvent(e);
		}
	}
	
	public void reportResult() {
		for (CallStack c: stacks) {
			int entryCount = c.size();
			System.out.println("Thread " + c.getThreadId());
			System.out.print("  Method signature: ");
			for (int i=0; i<entryCount; ++i) {
				System.out.print(c.getMethodOnStack(i));
				System.out.print("  ");
			}
			System.out.println();
		}
	}
	
	
	private class ThreadState {

		private Stack<Event> events;
		
		public ThreadState() {
			events = new Stack<Event>();
		}
		
		private boolean mayCauseException(Event e) {
			return (e.getEventType() == EventType.CALL);
		}
		
		private Event popDanglingEntry(Event currentEvent) {
			MethodInfo currentMethod = locationIdMap.getMethodInfo(currentEvent.getLocationId());
			Event top = events.pop();
			MethodInfo topMethod = locationIdMap.getMethodInfo(top.getLocationId());
			while (topMethod != currentMethod) {
				assert topMethod.getMethodName().equals("<init>"): "Unknown case of dangling entry event: " + topMethod.toString();
				assert top.getEventType() == EventType.METHOD_ENTRY || top.getEventType() == EventType.CALL: "Unknown case of dangling entry event: " + top.toString();
				top = events.pop();
				topMethod = locationIdMap.getMethodInfo(top.getLocationId());
			}
			return top;
		}
		
		public void processEvent(Event e) {
			switch (e.getEventType()) {
			case FORMAL_PARAM:
				Event e2 = events.lastElement();
				assert e2.getEventType() == EventType.METHOD_ENTRY: "ENTRY-FORMAL";
				break;
			case ACTUAL_PARAM:
				Event caller2 = events.lastElement();
				assert caller2.getEventType() == EventType.CALL: "CALL-ACTUAL";
				break;
				
			case METHOD_NORMAL_EXIT:
			case METHOD_EXCEPTIONAL_EXIT_LABEL:
			case METHOD_EXCEPTIONAL_EXIT:
				Event top = popDanglingEntry(e);
				if (mayCauseException(top)) top = events.pop(); 
				// Here, the top event must be an entry corresponding to the exit. 
				assert top.getEventType() == EventType.METHOD_ENTRY: "Entry-Exit";
				break;
			case CALL_RETURN:
				Event caller = popDanglingEntry(e);
				assert caller.getEventType() == EventType.CALL && caller.getLocationId() == e.getLocationId(): "CALL-RETURN";
				break;
			case CATCH: // When an exception is caught, remove relevant events from a call stack.
				Event c = popDanglingEntry(e);
				if (!mayCauseException(c)) events.push(c); // If the event is not related to an exception, keep the event on the stack
				break;
				
			case THROW:
				// ignore the event since the method is handled with exceptional exit.
				break;
			
			case LOCAL_LOAD:
			case LOCAL_STORE:
			case JUMP:
			case GET_INSTANCE_FIELD:
			case GET_INSTANCE_FIELD_RESULT:
			case GET_STATIC_FIELD:
			case ARRAY_LOAD:
			case ARRAY_LOAD_INDEX:
			case ARRAY_LOAD_RESULT:
			case ARRAY_LOAD_FAIL:
			case ARRAY_STORE:
			case ARRAY_STORE_INDEX:
			case ARRAY_STORE_VALUE:
			case LABEL:
			case MONITOR_ENTER:
			case MONITOR_EXIT:
			case MULTI_NEW_ARRAY_OWNER:
			case MULTI_NEW_ARRAY_ELEMENT:
			case NEW_OBJECT:
			case NEW_OBJECT_INITIALIZED:
			case NEW_OBJECT_CREATION_COMPLETED:
			case PUT_INSTANCE_FIELD:
			case PUT_INSTANCE_FIELD_VALUE:
			case PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION:
			case PUT_STATIC_FIELD:
			case NEW_ARRAY:
			case NEW_ARRAY_RESULT:
			case MULTI_NEW_ARRAY:
			case ARRAY_LENGTH:
			case ARRAY_LENGTH_RESULT:
			case INSTANCEOF:
			case INSTANCEOF_RESULT:
			case CONSTANT_OBJECT_LOAD:
			case RET:
				// ignore the event  
				break;
				
			case METHOD_ENTRY:
			case CALL:
				events.push(e);
				break;

			case RESERVED:
				assert false: "Reserved Event was found.";
			
//			default:
//				assert false: "The unknown event: " + e.getEventType();
			}
			
		}
	}
	
}
