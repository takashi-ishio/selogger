package selogger.logging.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class to generate Thread IDs.
 * This class assigns its own IDs (sequential numbers) for threads 
 * instead of Thread.currentThread.getId() to assign the same IDs 
 * if they are started in the same order.  
 */
public class ThreadId {

	/**
	 * An object to assign an integer for each thread.
	 */
	private static final AtomicInteger nextThreadId = new AtomicInteger(0);

	/**
	 * This object keeps thread IDs for each thread.
	 */
	private static ThreadLocal<Integer> threadId = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return nextThreadId.getAndIncrement();
		}
	};

	/**
	 * @return a thread ID assigned by this class.
	 */
	public static int get() {
		return threadId.get();
	}

}
