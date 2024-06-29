package selogger.weaver;

import org.junit.Assert;
import org.junit.Test;

public class ClassInfoTest {

	@Test
	public void testClassInfo() {
		ClassInfo c = new ClassInfo(1, "container", "filename", "classname", LogLevel.Normal, "0101", "#id", null);
		String s = c.toString();
		ClassInfo c2 = ClassInfo.parse(s);
		
		Assert.assertEquals(1, c.getClassId());
		Assert.assertEquals("#id", c.getClassLoaderIdentifier());
		Assert.assertEquals("classname", c.getClassName());
		Assert.assertEquals("filename", c.getFilename());
		Assert.assertEquals("container", c.getContainer());
		Assert.assertEquals(LogLevel.Normal, c.getLoglevel());
		Assert.assertEquals("0101", c.getHash());
		Assert.assertArrayEquals(new String[0], c.getAnnotations());

		Assert.assertEquals(1, c2.getClassId());
		Assert.assertEquals("#id", c2.getClassLoaderIdentifier());
		Assert.assertEquals("classname", c2.getClassName());
		Assert.assertEquals("filename", c2.getFilename());
		Assert.assertEquals("container", c2.getContainer());
		Assert.assertEquals(LogLevel.Normal, c2.getLoglevel());
		Assert.assertEquals("0101", c2.getHash());
		Assert.assertArrayEquals(new String[0], c2.getAnnotations());
	}

	@Test
	public void testClassInfoAnnotations() {
		ClassInfo c = new ClassInfo(1, "container", "filename", "classname", LogLevel.Normal, "0101", "#id", new String[] {"A1", "A2"});
		String s = c.toString();
		ClassInfo c2 = ClassInfo.parse(s);
		Assert.assertArrayEquals(new String[] {"A1", "A2"}, c.getAnnotations());
		Assert.assertArrayEquals(new String[] {"A1", "A2"}, c2.getAnnotations());
	}
}