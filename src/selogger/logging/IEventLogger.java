package selogger.logging;


/**
 * This interface defines a set of methods for recording runtime events. 
 * Classes implementing this interface should provide an actual logging strategy. 
 * selogger.logging.Logging class uses this interface to record events. 
 */
public interface IEventLogger {

	/**
	 * Close the logger.  
	 * An implementation class may use this method to release any resources used for logging. 
	 */
	public void close();
	
	/**
	 * Record an event occurrence and a value.
	 * @param dataId specifies an event and its bytecode location.
	 * @param value contains a value to be recorded.
	 */
	public void recordEvent(int dataId, Object value);

	/**
	 * Record an event occurrence and a value.
	 * @param dataId specifies an event and its bytecode location.
	 * @param value contains a value to be recorded.
	 */
	public void recordEvent(int dataId, int value);

	/**
	 * Record an event occurrence and a value.
	 * @param dataId specifies an event and its bytecode location.
	 * @param value contains a value to be recorded.
	 */
	public void recordEvent(int dataId, long value);

	/**
	 * Record an event occurrence and a value.
	 * @param dataId specifies an event and its bytecode location.
	 * @param value contains a value to be recorded.
	 */
	public void recordEvent(int dataId, byte value);
	
	/**
	 * Record an event occurrence and a value.
	 * @param dataId specifies an event and its bytecode location.
	 * @param value contains a value to be recorded.
	 */
	public void recordEvent(int dataId, short value);

	/**
	 * Record an event occurrence and a value.
	 * @param dataId specifies an event and its bytecode location.
	 * @param value contains a value to be recorded.
	 */
	public void recordEvent(int dataId, char value);

	/**
	 * Record an event occurrence and a value.
	 * @param dataId specifies an event and its bytecode location.
	 * @param value contains a value to be recorded.
	 */
	public void recordEvent(int dataId, boolean value);

	/**
	 * Record an event occurrence and a value.
	 * @param dataId specifies an event and its bytecode location.
	 * @param value contains a value to be recorded.
	 */
	public void recordEvent(int dataId, double value);

	/**
	 * Record an event occurrence and a value.
	 * @param dataId specifies an event and its bytecode location.
	 * @param value contains a value to be recorded.
	 */
	public void recordEvent(int dataId, float value);

}
