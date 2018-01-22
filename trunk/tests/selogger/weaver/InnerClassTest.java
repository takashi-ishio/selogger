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
import selogger.logging.EventLogger;
import selogger.logging.io.MemoryLogger;
import selogger.weaver.WeaverTest.WeaveClassLoader;

public class InnerClassTest {

	private WeaveLog weaveLog;
	private Class<?> wovenClass;
	private Class<?> ownerClass;
	private MemoryLogger memoryLogger;
	private EventIterator it;
	
	@Before
	public void setup() throws IOException, ClassNotFoundException {
		weaveLog = new WeaveLog(0, 0, 0);
		String className = "selogger/testdata/SimpleTarget$StringComparator";
		ClassReader r = new ClassReader(className);
		WeaverConfig config = new WeaverConfig(WeaverConfig.KEY_RECORD_DEFAULT); 
		ClassTransformer c = new ClassTransformer(weaveLog, config, r, this.getClass().getClassLoader());
		WeaveClassLoader loader = new WeaveClassLoader();
		wovenClass = loader.createClass("selogger.testdata.SimpleTarget$StringComparator", c.getWeaveResult());
		memoryLogger = EventLogger.initializeForTest();
		
		ClassReader r2 = new ClassReader("selogger/testdata/SimpleTarget");
		;
		//ClassTransformer c2 = new ClassTransformer(weaveLog, new WeaverConfig("PARAM"), r2, this.getClass().getClassLoader());

		ownerClass = loader.createClass("selogger.testdata.SimpleTarget", r2.b) ;//c2.getWeaveResult());
		it = new EventIterator(memoryLogger, weaveLog);
	}
	
	@After
	public void tearDown() {
		wovenClass = null;
		ownerClass = null;
	}

	@Test
	public void testSort() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		Constructor<?> c = wovenClass.getConstructor(new Class<?>[]{ownerClass});
		Object owner = ownerClass.newInstance();
		Object o = c.newInstance(owner);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("selogger/testdata/SimpleTarget$StringComparator", it.getClassName());
		Assert.assertEquals("<init>", it.getMethodName());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
		Assert.assertSame(owner, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION, it.getEventType());
		Assert.assertSame(owner, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL, it.getEventType());
		Assert.assertTrue(it.getAttributes().contains("java/lang/Object"));
		Assert.assertTrue(it.getAttributes().contains("<init>"));
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.NEW_OBJECT_INITIALIZED, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		
		Assert.assertFalse(it.next());
	}
	
}
