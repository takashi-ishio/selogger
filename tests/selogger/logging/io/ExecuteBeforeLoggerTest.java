package selogger.logging.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExecuteBeforeLoggerTest {

	/**
	 * Test a counter
	 */
	@Test
	public void testExecutionCounter() {
		ExecuteBeforeLogger.EventCounter counter = new ExecuteBeforeLogger.EventCounter(10);
		Assert.assertEquals("Thread ID should be recorded", 10, counter.getThreadId());
		// The initial counter records no events
		Assert.assertEquals(0, counter.getFrequency(1));
		Assert.assertEquals(0, counter.getMaxId());
		Assert.assertTrue(counter.isFirst(0));
		Assert.assertTrue(counter.isFirst(1));
		
		// record an event of dataId 3
		counter.increment(3); 
		Assert.assertEquals(3, counter.getMaxId());
		Assert.assertEquals(1, counter.getFrequency(3));
		Assert.assertTrue(counter.isFirst(0));
		Assert.assertFalse("dataId 3 has been already recorded.", counter.isFirst(3));
		
		// Record three more events
		counter.increment(6);
		counter.increment(2);
		counter.increment(3);
		// The vector object should record the events
		Assert.assertEquals(6, counter.getMaxId());
		Assert.assertEquals(2, counter.getFrequency(3));
		Assert.assertEquals(1, counter.getFrequency(2));
		Assert.assertEquals(1, counter.getFrequency(6));
		
		// Record a large dataId
		counter.increment(1000000);
		Assert.assertEquals(1000000, counter.getMaxId());
	}
	
	/**
	 * Test the behavior of first-occurrence recording
	 */
	@Test
	public void testJson() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ExecuteBeforeLogger logger = new ExecuteBeforeLogger(out, null, null);
		logger.recordEvent(1, 0);
		logger.recordEvent(1, 0);
		logger.recordEvent(3, 0);
		logger.recordEvent(1, 0);
		logger.close();
		JsonFactory factory = new JsonFactory();
		try {
			 ObjectMapper mapper = new ObjectMapper(factory);
			 JsonNode root = mapper.readTree(out.toByteArray());
			 Assert.assertEquals("execute-before", root.get(ExecuteBeforeLogger.FIELD_FORMAT).asText());
			 JsonNode records = root.get(ExecuteBeforeLogger.FIELD_RECORDS);
			 Assert.assertEquals(2, records.size());

			 // Recorded before the first (1, 0)
			 JsonNode record1 = records.get(0);
			 Assert.assertEquals(1, record1.get(ExecuteBeforeLogger.FIELD_DATA_ID).asLong());
			 Assert.assertEquals(1, record1.get(ExecuteBeforeLogger.FIELD_VECTOR_LENGTH).asInt());
			 JsonNode vector1 = record1.get(ExecuteBeforeLogger.FIELD_STATE);
			 Assert.assertEquals(0, vector1.get(0).asLong());
			 Assert.assertEquals(1, vector1.size());
			 
			 // Recorded before the first (3, 0)
			 JsonNode record2 = records.get(1);
			 Assert.assertEquals(3, record2.get(ExecuteBeforeLogger.FIELD_DATA_ID).asLong());
			 Assert.assertEquals(2, record2.get(ExecuteBeforeLogger.FIELD_VECTOR_LENGTH).asInt());
			 JsonNode vector2 = record2.get(ExecuteBeforeLogger.FIELD_STATE);
			 Assert.assertEquals(0, vector2.get(0).asLong());
			 Assert.assertEquals(2, vector2.get(1).asLong());
			 Assert.assertEquals(2, vector2.size());
			 
			 // Recorded at the end
			 JsonNode record3 = root.get(ExecuteBeforeLogger.FIELD_FINAL_RECORDS).get(0);
			 Assert.assertEquals(4, record3.get(ExecuteBeforeLogger.FIELD_VECTOR_LENGTH).asInt());
			 JsonNode vector3 = record3.get(ExecuteBeforeLogger.FIELD_STATE);
			 Assert.assertEquals(0, vector3.get(0).asLong());
			 Assert.assertEquals(3, vector3.get(1).asLong());
			 Assert.assertEquals(0, vector3.get(2).asLong());
			 Assert.assertEquals(1, vector3.get(3).asLong());
			 Assert.assertEquals(4, vector3.size());
		} catch (IOException e) {
			Assert.fail();
		}
	}
	
}
