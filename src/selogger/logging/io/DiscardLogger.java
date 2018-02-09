package selogger.logging.io;

import selogger.logging.IEventLogger;

public class DiscardLogger implements IEventLogger {

	public DiscardLogger() {
	}
	
	@Override
	public void close() {
	}
	
	@Override
	public void recordEvent(int dataId, boolean value) {
	}
	
	@Override
	public void recordEvent(int dataId, byte value) {
	}
	
	@Override
	public void recordEvent(int dataId, char value) {
	}
	
	@Override
	public void recordEvent(int dataId, double value) {
	}
	
	@Override
	public void recordEvent(int dataId, float value) {
	}
	
	@Override
	public void recordEvent(int dataId, int value) {
	}
	
	@Override
	public void recordEvent(int dataId, long value) {
	}
	
	@Override
	public void recordEvent(int dataId, Object value) {
	}
	
	@Override
	public void recordEvent(int dataId, short value) {
	}
}
