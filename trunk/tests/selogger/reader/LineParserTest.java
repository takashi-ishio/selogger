package selogger.reader;


import org.junit.Assert;
import org.junit.Test;

public class LineParserTest {

	@Test
	public void testParseEndWithSeparator() {
		String line = "0,1,2,3,4,";
		LineParser parser = new LineParser(line);
		Assert.assertEquals(0, parser.readInt());
		Assert.assertEquals(1, parser.readInt());
		Assert.assertEquals(2, parser.readInt());
		Assert.assertEquals(3, parser.readInt());
		Assert.assertEquals(4, parser.readInt());
		Assert.assertEquals("", parser.readString());
	}
	
	@Test
	public void testParseStrings() {
		String line = "0,1,2,3,4";
		LineParser parser = new LineParser(line);
		Assert.assertEquals("0", parser.readString());
		Assert.assertEquals("1", parser.readString());
		Assert.assertEquals("2", parser.readString());
		Assert.assertEquals("3", parser.readString());
		Assert.assertEquals("4", parser.readString());
		Assert.assertEquals("", parser.readString());
	}

	@Test
	public void testParseLong() {
		String line = "0,1,2,3,4";
		LineParser parser = new LineParser(line);
		Assert.assertEquals(0L, parser.readLong());
		Assert.assertEquals(1L, parser.readLong());
		Assert.assertEquals(2L, parser.readLong());
		Assert.assertEquals(3L, parser.readLong());
	}
}
