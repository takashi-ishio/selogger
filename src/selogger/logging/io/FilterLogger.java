package selogger.logging.io;

import selogger.logging.IErrorLogger;
import selogger.logging.IEventLogger;
import selogger.logging.ILoggingTarget;

public class FilterLogger implements IEventLogger {

	private IEventLogger mainLogger;
	private ILoggingTarget start;
	private ILoggingTarget end;
	private IErrorLogger errorLogger;
	private boolean enabled;
	
	/**
	 * A filter object to record only events between START and END.
	 * @param mainLogger is a logger object receiving filtered events 
	 * @param start specifies events that enable logging
	 * @param end specifies events that disable logging
	 * @param errorLogger is an error message recorder
	 */
	public FilterLogger(IEventLogger mainLogger, ILoggingTarget start, ILoggingTarget end, IErrorLogger errorLogger) {
		this.mainLogger = mainLogger;
		this.start = start;
		this.end = end;
		this.errorLogger = errorLogger;
		this.enabled = false;
	}
	
	/**
	 * @return true if the logging is enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Enable/Disable the logging according to an observed event.
	 * @param dataId is the dataId of the observed event.
	 * @return true if the dataId is an end event.  This flag is needed to record the event. 
	 */
	private boolean updateStatus(int dataId) {
		boolean disabled = false;
		if (start.isTarget(dataId)) {
			this.enabled = true;
			errorLogger.log("FilterLogger:enabled dataId=" + dataId);
		}
		if (enabled && end.isTarget(dataId)) {
			disabled = true;
			this.enabled = false;
			errorLogger.log("FilterLogger:disabled dataId=" + dataId);
		}
		return disabled;
	}

	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, boolean value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, byte value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, char value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, double value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, float value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, int value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, long value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, Object value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, short value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	/**
	 * This method closes the main logger object.
	 */
	@Override
	public void close() {
		mainLogger.close();
	}
	
}
