package selogger.reader.debug;


import java.util.Stack;

import selogger.reader.MethodInfo;



/**
 * A CallStack object represents a call stack 
 * for a single thread.
 * @author ishio
 *
 */
public class CallStack {

	private long threadId;
	private Stack<Entry> entries;
	
	/**
	 * @param threadId associates thread ID with the stack.
	 */
	public CallStack(long threadId) {
		this.threadId = threadId;
		this.entries = new Stack<Entry>();
	}
	
	/**
	 * Push an executed method and its object.
	 * @param methodId
	 * @param objectId
	 */
	public void push(long eventId, MethodInfo m) {
		this.entries.push(new Entry(eventId, m));
	}

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
	
	public MethodInfo getMethodOnStack(int pos) {
		return entries.get(pos).m;
	}
	
	public long getEventIdOnStack(int pos) {
		return entries.get(pos).enterEventId;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (int i=0; i<entries.size(); ++i) {
			if (i>0)	b.append(", ");
			b.append(entries.get(i).toString());
		}
		return b.toString();
	}
	
	private class Entry {
		
		private long enterEventId;
		private MethodInfo m;
		
		public Entry(long enterEventId, MethodInfo m) {
			this.enterEventId = enterEventId;
			this.m = m;
		}
		
		@Override
		public String toString() {
			return "(e=" + enterEventId + ", m=" + m.toString() + ")";
		}
	}

}
