package selogger.logging.io;


import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import selogger.logging.io.LatestEventLogger.ObjectRecordingStrategy;
import selogger.logging.util.JsonBuffer;
import selogger.logging.util.ObjectId;


public class LatestEventBufferTest {

	@Test
	public void testSizeExtension() {
		int SIZE = 2048;
		LatestEventBuffer buf = new LatestEventBuffer(int.class, SIZE, null);
		for (int i=1; i<=65536; i++) {
			buf.addInt(i, i, i);
			Assert.assertEquals(Math.min(i, SIZE), buf.size());
			Assert.assertEquals(i, buf.count());
			Assert.assertEquals(i < SIZE ? 1 : i-SIZE+1, buf.getInt(0));
			Assert.assertEquals(i < SIZE ? 1 : i-SIZE+1, buf.getSeqNum(0));
			Assert.assertEquals(i < SIZE ? 1 : i-SIZE+1, buf.getThreadId(0));
		}

		buf = new LatestEventBuffer(long.class,SIZE, null);
		for (int i=1; i<=65536; i++) {
			buf.addLong(i, i, i);
			Assert.assertEquals(Math.min(i, SIZE), buf.size());
			Assert.assertEquals(i, buf.count());
			Assert.assertEquals(i < SIZE ? 1 : i-SIZE+1, buf.getLong(0));
		}
	}
	
	@Test
	public void testToString() {
		LatestEventBuffer buf = new LatestEventBuffer(int.class, 4, null);
		buf.addInt(1, 0, 0);
		buf.addInt(2, 1, 0);
		buf.addInt(3, 2, 0);

		Assert.assertEquals("3,3,1,0,0,2,1,0,3,2,0,,,", buf.toString());
		Assert.assertEquals(3, buf.count());
		Assert.assertEquals(3, buf.size());

	}
	
	@Test
	public void testWriteJson() {
		LatestEventBuffer buf = new LatestEventBuffer(double.class, 4, null);
		buf.addDouble(0, 0, 0);
		buf.addDouble(1.0, 0, 0);
		buf.addDouble(2.0, 0, 0);
		JsonBuffer json = new JsonBuffer();
		buf.writeJson(json, false);
		String jsonStr = json.toString();
		Assert.assertTrue(jsonStr.contains("\"freq\":3"));
		Assert.assertTrue(jsonStr.contains("\"record\":3"));
		Assert.assertTrue(jsonStr.contains("\"value\":[0.0,1.0,2.0]"));
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode node = mapper.readTree(jsonStr);
			Assert.assertNotNull(node);
		} catch (IOException e) {
			Assert.fail("invalid json format");
		}
	}

	@Test
	public void testWriteJsonObjectId() {
		LatestEventBuffer buf = new LatestEventBuffer(ObjectId.class, 4, ObjectRecordingStrategy.Id);
		buf.addObjectId(new ObjectId(1, "abc", "def"), 0, 0);
		JsonBuffer json = new JsonBuffer();
		buf.writeJson(json, false);
		String jsonStr = json.toString();
		Assert.assertTrue(jsonStr.contains("\"freq\":1"));
		Assert.assertTrue(jsonStr.contains("\"record\":1"));
		Assert.assertTrue(jsonStr.contains("\"value\":[{\"id\":\"1\",\"type\":\"abc\",\"str\":\"def\"}]"));		

		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode node = mapper.readTree(jsonStr);
			Assert.assertNotNull(node);
		} catch (IOException e) {
			Assert.fail("invalid json format");
		}
	}

	@Test
	public void testWriteJsonObject() {
		LatestEventBuffer buf = new LatestEventBuffer(Object.class, 4, ObjectRecordingStrategy.Strong);
		buf.addObject("abc", 0, 0);
		JsonBuffer json = new JsonBuffer();
		buf.writeJson(json, false);
		String jsonStr = json.toString();
		Assert.assertTrue(jsonStr.contains("\"freq\":1"));
		Assert.assertTrue(jsonStr.contains("\"record\":1"));
		Assert.assertTrue(jsonStr.contains("\"str\":\"abc\""));
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode node = mapper.readTree(jsonStr);
			Assert.assertNotNull(node);
		} catch (IOException e) {
			Assert.fail("invalid json format");
		}
	}


}
