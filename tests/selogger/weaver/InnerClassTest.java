package selogger.weaver;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import selogger.EventType;
import selogger.logging.Logging;
import selogger.logging.io.MemoryLogger;
import selogger.testutil.WeaveClassLoader;
import selogger.weaver.method.Descriptor;

/**
 * Test class for inner class
 */
public class InnerClassTest {

	private Class<?> wovenClass;
	private Class<?> ownerClass;
	private MemoryLogger memoryLogger;
	private EventIterator it;
	
	/**
	 * Execute a weaving for a class and define them as Java classes 
	 */
	@Before
	public void setup() throws IOException, ClassNotFoundException {
		// Load the woven class
		WeaveConfig config = new WeaveConfig(WeaveConfig.KEY_RECORD_DEFAULT); 
		WeaveClassLoader loader = new WeaveClassLoader(config);
		wovenClass = loader.loadAndWeaveClass("selogger.testdata.SimpleTarget$StringComparator");
		ownerClass = loader.loadClassFromResource("selogger.testdata.SimpleTarget", "selogger/testdata/SimpleTarget.class");

		memoryLogger = new MemoryLogger();
		Logging.setLogger(memoryLogger);
		it = new EventIterator(memoryLogger, loader.getWeaveLog());
	}
	
	/**
	 * Remove woven class definition from memory
	 */
	@After
	public void tearDown() {
		wovenClass = null;
		ownerClass = null;
	}

	/**
	 * Execute a constructor of the woven class and 
	 * check the correctness of the observed events 
	 */
	@Test
	public void testSort() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Create an instance of woven class
		Constructor<?> c = wovenClass.getConstructor(new Class<?>[]{ownerClass});
		Object owner = ownerClass.getDeclaredConstructor().newInstance();
		Object o = c.newInstance(owner);

		// Check the correctness of the recorded event sequence
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("selogger/testdata/SimpleTarget$StringComparator", it.getClassName());
		Assert.assertEquals("<init>", it.getMethodName());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertSame(owner, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION, it.getEventType());
		Assert.assertSame(owner, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL, it.getEventType());
		Assert.assertEquals("java.lang.Object", it.getAttributes().getStringValue("owner", ""));
		Assert.assertEquals("<init>", it.getAttributes().getStringValue("name", ""));

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_RETURN, it.getEventType());
		Assert.assertEquals(Descriptor.Void, it.getDataIdValueDesc());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_OBJECT_INITIALIZED, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		
		Assert.assertFalse(it.next());
	}
	
}
