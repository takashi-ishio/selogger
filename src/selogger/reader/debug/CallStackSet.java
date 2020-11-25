package selogger.reader.debug;

import java.util.ArrayList;
import java.util.Iterator;

import selogger.weaver.MethodInfo;



/**
 * A set of call stacks for all threads in a run.
 */
public class CallStackSet implements Iterable<CallStack> {

	private ArrayList<CallStack> callstacks;

	/**
	 * Create an empty stack set.
	 */
	public CallStackSet() {
		callstacks = new ArrayList<CallStack>();
	}
	
	/**
	 * Update a stack for an entry event. 
	 * @param eventId is the event ID.
	 * @param threadId specifies a thread that called the method.
	 * @param m is the called method.
	 */
	public void processEnter(long eventId, int threadId, MethodInfo m) {
		// Get a call stack for the thread
		if (threadId >= callstacks.size()) {
			while (threadId >= callstacks.size()) {
				callstacks.add(null);
			}
		}
		CallStack stack = callstacks.get(threadId);
		if (stack == null) {
			stack = new CallStack(threadId);
			callstacks.set(threadId, stack);
		}
		// Update the stack
		stack.push(eventId, m);
	}
	
	/**
	 * Update a stack for an exit event.
	 * @param eventId is the event ID.
	 * @param threadId specifies a thread terminating the method.
	 * @param m is the terminating method.
	 */
	public void processExit(long eventId, int threadId, MethodInfo m) {
		CallStack stack = callstacks.get(threadId);
		assert stack != null: "Call stack not found for thread: " + Long.toString(threadId);
		assert stack.getThreadId() == threadId;
		
		stack.pop(m);
	}
	
	/**
	 * This method enables to use a for loop to access stacks. 
	 */
	@Override
	public Iterator<CallStack> iterator() {
		return callstacks.iterator();
	}

}
