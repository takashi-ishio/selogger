package selogger.weaver;

import org.junit.Assert;
import org.junit.Test;

public class MethodInfoTest {

	@Test
	public void testMethodInfo() {
		MethodInfo m = new MethodInfo(123, 456, "classname", "methodname", "desc", 7, "source", "0123456789ABCDEF", null, null);
		String s = m.toString();
		MethodInfo m2 = MethodInfo.parse(s);
		
		Assert.assertEquals(123, m.getClassId());
		Assert.assertEquals(456, m.getMethodId());
		Assert.assertEquals("classname", m.getClassName());
		Assert.assertEquals("methodname", m.getMethodName());
		Assert.assertEquals("desc", m.getMethodDesc());
		Assert.assertEquals(7, m.getAccess());
		Assert.assertEquals("source", m.getSourceFileName());
		Assert.assertEquals("0123456789ABCDEF", m.getMethodHash());
		Assert.assertEquals("01234567", m.getShortMethodHash());
		Assert.assertArrayEquals(new String[0], m.getVisibleAnnotations());
		Assert.assertArrayEquals(new String[0], m.getInvisibleAnnotations());
		
		Assert.assertEquals(123, m2.getClassId());
		Assert.assertEquals(456, m2.getMethodId());
		Assert.assertEquals("classname", m2.getClassName());
		Assert.assertEquals("methodname", m2.getMethodName());
		Assert.assertEquals("desc", m2.getMethodDesc());
		Assert.assertEquals(7, m2.getAccess());
		Assert.assertEquals("source", m2.getSourceFileName());
		Assert.assertEquals("0123456789ABCDEF", m2.getMethodHash());
		Assert.assertEquals("01234567", m2.getShortMethodHash());
		Assert.assertArrayEquals(new String[0], m2.getVisibleAnnotations());
		Assert.assertArrayEquals(new String[0], m2.getInvisibleAnnotations());
	}

	@Test
	public void testMethodInfoAttributes() {
		MethodInfo m = new MethodInfo(123, 456, "classname", "methodname", "desc", 7, "source", "0123456789ABCDEF", new String[]{"A1", "A2"}, new String[]{"A3"});
		String s = m.toString();
		MethodInfo m2 = MethodInfo.parse(s);
		Assert.assertArrayEquals(new String[] {"A1", "A2"}, m.getVisibleAnnotations());
		Assert.assertArrayEquals(new String[] {"A3"}, m.getInvisibleAnnotations());
		Assert.assertArrayEquals(new String[] {"A1", "A2"}, m2.getVisibleAnnotations());
		Assert.assertArrayEquals(new String[] {"A3"}, m2.getInvisibleAnnotations());
		
	}
}
