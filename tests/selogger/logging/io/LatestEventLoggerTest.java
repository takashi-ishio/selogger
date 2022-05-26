package selogger.logging.io;


import org.junit.Assert;
import org.junit.Test;

import selogger.logging.IErrorLogger;
import selogger.logging.io.LatestEventLogger.ObjectRecordingStrategy;
import selogger.logging.util.ObjectIdFile.ExceptionRecording;


public class LatestEventLoggerTest {

	/**
	 * An implementation of IErrorLogger that simply discards events for testing
	 */
	private static class EmptyErrorLogger implements IErrorLogger {

		@Override
		public void log(String msg) {
		}
		
		@Override
		public void log(Throwable t) {
		}
		
		@Override
		public void close() {
		}

	}

	/**
	 * LatestEventTimeLogger with additional methods for testing
	 */
	private static class LatestEventLoggerForTest extends LatestEventLogger {
		
		public LatestEventLoggerForTest(int size, ObjectRecordingStrategy keepObject) {
			super(null, size, keepObject, false, ExceptionRecording.Disabled, false, new EmptyErrorLogger());
		}
		
		/**
		 * @param dataId
		 * @return a string of recorded values for the dataId.
		 */
		public String getData(int dataId) {
			LatestEventBuffer buf = prepareBuffer(int.class, "int", dataId);
			return buf.toString();
		}

		/**
		 * @param dataId
		 * @return the buffer size to record values for the dataId.
		 */
		public int size(int dataId) {
			LatestEventBuffer buf = prepareBuffer(int.class, "int", dataId);
			return buf.size();
		}
		
		/**
		 * @param dataId
		 * @return the number of events of the dataId.
		 */
		public long count(int dataId) {
			LatestEventBuffer buf = prepareBuffer(int.class, "int", dataId);
			return buf.count();
		}
	}


	@Test
	public void testLatestTimeLogger() {
		LatestEventLoggerForTest log = new LatestEventLoggerForTest(4, ObjectRecordingStrategy.Weak);
		// Add three events for dataId=0
		log.recordEvent(0, 1);
		log.recordEvent(0, 2);
		log.recordEvent(0, 3);
		// They should be correctly recorded.  (Triples of value, timestamp, and thread)
		Assert.assertEquals("1,0,0,2,1,0,3,2,0", log.getData(0));
		Assert.assertEquals(3, log.count(0));
		Assert.assertEquals(3, log.size(0));

		// Add additional three events for dataId=0
		log.recordEvent(0, 4);
		log.recordEvent(0, 5);
		log.recordEvent(0, 6);
		// As the buffer size = 4, the latest four events should be recorded
		Assert.assertEquals("3,2,0,4,3,0,5,4,0,6,5,0", log.getData(0));
		Assert.assertEquals(6, log.count(0));
		Assert.assertEquals(4, log.size(0));

		// Add five events for dataId=0
		log.recordEvent(0, 7);
		log.recordEvent(0, 8);
		log.recordEvent(0, 9);
		log.recordEvent(0, 10);
		log.recordEvent(0, 11);
		// As the buffer size = 4, the latest four events should be recorded
		Assert.assertEquals("8,7,0,9,8,0,10,9,0,11,10,0", log.getData(0));
		Assert.assertEquals(11, log.count(0)); // 11 events
		Assert.assertEquals(4, log.size(0));
	}

}
