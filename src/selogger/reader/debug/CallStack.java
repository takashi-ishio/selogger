package selogger.reader.debug;


import java.util.Stack;

import selogger.weaver.MethodInfo;



/**
 * A CallStack object represents a stack for ENTRY/EXIT events
 * for a single thread.
 */
public class CallStack {

	private long threadId;
	private Stack<Entry> entries;
	
	/**
	 * Create an instance for a thread.
	 * @param threadId associates thread ID with the stack.
	 */
	public CallStack(long threadId) {
		this.threadId = threadId;
		this.entries = new Stack<Entry>();
	}
	
	/**
	 * Push an executed method and its object.
	 * @param eventId is the event ID.
	 * @param m is the executed method.
	 */
	public void push(long eventId, MethodInfo m) {
		this.entries.push(new Entry(eventId, m));
	}

	/**
	 * Pop the executed method from the stack.
	 * @param m specifies the method.
	 * m is needed to remove methods that could not 
	 * be removed from the stack due to exception handling.
	 */
	public void pop(MethodInfo m) {
		Entry e = entries.pop();
		while (m != e.m) { 
			assert e.m.getMethodName().equals("<init>"); // a constructor execution may be terminated by an exception
			e = entries.pop();
		}
		assert m == e.m: "Method push-pop failed. Expected: " + e.m.toString() + ", Actual: " + m.toString();
	}
	
	/**
	 * @return thread ID of the call stack.
	 */
	public long getThreadId() {
		return threadId;
	}
	
	/**
	 * @return the number of methods on the stack.
	 */
	public int size() {
		return entries.size();
	}
	
	/**
	 * Accessor for a MethodInfo on the call stack.
	 * @param index specifies the location in the stack.
	 * @return MethodInfo stored in the location.
	 */
	public MethodInfo getMethodOnStack(int pos) {
		return entries.get(pos).m;
	}
	
	/**
	 * Accessor for an event ID on the call stack.
	 * @param index specifies the location in the stack.
	 * @return event ID stored in the location.
	 */
	public long getEventIdOnStack(int index) {
		return entries.get(index).enterEventId;
	}
	
	/**
	 * @return a textual representation of the stack.
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (int i=0; i<entries.size(); ++i) {
			if (i>0)	b.append(", ");
			b.append(entries.get(i).toString());
		}
		return b.toString();
	}
	
	/**
	 * A call stack entry keeping when, which method is called. 
	 */
	private class Entry {
		
		private long enterEventId;
		private MethodInfo m;
		
		/**
		 * Create an entry 
		 * @param enterEventId is the first event entering the method.
		 * @param m is the called method. 
		 */
		public Entry(long enterEventId, MethodInfo m) {
			this.enterEventId = enterEventId;
			this.m = m;
		}
		
		/**
		 * @return a string representation including the attributes.
		 */
		@Override
		public String toString() {
			return "(e=" + enterEventId + ", m=" + m.toString() + ")";
		}
	}

}
