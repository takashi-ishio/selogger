package selogger.weaver;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

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
 * This class tests the events recorded by the "EXEC" configuration.
 * (WeaverTestAll checks only the frequency of events) 
 */
public class WeaverExecTest {

	private WeaveLog weaveLog;
	private Class<?> wovenClass;
	@SuppressWarnings("unused")
	private Class<?> innerClass;
	private MemoryLogger memoryLogger;
	private EventIterator it;
	
	
	@Before
	public void setup() throws IOException {
		weaveLog = new WeaveLog(0, 0, 0);
		String className = "selogger/testdata/SimpleTarget";
		ClassReader r = new ClassReader(className);
		WeaveConfig config = new WeaveConfig(WeaveConfig.KEY_RECORD_EXEC); 
		ClassTransformer c = new ClassTransformer(weaveLog, config, r, this.getClass().getClassLoader());
		WeaveClassLoader loader = new WeaveClassLoader();
		wovenClass = loader.createClass("selogger.testdata.SimpleTarget", c.getWeaveResult());
		memoryLogger = Logging.initializeForTest();
		
		innerClass = loader.loadClassFromResource("selogger.testdata.SimpleTarget$StringComparator", "selogger/testdata/SimpleTarget$StringComparator.class");

		it = new EventIterator(memoryLogger, weaveLog);
	}
	
	@After
	public void tearDown() {
		wovenClass = null;
		innerClass = null;
	}

	
	private void testBaseEvents(EventIterator it, Object instance) {
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("<clinit>", it.getMethodName());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(Descriptor.Void, it.getDataIdValueDesc());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("<init>", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_OBJECT_INITIALIZED, it.getEventType());
		Assert.assertSame(instance, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(Descriptor.Void, it.getDataIdValueDesc());

	}
	
	@Test
	public void testWeaving() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);
		
		// Execute another method
		Method method = wovenClass.getMethod("getField", new Class<?>[0]);
		method.invoke(o);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("getField", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(Descriptor.Integer, it.getDataIdValueDesc());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertFalse(it.next());
	}

	@Test
	public void testArray() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method createArray = wovenClass.getMethod("createArray", new Class<?>[] {int.class});
		Object array = createArray.invoke(o, 2);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("createArray", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(Descriptor.Object, it.getDataIdValueDesc());
		Assert.assertSame(array, it.getObjectValue());
		
		Assert.assertFalse(it.next());
	}
	
	@Test
	public void testException() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();

		// Check events
		testBaseEvents(it, o);

		Throwable result = null;
		// Execute a method
		Method exception = wovenClass.getMethod("exception", new Class<?>[0]);
		try {
			exception.invoke(o);
		} catch (InvocationTargetException e) {
			result = e.getCause();
		}

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("exception", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());

//		Assert.assertTrue(it.next());
//		int throwDataId = it.getDataId();
//		Assert.assertEquals(EventType.THROW, it.getEventType());
//		Assert.assertSame(result, it.getObjectValue());
//
//		Assert.assertTrue(it.next());
//		Assert.assertEquals(EventType.METHOD_EXCEPTIONAL_EXIT_LABEL, it.getEventType());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_THROW, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_EXCEPTIONAL_EXIT, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());
		
