package selogger.weaver;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.IntUnaryOperator;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import selogger.EventType;
import selogger.logging.Logging;
import selogger.logging.io.MemoryLogger;
import selogger.weaver.method.Descriptor;


public class WeaverTest {

	private WeaveLog weaveLog;
	private Class<?> wovenClass;
	@SuppressWarnings("unused")
	private Class<?> innerClass;
	private MemoryLogger memoryLogger;
	private EventIterator it;
	
	/**
	 * Execute a weaving for a class and define them as Java classes 
	 */
	@Before
	public void setup() throws IOException {
		// Execute a weaving 
		weaveLog = new WeaveLog(0, 0, 0);
		String className = "selogger/testdata/SimpleTarget";
		ClassReader r = new ClassReader(className);
		WeaveConfig config = new WeaveConfig(WeaveConfig.KEY_RECORD_DEFAULT); 
		ClassTransformer c = new ClassTransformer(weaveLog, config, r, this.getClass().getClassLoader());
		// Load the woven class 
		WeaveClassLoader loader = new WeaveClassLoader();
		wovenClass = loader.createClass("selogger.testdata.SimpleTarget", c.getWeaveResult());
		memoryLogger = Logging.initializeForTest();
		
		ClassReader r2 = new ClassReader("selogger/testdata/SimpleTarget$StringComparator");
		innerClass = loader.createClass("selogger.testdata.SimpleTarget$StringComparator", r2.b) ;

		it = new EventIterator(memoryLogger, weaveLog);
	}
	
	/**
	 * Remove woven class definition from memory
	 */
	@After
	public void tearDown() {
		wovenClass = null;
		innerClass = null;
	}

