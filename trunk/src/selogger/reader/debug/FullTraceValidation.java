package selogger.reader.debug;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import selogger.Config;
import selogger.EventId;
import selogger.logging.BinaryFileListStream;
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
		Config config = new Config();
		long time = System.currentTimeMillis();
		long events = 0;
		LogDirectory r = null;
		try {
			r = new LogDirectory(config.getOutputDir());
			EventReader reader = r.getReader();
			FullTraceValidation validator = new FullTraceValidation(args[0]);
			reader.setProcessParams(true);
			int count = 0;
			for (Event e = reader.readEvent(); e != null; e = reader.readEvent()) {
				if (e.getEventId() % BinaryFileListStream.EVENTS_PER_FILE == 0) System.out.print(".");
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
	
	private TLongObjectHashMap<ThreadState> threadState;
	private CallStackSet stacks;
	private LocationIdMap locationIdMap;
	
	public FullTraceValidation(String dir) throws IOException {
		threadState = new TLongObjectHashMap<ThreadState>();
		stacks = new CallStackSet();
		locationIdMap = new LocationIdMap(new File(dir));
	}
	
	public void processNextEvent(Event e) {
		// Check entry-exit separately from other events.
		if (e.getEventType() == EventId.EVENT_METHOD_ENTRY ||
				e.getEventType() == EventId.EVENT_METHOD_NORMAL_EXIT ||
				e.getEventType() == EventId.EVENT_METHOD_EXCEPTIONAL_EXIT) {
			
			MethodInfo m = locationIdMap.getMethodInfo(e.getLocationId());
			if (e.getEventType() == EventId.EVENT_METHOD_ENTRY) {
				stacks.processEnter(e.getEventId(), e.getThreadId(), m);
			} else {
				stacks.processExit(e.getEventId(), e.getThreadId(), m);
			}
		}
		
		// Check call-return, entry-exit events.
		long thread = e.getThreadId();
		if (threadState.containsKey(thread)) {
			ThreadState s = threadState.get(thread);
			s.processEvent(e);
		} else {
			ThreadState s = new ThreadState();
			threadState.put(thread, s);
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
			return (e.getEventType() == EventId.EVENT_METHOD_CALL);
		}
		
		private Event popDanglingEntry(Event currentEvent) {
			MethodInfo currentMethod = locationIdMap.getMethodInfo(currentEvent.getLocationId());
			Event top = events.pop();
			MethodInfo topMethod = locationIdMap.getMethodInfo(top.getLocationId());
			while (topMethod != currentMethod) {
				assert topMethod.getMethodName().equals("<init>"): "Unknown case of dangling entry event: " + topMethod.toString();
				assert top.getEventType() == EventId.EVENT_METHOD_ENTRY || top.getEventType() == EventId.EVENT_METHOD_CALL: "Unknown case of dangling entry event: " + top.toString();
				top = events.pop();
				topMethod = locationIdMap.getMethodInfo(top.getLocationId());
			}
			return top;
		}
		
		public void processEvent(Event e) {
			switch (e.getEventType()) {
			case EventId.EVENT_FORMAL_PARAM:
				Event e2 = events.lastElement();
				assert e2.getEventType() == EventId.EVENT_METHOD_ENTRY: "ENTRY-FORMAL";
				break;
			case EventId.EVENT_ACTUAL_PARAM:
				Event caller2 = events.lastElement();
				assert caller2.getEventType() == EventId.EVENT_METHOD_CALL: "CALL-ACTUAL";
				break;
				
			case EventId.EVENT_METHOD_NORMAL_EXIT:
			case EventId.EVENT_METHOD_EXCEPTIONAL_EXIT:
				Event top = popDanglingEntry(e);
				if (mayCauseException(top)) top = events.pop(); 
				// Here, the top event must be an entry corresponding to the exit. 
				assert top.getEventType() == EventId.EVENT_METHOD_ENTRY: "Entry-Exit";
				break;
			case EventId.EVENT_OBJECT_INITIALIZED:
			case EventId.EVENT_OBJECT_CREATION_COMPLETED:
			case EventId.EVENT_RETURN_VALUE_AFTER_CALL:
				Event caller = popDanglingEntry(e);
				assert caller.getEventType() == EventId.EVENT_METHOD_CALL && caller.getLocationId() == e.getLocationId(): "CALL-RETURN";
				break;
			case EventId.EVENT_CATCH: // When an exception is caught, remove relevant events from a call stack.
				Event c = popDanglingEntry(e);
				if (!mayCauseException(c)) events.push(c); // If the event is not related to an exception, keep the event on the stack
				break;
				
			case EventId.EVENT_THROW:
				// ignore the event since the method is handled with exceptional exit.
				break;
			

			case EventId.EVENT_GET_INSTANCE_FIELD:
			case EventId.EVENT_GET_STATIC_FIELD:
			case EventId.EVENT_ARRAY_LOAD:
			case EventId.EVENT_ARRAY_STORE:
			case EventId.EVENT_LABEL:
			case EventId.EVENT_MONITOR_ENTER:
			case EventId.EVENT_MONITOR_EXIT:
			case EventId.EVENT_MULTI_NEW_ARRAY_CONTENT:
			case EventId.EVENT_PUT_INSTANCE_FIELD:
			case EventId.EVENT_PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION:
			case EventId.EVENT_PUT_STATIC_FIELD:
			case EventId.EVENT_NEW_ARRAY:
			case EventId.EVENT_MULTI_NEW_ARRAY:
			case EventId.EVENT_ARRAY_LENGTH:
			case EventId.EVENT_INSTANCEOF:
			case EventId.EVENT_CONSTANT_OBJECT_LOAD:
				// ignore the event  
				break;
				
			case EventId.EVENT_METHOD_ENTRY:
			case EventId.EVENT_METHOD_CALL:
				events.push(e);
				break;
				
			default:
				assert false: "The unknown event: " + e.getEventType();
			}
			
		}
	}
	
}
