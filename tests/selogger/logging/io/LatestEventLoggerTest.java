package selogger.logging.io;


import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Scanner;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import selogger.EventType;
import selogger.logging.IErrorLogger;
import selogger.logging.io.LatestEventLogger.ObjectRecordingStrategy;
import selogger.logging.util.ObjectIdFile.ExceptionRecording;
import selogger.weaver.DataInfo;
import selogger.weaver.MethodInfo;
import selogger.weaver.method.Descriptor;


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
		
		/**
		 * @return the Json file result
		 */
		public String getJsonOutput() {
			StringWriter w = new StringWriter();
			PrintWriter writer = new PrintWriter(w);
			saveJson(writer);
			writer.close();
			return w.toString();
		}

		/**
		 * @return the csv file result
		 */
		public String getCsvOutput() {
			StringWriter w = new StringWriter();
			PrintWriter writer = new PrintWriter(w);
			saveText(writer);
			writer.close();
			return w.toString();
		}
	}


	/**
	 * This test case assumes that the method is executed by the main thread 
	 */
	@Test
	public void testLatestTimeLogger() {
		LatestEventLoggerForTest log = new LatestEventLoggerForTest(4, ObjectRecordingStrategy.Weak);
		long seqnum = LatestEventLogger.getSeqnum();
		// Add three events for dataId=0
		log.recordEvent(0, 1);
		log.recordEvent(0, 2);
		log.recordEvent(0, 3);
		// They should be correctly recorded.  (Freq, Record, Triples of value, timestamp, and thread)
		String[] elements = log.getData(0).split(",");
		Assert.assertEquals("3", elements[0]);
		Assert.assertEquals("3", elements[1]);
		Assert.assertEquals("1", elements[2]);
		Assert.assertEquals(Long.toString(seqnum), elements[3]);
		Assert.assertEquals("0", elements[4]);
		Assert.assertEquals("2", elements[5]);
		Assert.assertEquals(Long.toString(seqnum+1), elements[6]);
		Assert.assertEquals("0", elements[7]);
		Assert.assertEquals("3", elements[8]);
		Assert.assertEquals(Long.toString(seqnum+2), elements[9]);
		Assert.assertEquals("0", elements[10]);
		Assert.assertEquals(3, log.count(0));
		Assert.assertEquals(3, log.size(0));

		// Add additional three events for dataId=0
		log.recordEvent(0, 4);
		log.recordEvent(0, 5);
		log.recordEvent(0, 6);
		// As the buffer size = 4, the latest four events should be recorded
		elements = log.getData(0).split(",");
		Assert.assertEquals("6", elements[0]);
		Assert.assertEquals("4", elements[1]);
		Assert.assertEquals("3", elements[2]);
		Assert.assertEquals("4", elements[5]);
		Assert.assertEquals("5", elements[8]);
		Assert.assertEquals("6", elements[11]);
		Assert.assertEquals(Long.toString(seqnum+2), elements[3]);
		Assert.assertEquals(Long.toString(seqnum+3), elements[6]);
		Assert.assertEquals(Long.toString(seqnum+4), elements[9]);
		Assert.assertEquals(Long.toString(seqnum+5), elements[12]);
		Assert.assertEquals(6, log.count(0));
		Assert.assertEquals(4, log.size(0));

		// Add five events for dataId=0
		log.recordEvent(0, 7);
		log.recordEvent(0, 8);
		log.recordEvent(0, 9);
		log.recordEvent(0, 10);
		log.recordEvent(0, 11);
		// As the buffer size = 4, the latest four events should be recorded
		elements = log.getData(0).split(",");
		Assert.assertEquals("11", elements[0]);
		Assert.assertEquals("4", elements[1]);
		Assert.assertEquals("8", elements[2]);
		Assert.assertEquals("9", elements[5]);
		Assert.assertEquals("10", elements[8]);
		Assert.assertEquals("11", elements[11]);
		Assert.assertEquals(11, log.count(0)); // 11 events
		Assert.assertEquals(4, log.size(0));
	}

	/**
	 * @return a logger object for test cases
	 */
	private LatestEventLoggerForTest createLog() {
		LatestEventLoggerForTest log = new LatestEventLoggerForTest(4, ObjectRecordingStrategy.Weak);
		DataInfo d1 = new DataInfo(0, 0, 0, 0, 0, EventType.METHOD_ENTRY, Descriptor.Void, null);
		DataInfo d2 = new DataInfo(0, 0, 1, 0, 0, EventType.METHOD_NORMAL_EXIT, Descriptor.Integer, null);
		MethodInfo m = new MethodInfo(0, 0, "myClass", "myMethod", "()I", 0, "myClass.java", "0123456789abcdef");
		d1.setMethodInfo(m);
		d2.setMethodInfo(m);
		log.onCreated(Arrays.asList(d1, d2));
		return log;
	}

	@Test
	public void testJsonFormat() {
		// Create an artificial program
		LatestEventLoggerForTest log = createLog();

		// Add events
		log.recordEvent(0, 1);
		log.recordEvent(1, 1);
		log.recordEvent(0, 2);
		log.recordEvent(1, 2);
		log.recordEvent(1, 3);
		
		String json = log.getJsonOutput();
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(json);
			Assert.assertEquals("nearomni", node.get("format").asText());
			
			JsonNode event1 = node.get("events").get(0);
			Assert.assertEquals("myClass", event1.get("cname").asText());
			Assert.assertEquals("myMethod", event1.get("mname").asText());
			Assert.assertEquals("()I", event1.get("mdesc").asText());
			Assert.assertEquals("METHOD_ENTRY", event1.get("event").asText());
			Assert.assertEquals(2, event1.get("freq").asInt());
			Assert.assertEquals(2, event1.get("record").asInt());				
			Assert.assertEquals("void", event1.get("vtype").asText());				
			Assert.assertNull(event1.get("value"));

			JsonNode event2 = node.get("events").get(1);
			Assert.assertEquals("myClass", event2.get("cname").asText());
			Assert.assertEquals("myMethod", event2.get("mname").asText());
			Assert.assertEquals("()I", event2.get("mdesc").asText());
			Assert.assertEquals("METHOD_NORMAL_EXIT", event2.get("event").asText());
			Assert.assertEquals(3, event2.get("freq").asInt());
			Assert.assertEquals(3, event2.get("record").asInt());				
			Assert.assertEquals("int", event2.get("vtype").asText());				
			JsonNode values = event2.get("value");
			Assert.assertEquals(1, values.get(0).asInt());
			Assert.assertEquals(2, values.get(1).asInt());
			Assert.assertEquals(3, values.get(2).asInt());
		} catch (IOException e) {
			Assert.fail();
		}
	}	

	private int countColumns(String line) {
		return line.split(",", -1).length;
	}

	@Test
	public void testCsvFormat() {
		// Create an artificial program
		LatestEventLoggerForTest log = createLog();

		// Add events
		log.recordEvent(0, 1);
		log.recordEvent(1, 2);
		log.recordEvent(0, 3);
		log.recordEvent(1, 4);
		log.recordEvent(0, 5);
		log.recordEvent(1, 6);
		log.recordEvent(0, 7);
		
		String csv = log.getCsvOutput();
		LineNumberReader reader = new LineNumberReader(new StringReader(csv));
		try {
			// The number of columns in the header should be the same as other lines 
			String header = reader.readLine();
			Assert.assertNotNull(header);
			String[] columns = header.split(",");
			
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				int c = countColumns(line);
				Assert.assertEquals(columns.length, c);
			}
		} catch (IOException e) {
			Assert.fail();
		}
	}	

}
