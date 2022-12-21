package selogger.logging.io;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

import selogger.logging.IErrorLogger;
import selogger.logging.IEventLogger;
import selogger.logging.ILoggingTarget;

public class FilterLogger implements IEventLogger {

	private IEventLogger mainLogger;
	private ILoggingTarget start;
	private ILoggingTarget end;
	private IErrorLogger errorLogger;
	private AtomicInteger enabledCount;
	private boolean allowNestedIntervals;
	private PartialSaveStrategy partialSave;
	
	/**
	 * Strategies to write files when an "end" event is observed
	 */
	public enum PartialSaveStrategy { 
		/**
		 * This strategy writes a file only when a program is terminated.
		 */
		No, 
		/**
		 * This strategy saves a snapshot when the "end" event occurred.
		 */
		WriteSnapshot, 
		/**
		 * This strategy saves a snapshot to a file and 
		 * discard the recorded information. 
		 * Every "end" event produces a partial trace from a begin event 
		 * to its corresponding end event. 
		 */
		WriteAndReset 
	};

	/**
	 * Increment a counter to record the number of nested BEGIN events.
	 */
	private static IntUnaryOperator increment = new IntUnaryOperator() {
		@Override
		public int applyAsInt(int operand) {
			if (operand > 0) return operand + 1; 
			else return 1;
		}
	};

	private static IntUnaryOperator turnOn = new IntUnaryOperator() {
		@Override
		public int applyAsInt(int operand) {
			return 1;
		}
	};

	private static IntUnaryOperator decrement = new IntUnaryOperator() {
		@Override
		public int applyAsInt(int operand) {
			if (operand > 0) return operand - 1; 
			else return 0;
		}
	};
	
	/**
	 * A filter object to record only events between START and END.
	 * @param mainLogger is a logger object receiving filtered events 
	 * @param start specifies events that enable logging
	 * @param end specifies events that disable logging
	 * @param errorLogger is an error message recorder
	 */
	public FilterLogger(IEventLogger mainLogger, ILoggingTarget start, ILoggingTarget end, IErrorLogger errorLogger, boolean allowNestedIntervals, PartialSaveStrategy partialSave) {
		this.mainLogger = mainLogger;
		this.start = start;
		this.end = end;
		this.errorLogger = errorLogger;
		this.enabledCount = new AtomicInteger(0);
		this.allowNestedIntervals = allowNestedIntervals;
		this.partialSave = partialSave;
	}
	
	/**
	 * @return true if the logging is enabled
	 */
	public boolean isEnabled() {
		return enabledCount.get() > 0;
	}
	
	
	/**
	 * Enable/Disable the logging according to an observed event.
	 * @param dataId is the dataId of the observed event.
	 * @return true if the dataId is an end event.  This flag is needed to record the event. 
	 */
	private boolean updateStatus(int dataId) {
		boolean disabled = false;
		if (start.isTarget(dataId)) {
			IntUnaryOperator updater = allowNestedIntervals ? increment : turnOn;
			int count = enabledCount.updateAndGet(updater);
			errorLogger.log("FilterLogger:logstart dataId=" + dataId + " level=" + count);
		}
		if (enabledCount.get() > 0 && end.isTarget(dataId)) {
			int count = enabledCount.updateAndGet(decrement);
			disabled = (count == 0);
			errorLogger.log("FilterLogger:logend dataId=" + dataId + " level=" + count);
		}
		return disabled;
	}

	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, boolean value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (isEnabled() || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
		if (disabledOnThisEvent && partialSave != PartialSaveStrategy.No) mainLogger.save(partialSave == PartialSaveStrategy.WriteAndReset);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, byte value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (isEnabled() || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
		if (disabledOnThisEvent && partialSave != PartialSaveStrategy.No) mainLogger.save(partialSave == PartialSaveStrategy.WriteAndReset);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, char value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (isEnabled() || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
		if (disabledOnThisEvent && partialSave != PartialSaveStrategy.No) mainLogger.save(partialSave == PartialSaveStrategy.WriteAndReset);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, double value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (isEnabled() || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
		if (disabledOnThisEvent && partialSave != PartialSaveStrategy.No) mainLogger.save(partialSave == PartialSaveStrategy.WriteAndReset);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, float value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (isEnabled() || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
		if (disabledOnThisEvent && partialSave != PartialSaveStrategy.No) mainLogger.save(partialSave == PartialSaveStrategy.WriteAndReset);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, int value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (isEnabled() || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
		if (disabledOnThisEvent && partialSave != PartialSaveStrategy.No) mainLogger.save(partialSave == PartialSaveStrategy.WriteAndReset);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, long value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (isEnabled() || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
		if (disabledOnThisEvent && partialSave != PartialSaveStrategy.No) mainLogger.save(partialSave == PartialSaveStrategy.WriteAndReset);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, Object value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (isEnabled() || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
		if (disabledOnThisEvent && partialSave != PartialSaveStrategy.No) mainLogger.save(partialSave == PartialSaveStrategy.WriteAndReset);
	}
	
	/**
	 * Record an event if the logging is enabled
	 */
	@Override
	public void recordEvent(int dataId, short value) {
		boolean disabledOnThisEvent = updateStatus(dataId);
		if (isEnabled() || disabledOnThisEvent) mainLogger.recordEvent(dataId, value);
		if (disabledOnThisEvent && partialSave != PartialSaveStrategy.No) mainLogger.save(partialSave == PartialSaveStrategy.WriteAndReset);
	}
	
	/**
	 * This method closes the main logger object.
	 */
	@Override
	public void close() {
		mainLogger.close();
	}
	
	/**
	 * Save the recorded trace
	 */
	@Override
	public void save(boolean resetTrace) {
		mainLogger.save(resetTrace);
	}
	
}
