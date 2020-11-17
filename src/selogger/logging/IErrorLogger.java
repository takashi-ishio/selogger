package selogger.logging;

/**
 * An interface for recording errors reported by SELogger components.
 * Since SELogger works as a Java agent, it should not directly use STDOUT/STDERR.    
 */
public interface IErrorLogger {

	/**
	 * Record an exception.
	 */
	public void log(Throwable t);
	
	/**
	 * Record a message.
	 */
	public void log(String msg);
	
	/**
	 * This method is called when the program is terminated.
	 */
	public void close();
}