	/**
	 * check the correctness of the observed events for a constructor call 
	 */
	private void testBaseEvents(EventIterator it, Object instance) {
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("<clinit>", it.getMethodName());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.PUT_STATIC_FIELD, it.getEventType());
		Assert.assertEquals(1, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(Descriptor.Void, it.getDataIdValueDesc());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("<init>", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL, it.getEventType());
		Assert.assertEquals("<init>", it.getMethodName());
		Assert.assertTrue(it.getAttributes().contains("java/lang/Object"));
		Assert.assertTrue(it.getAttributes().contains("CallType=ReceiverNotInitialized"));

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_RETURN, it.getEventType());
		Assert.assertEquals(Descriptor.Void, it.getDataIdValueDesc());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_OBJECT_INITIALIZED, it.getEventType());
		Assert.assertSame(instance, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.PUT_INSTANCE_FIELD, it.getEventType());
		Assert.assertSame(instance, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.PUT_INSTANCE_FIELD_VALUE, it.getEventType());
		Assert.assertEquals(Descriptor.Integer, it.getDataIdValueDesc());
		Assert.assertEquals(int.class, it.getValueType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(Descriptor.Void, it.getDataIdValueDesc());

	}
	
	/**
	 * Test an instance method "getField"
	 */
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
		Assert.assertEquals(Descriptor.Object, it.getDataIdValueDesc());
		Assert.assertEquals(o, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.GET_INSTANCE_FIELD, it.getEventType());
		Assert.assertEquals(Descriptor.Object, it.getDataIdValueDesc());
		Assert.assertEquals(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.GET_INSTANCE_FIELD_RESULT, it.getEventType());
		Assert.assertEquals(Descriptor.Integer, it.getDataIdValueDesc());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(Descriptor.Integer, it.getDataIdValueDesc());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertFalse(it.next());
	}

	/**
	 * Test an instance method "createArray"
	 */
	@Test
	public void testArray() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method createArray = wovenClass.getMethod("createArray", new Class<?>[] {int.class});
		Object array = createArray.invoke(o, 2);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("createArray", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertEquals(Descriptor.Object, it.getDataIdValueDesc());
		Assert.assertEquals(o, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertEquals(Descriptor.Integer, it.getDataIdValueDesc());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.NEW_ARRAY, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());
		Assert.assertTrue(it.getAttributes().contains("ElementType=short"));

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.NEW_ARRAY_RESULT, it.getEventType());
		Assert.assertSame(array, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LENGTH, it.getEventType());
		Assert.assertSame(array, it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LENGTH_RESULT, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_STORE, it.getEventType());
		Assert.assertSame(array, it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_STORE_INDEX, it.getEventType());
		Assert.assertEquals(0, it.getIntValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_STORE_VALUE, it.getEventType());
		Assert.assertEquals(0, it.getShortValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LENGTH, it.getEventType());
		Assert.assertSame(array, it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LENGTH_RESULT, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_STORE, it.getEventType());
		Assert.assertSame(array, it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_STORE_INDEX, it.getEventType());
		Assert.assertEquals(1, it.getIntValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_STORE_VALUE, it.getEventType());
		Assert.assertEquals(1, it.getShortValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LENGTH, it.getEventType());
		Assert.assertSame(array, it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LENGTH_RESULT, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LOAD, it.getEventType());
		Assert.assertSame(array, it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LOAD_INDEX, it.getEventType());
		Assert.assertEquals(0, it.getIntValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LOAD_RESULT, it.getEventType());
		Assert.assertEquals(0, it.getShortValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_STORE, it.getEventType());
		Assert.assertSame(array, it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_STORE_INDEX, it.getEventType());
		Assert.assertEquals(0, it.getIntValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_STORE_VALUE, it.getEventType());
		Assert.assertEquals(1, it.getShortValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_STORE, it.getEventType());
		Assert.assertSame(array, it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_STORE_INDEX, it.getEventType());
		Assert.assertEquals(1, it.getIntValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_STORE_VALUE, it.getEventType());
		Assert.assertEquals(2, it.getShortValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(Descriptor.Object, it.getDataIdValueDesc());
		Assert.assertSame(array, it.getObjectValue());
		
		Assert.assertFalse(it.next());
	}
	
	/**
	 * Test an instance method "exception"
	 */
	@Test
	public void testException() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();

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
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.NEW_ARRAY, it.getEventType());
		Assert.assertEquals(0, it.getIntValue());
		Assert.assertTrue(it.getAttributes().contains("ElementType=boolean"));

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.NEW_ARRAY_RESULT, it.getEventType());
		boolean[] array = (boolean[])it.getObjectValue();
		Assert.assertEquals(0, array.length);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LOAD, it.getEventType());
		Assert.assertSame(array, it.getObjectValue());
		int arrayLoad = it.getDataId();

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LOAD_INDEX, it.getEventType());
		Assert.assertEquals(0, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH_LABEL, it.getEventType());
		Assert.assertSame(arrayLoad, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());

		Assert.assertTrue(it.next());
		int throwDataId = it.getDataId();
		Assert.assertEquals(EventType.METHOD_THROW, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH_LABEL, it.getEventType());
		Assert.assertEquals(throwDataId, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH, it.getEventType());
		Assert.assertEquals(result, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_EXCEPTIONAL_EXIT, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());
		
		Assert.assertFalse(it.next());
	}
	
	/**
	 * Test an instance method "synchronization"
	 */
	@Test
	public void testSynchronization() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();

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
		Assert.assertEquals(EventType.MONITOR_ENTER, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MONITOR_ENTER_RESULT, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL, it.getEventType());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_PARAM, it.getEventType());
		Assert.assertEquals(1.0, it.getDoubleValue(), 0);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_PARAM, it.getEventType());
		Assert.assertEquals(2.0, it.getDoubleValue(), 0);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_RETURN, it.getEventType());
		Assert.assertEquals(2.0, it.getDoubleValue(), 0);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MONITOR_EXIT, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(2.0, it.getDoubleValue(), 0);

		Assert.assertFalse(it.next());
	}

	/**
	 * Test an instance method "read"
	 */
	@Test
	public void testRead() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();

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
		Assert.assertEquals(EventType.GET_STATIC_FIELD, it.getEventType());
		Assert.assertSame(1, it.getIntValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertSame(1, it.getIntValue());

		Assert.assertFalse(it.next());
	}
	
	/**
	 * Test an instance method "multiarray"
	 */
	@Test
	public void testMultiarray() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();
		
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
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertEquals(2, it.getByteValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertEquals(2, it.getCharValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MULTI_NEW_ARRAY, it.getEventType());
		Assert.assertSame(ret, it.getObjectValue());
		
		int[][][] array = (int[][][])ret;
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MULTI_NEW_ARRAY_OWNER, it.getEventType());
		Assert.assertEquals(array, it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MULTI_NEW_ARRAY_ELEMENT, it.getEventType());
		Assert.assertSame(array[0], it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MULTI_NEW_ARRAY_ELEMENT, it.getEventType());
		Assert.assertSame(array[1], it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MULTI_NEW_ARRAY_OWNER, it.getEventType());
		Assert.assertEquals(array[0], it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MULTI_NEW_ARRAY_ELEMENT, it.getEventType());
		Assert.assertSame(array[0][0], it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MULTI_NEW_ARRAY_ELEMENT, it.getEventType());
		Assert.assertSame(array[0][1], it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MULTI_NEW_ARRAY_OWNER, it.getEventType());
		Assert.assertEquals(array[1], it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MULTI_NEW_ARRAY_ELEMENT, it.getEventType());
		Assert.assertSame(array[1][0], it.getObjectValue());
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MULTI_NEW_ARRAY_ELEMENT, it.getEventType());
		Assert.assertSame(array[1][1], it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertSame(array, it.getObjectValue());
		
		Assert.assertFalse(it.next());
	}

	/**
	 * Test an instance method "constString"
	 */
	@Test
	public void testConstString() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();
		
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
		Assert.assertEquals(EventType.OBJECT_CONSTANT_LOAD, it.getEventType());
		Assert.assertSame(ret, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertSame(ret, it.getObjectValue());
		
		Assert.assertFalse(it.next());
	}

	/**
	 * Test an instance method "typeCheck"
	 */
	@Test
	public void testTypeCheck() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();

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
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertSame(param, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.OBJECT_INSTANCEOF, it.getEventType());
		Assert.assertSame(param, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.OBJECT_INSTANCEOF_RESULT, it.getEventType());
		Assert.assertTrue(it.getBooleanValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertTrue(it.getBooleanValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("typeCheck", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertSame(null, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.OBJECT_INSTANCEOF, it.getEventType());
		Assert.assertSame(null, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.OBJECT_INSTANCEOF_RESULT, it.getEventType());
		Assert.assertFalse(it.getBooleanValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertFalse(it.getBooleanValue());

		Assert.assertFalse(it.next());
	}
	
	/**
	 * Test an instance method "sort"
	 */
	@Test
	public void testSort() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

		// Event generation
		Object o = wovenClass.newInstance();
		
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
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertSame(param, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.NEW_OBJECT, it.getEventType());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL, it.getEventType());
		Assert.assertTrue(it.getAttributes().contains("<init>"));

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_PARAM, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_RETURN, it.getEventType());
		Assert.assertEquals(Descriptor.Void, it.getDataIdValueDesc());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.NEW_OBJECT_CREATED, it.getEventType());
		Assert.assertTrue(it.getObjectValue() instanceof Comparator);
		Comparator<?> comparator = (Comparator<?>)it.getObjectValue();

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL, it.getEventType());
		Assert.assertTrue(it.getAttributes().contains("sort"));

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_PARAM, it.getEventType());
		Assert.assertSame(param, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_PARAM, it.getEventType());
		Assert.assertSame(comparator, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_RETURN, it.getEventType());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());

		Assert.assertFalse(it.next());

	}

	/**
	 * Test an instance method "invokeVirtual"
	 */
	@Test
	public void testInvokeVirtual() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

		// Event generation
		Object o = wovenClass.newInstance();
		
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
		Assert.assertEquals(EventType.CALL, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("getField", it.getMethodName());
		Assert.assertSame(o, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.GET_INSTANCE_FIELD, it.getEventType());
		Assert.assertEquals(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.GET_INSTANCE_FIELD_RESULT, it.getEventType());
		Assert.assertEquals(Descriptor.Integer, it.getDataIdValueDesc());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(Descriptor.Integer, it.getDataIdValueDesc());
		Assert.assertEquals(2, it.getIntValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_RETURN, it.getEventType());
		Assert.assertEquals(Descriptor.Integer, it.getDataIdValueDesc());
		Assert.assertEquals(2, it.getIntValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL, it.getEventType());
		Assert.assertTrue(it.getAttributes().contains("CallType=Regular"));
		Assert.assertTrue(it.getAttributes().contains("getLong"));
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_PARAM, it.getEventType());
		Assert.assertEquals(2, it.getLongValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("getLong", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertEquals(Descriptor.Long, it.getDataIdValueDesc());
		Assert.assertEquals(2, it.getLongValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(3, it.getLongValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_RETURN, it.getEventType());
		Assert.assertEquals(3, it.getLongValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(3, it.getLongValue());

		Assert.assertFalse(it.next());
	}

	/**
	 * Test an instance method "invokeDynamic"
	 */
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
		Assert.assertEquals(EventType.INVOKE_DYNAMIC, it.getEventType());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.INVOKE_DYNAMIC_RESULT, it.getEventType());
		IntUnaryOperator f = (IntUnaryOperator)it.getObjectValue();

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL, it.getEventType());
		Assert.assertSame(f, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_PARAM, it.getEventType());
		Assert.assertEquals(1, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertTrue(it.getMethodName().contains("lambda"));
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertTrue(it.getAttributes().contains("Receiver=false"));

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertEquals(1, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_RETURN, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertFalse(it.next());
	}

	/**
	 * Test an instance method "invokeDynamic2"
	 */
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
		Assert.assertEquals(EventType.INVOKE_DYNAMIC, it.getEventType());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.INVOKE_DYNAMIC_PARAM, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.INVOKE_DYNAMIC_RESULT, it.getEventType());
		IntUnaryOperator f = (IntUnaryOperator)it.getObjectValue();

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL, it.getEventType());
		Assert.assertSame(f, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_PARAM, it.getEventType());
		Assert.assertEquals(1, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertTrue(it.getMethodName().contains("lambda"));
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertTrue(it.getAttributes().contains("Receiver=true"));
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertEquals(1, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.GET_INSTANCE_FIELD, it.getEventType());
		Assert.assertEquals(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.GET_INSTANCE_FIELD_RESULT, it.getEventType());
		Assert.assertEquals(Descriptor.Integer, it.getDataIdValueDesc());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_RETURN, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertFalse(it.next());
	}

	/**
	 * Test an instance method "invokeDynamic3"
	 */
	@Test
	public void testInvokeDynamic3() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method exception = wovenClass.getMethod("invokeDynamic3", new Class<?>[0]);
		Object ret = exception.invoke(o);
		
		Assert.assertEquals(7, ((Integer)ret).intValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("invokeDynamic3", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.INVOKE_DYNAMIC, it.getEventType());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.INVOKE_DYNAMIC_PARAM, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.INVOKE_DYNAMIC_PARAM, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.INVOKE_DYNAMIC_PARAM, it.getEventType());
		Assert.assertEquals(3, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.INVOKE_DYNAMIC_RESULT, it.getEventType());
		IntUnaryOperator f = (IntUnaryOperator)it.getObjectValue();

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL, it.getEventType());
		Assert.assertSame(f, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_PARAM, it.getEventType());
		Assert.assertEquals(1, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertTrue(it.getMethodName().contains("lambda"));
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertTrue(it.getAttributes().contains("Receiver=true"));
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertEquals(3, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_PARAM, it.getEventType());
		Assert.assertEquals(1, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.GET_INSTANCE_FIELD, it.getEventType());
		Assert.assertEquals(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.GET_INSTANCE_FIELD_RESULT, it.getEventType());
		Assert.assertEquals(Descriptor.Integer, it.getDataIdValueDesc());
		Assert.assertEquals(2, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(7, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL_RETURN, it.getEventType());
		Assert.assertEquals(7, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertEquals(7, it.getIntValue());

		Assert.assertFalse(it.next());
	}

	/**
	 * Test an instance method "getFloat"
	 */
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
	
	/**
	 * Test an instance method "exceptionInCall"
	 */
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
		Assert.assertEquals(EventType.CALL, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());
		int callDataId = it.getDataId();

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("exception", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.NEW_ARRAY, it.getEventType());
		Assert.assertEquals(0, it.getIntValue());
		Assert.assertTrue(it.getAttributes().contains("ElementType=boolean"));

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.NEW_ARRAY_RESULT, it.getEventType());
		boolean[] array = (boolean[])it.getObjectValue();
		Assert.assertEquals(0, array.length);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LOAD, it.getEventType());
		Assert.assertSame(array, it.getObjectValue());
		int arrayload = it.getDataId();

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LOAD_INDEX, it.getEventType());
		Assert.assertEquals(0, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH_LABEL, it.getEventType());
		Assert.assertEquals(arrayload, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());

		Assert.assertTrue(it.next());
		int throwDataId = it.getDataId();
		Assert.assertEquals(EventType.METHOD_THROW, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH_LABEL, it.getEventType());
		Assert.assertEquals(throwDataId, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH, it.getEventType());
		Assert.assertEquals(result, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_EXCEPTIONAL_EXIT, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH_LABEL, it.getEventType());
		Assert.assertEquals(callDataId, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH, it.getEventType());
		Assert.assertEquals(result, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_EXCEPTIONAL_EXIT, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());

		Assert.assertFalse(it.next());
	}

	
}
