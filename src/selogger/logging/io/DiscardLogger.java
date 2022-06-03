package selogger.logging.io;

import selogger.logging.IEventLogger;

/**
 * This class is an implementation of IEventLogger that discards all events.
 * This is useful to measure the overhead of inserted logging code without disk writing.
 */
public class DiscardLogger implements IEventLogger {

	/**
	 * Create an instance of the class.
	 * No parameter is needed because the object actually does nothing.
	 */
	public DiscardLogger() {
		// Nothing prepared
	}
	
	/**
	 * The close method does nothing as no resource is used by this logger.
	 */
	@Override
	public void close() {
	}
	
	/**
	 * The method does nothing since no execution trace is recorded.
	 */
	@Override
	public void save(boolean resetTrace) {
	}
	
	/**
	 * This logger does not record the given value.
	 */
	@Override
	public void recordEvent(int dataId, boolean value) {
	}
	
	/**
	 * This logger does not record the given value.
	 */
	@Override
	public void recordEvent(int dataId, byte value) {
	}
	
	/**
	 * This logger does not record the given value.
	 */
	@Override
	public void recordEvent(int dataId, char value) {
	}
	
	/**
	 * This logger does not record the given value.
	 */
	@Override
	public void recordEvent(int dataId, double value) {
	}
	
	/**
	 * This logger does not record the given value.
	 */
	@Override
	public void recordEvent(int dataId, float value) {
	}
	
	/**
	 * This logger does not record the given value.
	 */
	@Override
	public void recordEvent(int dataId, int value) {
	}
	
	/**
	 * This logger does not record the given value.
	 */
	@Override
	public void recordEvent(int dataId, long value) {
	}
	
	/**
	 * This logger does not record the given value.
	 */
	@Override
	public void recordEvent(int dataId, Object value) {
	}
	
	/**
	 * This logger does not record the given value.
	 */
	@Override
	public void recordEvent(int dataId, short value) {
	}
}
