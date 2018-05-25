package selogger.logging.io;


import org.junit.Assert;
import org.junit.Test;


public class LatestEventLoggerTest {

	private static class LatestEventLoggerForTest extends LatestEventLogger {
		
		public LatestEventLoggerForTest(int size, boolean keepObject) {
			super(null, size, keepObject);
		}
		
		public String getData(int dataId) {
			LatestEventLogger.Buffer buf = prepareBuffer(Integer.class, dataId);
			return buf.toString();
		}

		public int size(int dataId) {
			LatestEventLogger.Buffer buf = prepareBuffer(Integer.class, dataId);
			return buf.size();
		}
		
		public int count(int dataId) {
			LatestEventLogger.Buffer buf = prepareBuffer(Integer.class, dataId);
			return buf.count();
		}
	}

	private static class LatestEventTimeLoggerForTest extends LatestEventTimeLogger {
		
		public LatestEventTimeLoggerForTest(int size, boolean keepObject) {
			super(null, size, keepObject);
		}
		
		public String getData(int dataId) {
			LatestEventTimeLogger.Buffer buf = prepareBuffer(Integer.class, dataId);
			return buf.toString();
		}

		public int size(int dataId) {
			LatestEventTimeLogger.Buffer buf = prepareBuffer(Integer.class, dataId);
			return buf.size();
		}
		
		public int count(int dataId) {
			LatestEventTimeLogger.Buffer buf = prepareBuffer(Integer.class, dataId);
			return buf.count();
		}
	}

	@Test
	public void testLatestLogger() {
		LatestEventLoggerForTest log = new LatestEventLoggerForTest(4, false);
		log.recordEvent(0, 1);
		log.recordEvent(0, 2);
		log.recordEvent(0, 3);
		// value, timestamp, thread
		Assert.assertEquals("1,2,3", log.getData(0));
		Assert.assertEquals(3, log.count(0));
		Assert.assertEquals(3, log.size(0));

		log.recordEvent(0, 4);
		log.recordEvent(0, 5);
		log.recordEvent(0, 6);
		// value, timestamp, thread
		Assert.assertEquals("3,4,5,6", log.getData(0));
		Assert.assertEquals(6, log.count(0));
		Assert.assertEquals(4, log.size(0));

		log.recordEvent(0, 7);
		log.recordEvent(0, 8);
		log.recordEvent(0, 9);
		log.recordEvent(0, 10);
		log.recordEvent(0, 11);
		// value, timestamp, thread
		Assert.assertEquals("8,9,10,11", log.getData(0));
		Assert.assertEquals(11, log.count(0));
		Assert.assertEquals(4, log.size(0));
	}

	@Test
	public void testLatestTimeLogger() {
		LatestEventTimeLoggerForTest log = new LatestEventTimeLoggerForTest(4, false);
		log.recordEvent(0, 1);
		log.recordEvent(0, 2);
		log.recordEvent(0, 3);
		// value, timestamp, thread
		Assert.assertEquals("1,0,0,2,1,0,3,2,0", log.getData(0));
		Assert.assertEquals(3, log.count(0));
		Assert.assertEquals(3, log.size(0));

		log.recordEvent(0, 4);
		log.recordEvent(0, 5);
		log.recordEvent(0, 6);
		// value, timestamp, thread
		Assert.assertEquals("3,2,0,4,3,0,5,4,0,6,5,0", log.getData(0));
		Assert.assertEquals(6, log.count(0));
		Assert.assertEquals(4, log.size(0));

		log.recordEvent(0, 7);
		log.recordEvent(0, 8);
		log.recordEvent(0, 9);
		log.recordEvent(0, 10);
		log.recordEvent(0, 11);
		// value, timestamp, thread
		Assert.assertEquals("8,7,0,9,8,0,10,9,0,11,10,0", log.getData(0));
		Assert.assertEquals(11, log.count(0));
		Assert.assertEquals(4, log.size(0));
	}

	
}
