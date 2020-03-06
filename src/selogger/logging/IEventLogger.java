package selogger.logging;


/**
 * This interface defines a set of methods for recording runtime events. 
 * Logging class uses this interface to record events; a user can choose a strategy at runtime.
 */
public interface IEventLogger {

	public void close();
	public void recordEvent(int dataId, Object value);
	public void recordEvent(int dataId, int value);
	public void recordEvent(int dataId, long value);
	public void recordEvent(int dataId, byte value);
	public void recordEvent(int dataId, short value);
	public void recordEvent(int dataId, char value);
	public void recordEvent(int dataId, boolean value);
	public void recordEvent(int dataId, double value);
	public void recordEvent(int dataId, float value);

}
