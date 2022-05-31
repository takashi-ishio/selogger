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
	
	private void updateStatus(int dataId) {
		if (end.isTarget(dataId)) {
			this.enabled = false;
			errorLogger.log("FilterLogger:disabled dataId=" + dataId);
		}
		if (start.isTarget(dataId)) {
			this.enabled = true;
			errorLogger.log("FilterLogger:enabled dataId=" + dataId);
		}
	}

	@Override
	public void recordEvent(int dataId, boolean value) {
		updateStatus(dataId);
		if (enabled) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, byte value) {
		updateStatus(dataId);
		if (enabled) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, char value) {
		updateStatus(dataId);
		if (enabled) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, double value) {
		updateStatus(dataId);
		if (enabled) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, float value) {
		updateStatus(dataId);
		if (enabled) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, int value) {
		updateStatus(dataId);
		if (enabled) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, long value) {
		updateStatus(dataId);
		if (enabled) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, Object value) {
		updateStatus(dataId);
		if (enabled) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void recordEvent(int dataId, short value) {
		updateStatus(dataId);
		if (enabled) mainLogger.recordEvent(dataId, value);
	}
	
	@Override
	public void close() {
		mainLogger.close();
	}
	
}
