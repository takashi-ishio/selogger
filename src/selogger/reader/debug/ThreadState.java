package selogger.reader.debug;

import java.util.Stack;

import selogger.EventType;
import selogger.reader.Event;
import selogger.weaver.MethodInfo;

/**
 * A class representing an event stack to check 
 * ENTRY-EXIT and CALL-CALL_RETURN consistency.  
 */
public class ThreadState {

	private Stack<Event> events;
	
	/**
	 * Create an object for a thread
	 */
	public ThreadState() {
		events = new Stack<Event>();
	}
	
	/**
	 * Remove entry events if they are not a given event
	 * @param currentEvent specifies a method on the top of the call stack 
	 * @return the last popped event
	 */
	private Event popDanglingEntry(Event currentEvent) {
		MethodInfo currentMethod = currentEvent.getMethodEntry();
		Event top = events.pop();
		MethodInfo topMethod = top.getMethodEntry();
		while (topMethod != currentMethod) {
			assert topMethod.getMethodName().equals("<init>"): "Unknown case of dangling entry event: " + topMethod.toString();
			assert top.getEventType() == EventType.METHOD_ENTRY || top.getEventType() == EventType.CALL: "Unknown case of dangling entry event: " + top.toString();
			top = events.pop();
			topMethod = top.getMethodEntry();
		}
		return top;
	}
	
	/**
	 * Update a thread state using the event.
	 */
	public void processEvent(Event e) {
		switch (e.getEventType()) {
		case METHOD_ENTRY:
		case CALL:
			// Push the entry/call event on the stack
			events.push(e);
			break;

		case METHOD_PARAM:
			// The previous event of a formal parameter must be a METHOD_ENTRY event on the stack top 
			Event e2 = events.lastElement();
			assert e2.getEventType() == EventType.METHOD_ENTRY: "ENTRY-FORMAL";
			break;
			
		case CALL_PARAM:
			// The previous event of an actual parameter must be a CALL event on the stack top 
			Event caller2 = events.lastElement();
			assert caller2.getEventType() == EventType.CALL: "CALL-ACTUAL";
			break;
			
		case INVOKE_DYNAMIC_PARAM:
			assert false: "This event should be skipped by processParams";
			break;
		
		case METHOD_NORMAL_EXIT:
		case METHOD_EXCEPTIONAL_EXIT:
			// Remove the top of the stack
			Event top = popDanglingEntry(e);
			if (e.getEventType() == EventType.CALL) {
				top = events.pop(); 
			}
			// Here, the top event must be an entry corresponding to the exit. 
			assert top.getEventType() == EventType.METHOD_ENTRY: "Entry-Exit";
			break;
			
		case CALL_RETURN:
			// The last event of the caller method on the stack must be a CALL event 
			Event caller = popDanglingEntry(e);
			assert caller.getEventType() == EventType.CALL;
			int parentId = Integer.parseInt(e.getDataAttribute("CallParent", "-1"));
			assert caller.getDataId() == parentId: "CALL-RETURN";
			break;

		case CATCH: 
			// When an exception is caught, remove relevant events from a call stack.
			// Check the top entry (ENTRY or CALL).
			Event c = popDanglingEntry(e);
			// If the event is related to an ENTRY event, keep the event on the stack.
			if (e.getEventType() == EventType.METHOD_ENTRY) {
				events.push(c); 
			}
			break;
			
		case METHOD_THROW:
			// ignore the event since the method is handled with exceptional exit.
			break;
		
		case LOCAL_LOAD:
		case LOCAL_STORE:
		case LOCAL_INCREMENT:
		case JUMP:
		case GET_INSTANCE_FIELD:
		case GET_INSTANCE_FIELD_RESULT:
		case GET_STATIC_FIELD:
		case ARRAY_LOAD:
		case ARRAY_LOAD_INDEX:
		case ARRAY_LOAD_RESULT:
		case ARRAY_STORE:
		case ARRAY_STORE_INDEX:
		case ARRAY_STORE_VALUE:
		case LABEL:
		case CATCH_LABEL:
		case MONITOR_ENTER:
		case MONITOR_ENTER_RESULT:
		case MONITOR_EXIT:
		case MULTI_NEW_ARRAY_OWNER:
		case MULTI_NEW_ARRAY_ELEMENT:
		case NEW_OBJECT:
		case METHOD_OBJECT_INITIALIZED:
		case NEW_OBJECT_CREATED:
		case PUT_INSTANCE_FIELD:
		case PUT_INSTANCE_FIELD_VALUE:
		case PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION:
		case PUT_STATIC_FIELD:
		case NEW_ARRAY:
		case NEW_ARRAY_RESULT:
		case MULTI_NEW_ARRAY:
		case ARRAY_LENGTH:
		case ARRAY_LENGTH_RESULT:
		case OBJECT_INSTANCEOF:
		case OBJECT_INSTANCEOF_RESULT:
		case OBJECT_CONSTANT_LOAD:
		case INVOKE_DYNAMIC:
		case INVOKE_DYNAMIC_RESULT:
		case RET:
		case DIVIDE:
			// ignore the event  
			break;
			
		case RESERVED:
			assert false: "Reserved Event was found.";
		
//		default:
//			assert false: "The unknown event: " + e.getEventType();
		}
		
	}

}