		Assert.assertFalse(it.next());
	}
	
	@Test
	public void testSynchronization() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();

		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method exception = wovenClass.getMethod("synchronization", new Class<?>[0]);
		Object ret = exception.invoke(o);
		
		Assert.assertEquals(2.0, ((Double)ret).doubleValue(), 0);
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("synchronization", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(2.0, it.getDoubleValue(), 0);

		Assert.assertFalse(it.next());
	}

	@Test
	public void testRead() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();

		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method exception = wovenClass.getMethod("read", new Class<?>[0]);
		Object ret = exception.invoke(o);
		Assert.assertEquals(1, ((Integer)ret).intValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("read", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertSame(1, it.getIntValue());

		Assert.assertFalse(it.next());
	}
	
	@Test
	public void testMultiarray() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method exception = wovenClass.getMethod("multiarray", new Class<?>[]{byte.class, char.class});
		Object ret = exception.invoke(o, (byte)2, (char)2);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("multiarray", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());


		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertSame(ret, it.getObjectValue());
		
		Assert.assertFalse(it.next());
	}

	@Test
	public void testConstString() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method exception = wovenClass.getMethod("constString", new Class<?>[0]);
		Object ret = exception.invoke(o);
		
		Assert.assertEquals("TEST", ret);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("constString", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertSame(ret, it.getObjectValue());
		
		Assert.assertFalse(it.next());
	}

	@Test
	public void testTypeCheck() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();

		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method exception = wovenClass.getMethod("typeCheck", new Class<?>[] {Object.class});
		String param = "Test";
		Object ret1 = exception.invoke(o, param);
		Object ret2 = exception.invoke(o, new Object[]{null});
		
		Assert.assertTrue(((Boolean)ret1).booleanValue());
		Assert.assertFalse(((Boolean)ret2).booleanValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("typeCheck", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertTrue(it.getBooleanValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("typeCheck", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertFalse(it.getBooleanValue());

		Assert.assertFalse(it.next());
	}
	
	@Test
	public void testSort() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method exception = wovenClass.getMethod("sort", new Class<?>[] {ArrayList.class});
		ArrayList<String> param = new ArrayList<>();
		param.add("A");
		param.add("B");
		exception.invoke(o, param);
		
		Assert.assertEquals("B", param.get(0));
		Assert.assertEquals("A", param.get(1));
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("sort", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());

		Assert.assertFalse(it.next());

	}

	@Test
	public void testInvokeVirtual() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method exception = wovenClass.getMethod("invokeVirtual", new Class<?>[0]);
		Object ret = exception.invoke(o);
		
		Assert.assertEquals(3, ((Long)ret).longValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("invokeVirtual", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("getField", it.getMethodName());
		Assert.assertSame(o, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(Descriptor.Integer, it.getDataIdValueDesc());
		Assert.assertEquals(2, it.getIntValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("getLong", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(3, it.getLongValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(3, it.getLongValue());

		Assert.assertFalse(it.next());
	}

	@Test
	public void testInvokeDynamic() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method exception = wovenClass.getMethod("invokeDynamic", new Class<?>[0]);
		Object ret = exception.invoke(o);
		
		Assert.assertEquals(2, ((Integer)ret).intValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("invokeDynamic", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertTrue(it.getMethodName().contains("lambda"));
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertTrue(it.getAttributes().contains("Receiver=false"));

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertFalse(it.next());
	}

	@Test
	public void testInvokeDynamic2() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method exception = wovenClass.getMethod("invokeDynamic2", new Class<?>[0]);
		Object ret = exception.invoke(o);
		
		Assert.assertEquals(2, ((Integer)ret).intValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("invokeDynamic2", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertTrue(it.getMethodName().contains("lambda"));
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertTrue(it.getAttributes().contains("Receiver=true"));
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertFalse(it.next());
	}

	@Test
	public void testFloat() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method m = wovenClass.getMethod("getFloat", new Class<?>[0]);
		m.invoke(o);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("getFloat", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(1.0F, it.getFloatValue(), 0);

		Assert.assertFalse(it.next());
	}
	
	@Test
	public void testExceptionInCall() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method m = wovenClass.getMethod("exceptionInCall", new Class<?>[0]);
		Throwable result = null;
		try {
			m.invoke(o);
		} catch (InvocationTargetException e) {
			result = e.getCause();
		}

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("exceptionInCall", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("exception", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_THROW, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_EXCEPTIONAL_EXIT, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_EXCEPTIONAL_EXIT, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());

		Assert.assertFalse(it.next());
	}


}
