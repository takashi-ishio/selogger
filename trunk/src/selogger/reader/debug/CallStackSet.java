package selogger.reader.debug;

import java.util.Iterator;

import selogger.reader.MethodInfo;

import gnu.trove.map.hash.TLongObjectHashMap;

/**
 * A set of call stacks for all threads in a run.
 * @author ishio
 *
 */
public class CallStackSet implements Iterable<CallStack> {

	private TLongObjectHashMap<CallStack> callstacks;

	public CallStackSet() {
		callstacks = new TLongObjectHashMap<CallStack>();
	}
	
	/**
	 * Process a call stack
	 * @param eventId
	 * @param threadId
	 * @param m
	 */
	public void processEnter(long eventId, long threadId, MethodInfo m) {
		CallStack stack = callstacks.get(threadId);
		if (stack == null) {
			stack = new CallStack(threadId);
			callstacks.put(threadId, stack);
		}
		stack.push(eventId, m);
	}
	
	public void processExit(long eventId, long threadId, MethodInfo m) {
		CallStack stack = callstacks.get(threadId);
		assert stack != null: "Call stack not found for thread: " + Long.toString(threadId);
		assert stack.getThreadId() == threadId;
		
		stack.pop(m);
	}
	
	
	@Override
	public Iterator<CallStack> iterator() {
		return callstacks.valueCollection().iterator();
	}

}
