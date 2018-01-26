package selogger.reader.debug;

import java.util.ArrayList;
import java.util.Iterator;

import selogger.weaver.MethodInfo;



/**
 * A set of call stacks for all threads in a run.
 * @author ishio
 *
 */
public class CallStackSet implements Iterable<CallStack> {

	private ArrayList<CallStack> callstacks;

	public CallStackSet() {
		callstacks = new ArrayList<CallStack>();
	}
	
	/**
	 * Process a call stack
	 * @param eventId
	 * @param threadId
	 * @param m
	 */
	public void processEnter(long eventId, int threadId, MethodInfo m) {
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
		stack.push(eventId, m);
	}
	
	public void processExit(long eventId, int threadId, MethodInfo m) {
		CallStack stack = callstacks.get(threadId);
		assert stack != null: "Call stack not found for thread: " + Long.toString(threadId);
		assert stack.getThreadId() == threadId;
		
		stack.pop(m);
	}
	
	
	@Override
	public Iterator<CallStack> iterator() {
		return callstacks.iterator();
	}

}
