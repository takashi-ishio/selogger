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
	
	public FilterLogger(IEventLogger mainLogger, ILoggingTarget start, ILoggingTarget end, IErrorLogger errorLogger) {
		this.mainLogger = mainLogger;
		this.start = start;
		this.end = end;
		this.errorLogger = errorLogger;
		this.enabled = false;
	}
	
	private boolean updateStatus(int dataId) {
		boolean disabled = false;
		if (enabled && end.isTarget(dataId)) {
			disabled = true;
			this.enabled = false;
			errorLogger.log("FilterLogger:disabled dataId=" + dataId);
		}
		if (start.isTarget(dataId)) {
			this.enabled = true;
			errorLogger.log("FilterLogger:enabled dataId=" + dataId);
		}
		return disabled;
	}

	@Override
	public void recordEvent(int dataId, boolean value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, byte value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, char value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, double value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, float value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, int value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, long value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, Object value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, short value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (enabled || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void close() {
		mainLogger.close();
	}
	
}
