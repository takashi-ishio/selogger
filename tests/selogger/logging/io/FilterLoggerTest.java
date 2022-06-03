package selogger.logging.io;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import selogger.logging.IErrorLogger;
import selogger.logging.ILoggingTarget;

public class FilterLoggerTest {

	/**
	 * A target object to directly specify a dataId for testing
	 */
	public static class FixedId implements ILoggingTarget {
		
		private int target = 0;
		
		public FixedId(int target) {
			this.target = target;
		}
		@Override
		public boolean isTarget(int dataid) {
			return dataid == target;
		}
	}
	
	/**
	 * A message recorder for testing
	 */
	public static class StringLogger implements IErrorLogger {
		private ArrayList<String> messages;
		public StringLogger() {
			messages = new ArrayList<>();
		}
		@Override
		public void log(String msg) {
			messages.add(msg);
		}
		@Override
		public void log(Throwable t) {
		}
		@Override
		public void close() {
		}
		
		public int getMessageCount() {
			return messages.size();
		}
	}

	@Test
	public void testFilter() {
		MemoryLogger mem = new MemoryLogger();
		StringLogger messages = new StringLogger();
		FilterLogger filter = new FilterLogger(mem, new FixedId(1), new FixedId(3), messages, false);
		
		// At first, logging is disabled 
		Assert.assertFalse(filter.isEnabled());
		filter.recordEvent(0, 0);
		Assert.assertFalse(filter.isEnabled());
		
		// The first start event enables the logging 
		filter.recordEvent(1, 0);
		Assert.assertTrue(filter.isEnabled());
		filter.recordEvent(2, 0);

		// The second start event does not affect logging
		filter.recordEvent(1, 0);

		// The end event disables the logging 
		filter.recordEvent(3, 0);
		Assert.assertFalse(filter.isEnabled());

		// This event is ignored
		filter.recordEvent(4, 0);
		
		// Four events between 1 and 3 are recorded 
		Assert.assertEquals(4, mem.getEvents().size());
		Assert.assertEquals(1, mem.getEvents().get(0).getDataId());
		Assert.assertEquals(2, mem.getEvents().get(1).getDataId());
		Assert.assertEquals(1, mem.getEvents().get(2).getDataId());
		Assert.assertEquals(3, mem.getEvents().get(3).getDataId());
	}

	@Test
	public void testEnableAndDisable() {
		MemoryLogger mem = new MemoryLogger();
		StringLogger messages = new StringLogger();
		FilterLogger filter = new FilterLogger(mem, new FixedId(1), new FixedId(1), messages, false);
		
		// At first, logging is disabled
		Assert.assertFalse(filter.isEnabled());
		filter.recordEvent(0, 0);

		// Logging stays disabled if the start and end events are the same,
		// while it records the event itself
		Assert.assertFalse(filter.isEnabled());
		filter.recordEvent(1, 0);
		Assert.assertFalse(filter.isEnabled());
		
		Assert.assertEquals(2, messages.getMessageCount());
		Assert.assertEquals(1, mem.getEvents().size());
		Assert.assertEquals(1, mem.getEvents().get(0).getDataId());

		// This event is ignored
		filter.recordEvent(2, 0);
		Assert.assertFalse(filter.isEnabled());
		Assert.assertEquals(2, messages.getMessageCount());

		// The filter records the target event 
		filter.recordEvent(1, 0);
		Assert.assertFalse(filter.isEnabled());
		Assert.assertEquals(4, messages.getMessageCount());
		Assert.assertEquals(2, mem.getEvents().size());
		Assert.assertEquals(1, mem.getEvents().get(1).getDataId());

		// This event is ignored
		filter.recordEvent(4, 0);
		Assert.assertFalse(filter.isEnabled());
	}
	
	@Test
	public void testFilterAllowingNestedIntervals() {
		MemoryLogger mem = new MemoryLogger();
		StringLogger messages = new StringLogger();
		FilterLogger filter = new FilterLogger(mem, new FixedId(1), new FixedId(3), messages, true);
		
		// At first, logging is disabled 
		Assert.assertFalse(filter.isEnabled());
		filter.recordEvent(0, 0);
		Assert.assertFalse(filter.isEnabled());
		
		// The first start event enables the logging 
		filter.recordEvent(1, 0);
		Assert.assertTrue(filter.isEnabled());
		filter.recordEvent(2, 0);

		// The second start event  
		filter.recordEvent(1, 0);

		// The end event matches to the second start event.  It does not stop the logging 
		filter.recordEvent(3, 0);
		Assert.assertTrue(filter.isEnabled());

		// This event is recorded
		filter.recordEvent(4, 0);
		
		// The second end event stops the logging
		filter.recordEvent(3, 0);

		// This event is ignored
		filter.recordEvent(4, 0);

		// Four events between 1 and 3 are recorded 
		Assert.assertEquals(6, mem.getEvents().size());
		Assert.assertEquals(1, mem.getEvents().get(0).getDataId());
		Assert.assertEquals(2, mem.getEvents().get(1).getDataId());
		Assert.assertEquals(1, mem.getEvents().get(2).getDataId());
		Assert.assertEquals(3, mem.getEvents().get(3).getDataId());
		Assert.assertEquals(4, mem.getEvents().get(4).getDataId());
		Assert.assertEquals(3, mem.getEvents().get(5).getDataId());
	}

}
