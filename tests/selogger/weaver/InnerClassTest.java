package selogger.weaver;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import selogger.EventType;
import selogger.logging.Logging;
import selogger.logging.io.MemoryLogger;
import selogger.weaver.method.Descriptor;

/**
 * Test class for inner class
 */
public class InnerClassTest {

	private WeaveLog weaveLog;
	private Class<?> wovenClass;
	private Class<?> ownerClass;
	private MemoryLogger memoryLogger;
	private EventIterator it;
	
	/**
	 * Execute a weaving for a class and define them as Java classes 
	 */
	@Before
	public void setup() throws IOException, ClassNotFoundException {
		// Set up the weaver
		weaveLog = new WeaveLog(0, 0, 0);
		String className = "selogger/testdata/SimpleTarget$StringComparator";
		ClassReader r = new ClassReader(className);
		WeaveConfig config = new WeaveConfig(WeaveConfig.KEY_RECORD_DEFAULT); 
		
		// Execute the weaving
		ClassTransformer c = new ClassTransformer(weaveLog, config, r, this.getClass().getClassLoader());
		
		// Load the woven class
		WeaveClassLoader loader = new WeaveClassLoader();
		wovenClass = loader.createClass("selogger.testdata.SimpleTarget$StringComparator", c.getWeaveResult());
		
		memoryLogger = Logging.initializeForTest();
		
		// Load SimpleTarget class
		ClassReader r2 = new ClassReader("selogger/testdata/SimpleTarget");
		ownerClass = loader.createClass("selogger.testdata.SimpleTarget", r2.b);
		
		it = new EventIterator(memoryLogger, weaveLog);
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
		Object owner = ownerClass.newInstance();
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
		Assert.assertTrue(it.getAttributes().contains("java/lang/Object"));
		Assert.assertTrue(it.getAttributes().contains("<init>"));

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
