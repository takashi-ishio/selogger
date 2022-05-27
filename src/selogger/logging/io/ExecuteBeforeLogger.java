package selogger.logging.io;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import selogger.logging.IErrorLogger;
import selogger.logging.IEventLogger;

public class ExecuteBeforeLogger implements IEventLogger {
	
	private static class ExecutionCounter {
		private long[] counters;
		private int maxId;
		
		public ExecutionCounter() {
			counters = new long[65536];
		}
		
		public boolean isFirst(int dataId) {
			return counters.length <= dataId || counters[dataId] == 0;
		}
		
		public void increment(int dataId) {
			if (counters.length <= dataId) {
				counters = Arrays.copyOf(counters, Math.max(counters.length * 2, (int)(dataId * 1.1)));
			}
			counters[dataId]++;
			maxId = Math.max(maxId, dataId);
		}
		
		public int getMaxId() {
			return maxId;
		}
	}
	
	private JsonGenerator generator;
	private IErrorLogger logger;
	
	private static ThreadLocal<ExecutionCounter> executed = new ThreadLocal<ExecutionCounter>() {
		@Override
		protected ExecutionCounter initialValue() {
			return new ExecutionCounter();
		}
	};
	
	public ExecuteBeforeLogger(File outputDir, IErrorLogger logger) {
		try {
			JsonFactory factory = new JsonFactory();
			generator = factory.createGenerator(new File(outputDir, "executebefore.json"), JsonEncoding.UTF8);
			generator.useDefaultPrettyPrinter();
			generator.writeStartObject();
			generator.writeStringField("format", "execute-before");
			generator.writeArrayFieldStart("records");
			
		} catch (IOException e) {
			generator = null;
			logger.log(e);
		}
	}
	
	private void record(int dataId) {  
		if (generator == null) return;
		
		ExecutionCounter executedDataId = executed.get();
		if (executedDataId.isFirst(dataId)) {
			try {
				synchronized (generator) {
					if (!generator.isClosed()) {
						int vectorLength = executedDataId.getMaxId() + 1;
						generator.writeStartObject();
						generator.writeNumberField("dataId", dataId);
						generator.writeNumberField("threadId", Thread.currentThread().getId());
						generator.writeNumberField("vectorLength", vectorLength);
						generator.writeFieldName("executeBefore");
						generator.writeArray(executedDataId.counters, 0, vectorLength);
						generator.writeEndObject();
					}
				}
			} catch (IOException e) {
				logger.log(e);
				generator = null;
			}
		}
		executedDataId.increment(dataId);
	}

	@Override
	public synchronized void close() {
		if (generator != null) {
			synchronized (generator) {
				try {
					generator.writeEndArray();
					generator.close();
					generator = null;
				} catch (IOException e) {
					logger.log(e);
				}
			}
		}
	}

	@Override
	public void recordEvent(int dataId, boolean value) {
		record(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, byte value) {
		record(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, char value) {
		record(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, double value) {
		record(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, float value) {
		record(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, int value) {
		record(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, long value) {
		record(dataId);
	}

	@Override
	public void recordEvent(int dataId, Object value) {
		record(dataId);
	}
	
	@Override
	public void recordEvent(int dataId, short value) {
		record(dataId);
	}
	
}
