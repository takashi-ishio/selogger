package selogger.logging.io;

import org.junit.Assert;
import org.junit.Test;


public class LatestEventBufferTest {

	@Test
	public void testSizeExtension() {
		int SIZE = 2048;
		LatestEventBuffer buf = new LatestEventBuffer(int.class, "int", SIZE, null);
		for (int i=1; i<=65536; i++) {
			buf.addInt(i, i, i);
			Assert.assertEquals(Math.min(i, SIZE), buf.size());
			Assert.assertEquals(i, buf.count());
			Assert.assertEquals(i < SIZE ? 1 : i-SIZE+1, buf.getInt(0));
			Assert.assertEquals(i < SIZE ? 1 : i-SIZE+1, buf.getSeqNum(0));
			Assert.assertEquals(i < SIZE ? 1 : i-SIZE+1, buf.getThreadId(0));
		}

		buf = new LatestEventBuffer(long.class, "long", SIZE, null);
		for (int i=1; i<=65536; i++) {
			buf.addLong(i, i, i);
			Assert.assertEquals(Math.min(i, SIZE), buf.size());
			Assert.assertEquals(i, buf.count());
			Assert.assertEquals(i < SIZE ? 1 : i-SIZE+1, buf.getLong(0));
		}
	}
}
