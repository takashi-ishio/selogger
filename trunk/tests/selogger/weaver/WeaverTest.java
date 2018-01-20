package selogger.weaver;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import selogger.EventType;
import selogger.logging.EventLogger;
import selogger.logging.io.MemoryLogger;
import selogger.weaver.method.Descriptor;


public class WeaverTest {

	private WeaveLog weaveLog;
	private Class<?> wovenClass;
	private MemoryLogger memoryLogger;
	
	@Before
	public void setup() throws IOException {
		String className = "selogger/testdata/SimpleTarget";
		ClassReader r = new ClassReader(className);
		weaveLog = new WeaveLog(0, 0, 0);
		WeaverConfig config = new WeaverConfig(WeaverConfig.KEY_RECORD_DEFAULT); 
		ClassTransformer c = new ClassTransformer(weaveLog, config, r, this.getClass().getClassLoader());
		wovenClass = new WeaveClassLoader().createClass("selogger.testdata.SimpleTarget", c.getWeaveResult());
		memoryLogger = EventLogger.initializeForTest();
	}

	public class EventIterator {
		
		private int eventIndex;
		
		public EventIterator() {
			eventIndex = -1;
		}
		
		/**
		 * Proceed to the next event.
		 * This method must be called before calling other getter methods.
		 * @return true if the event data is available.
		 * False indicate the end of data.
		 */
		public boolean next() {
			eventIndex++;
			return eventIndex < memoryLogger.getEvents().size();
		}
		
		public int getDataId() {
			return memoryLogger.getEvents().get(eventIndex).getDataId();
		}
		
		public String getClassName() {
			int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
			int methodId = weaveLog.getDataEntries().get(dataId).getMethodId();
			return weaveLog.getMethods().get(methodId).getClassName();
		}
		
		public String getMethodName() {
			int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
			int methodId = weaveLog.getDataEntries().get(dataId).getMethodId();
			return weaveLog.getMethods().get(methodId).getMethodName();
		}
		
		public EventType getEventType() {
			int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
			return weaveLog.getDataEntries().get(dataId).getEventType();
		}

		public int getIntValue() {
			return memoryLogger.getEvents().get(eventIndex).getIntValue();
		}

		public double getDoubleValue() {
			return memoryLogger.getEvents().get(eventIndex).getDoubleValue();
		}
		
		public boolean getBooleanValue() {
			return memoryLogger.getEvents().get(eventIndex).getBooleanValue();
		}
		
		public byte getByteValue() {
			return memoryLogger.getEvents().get(eventIndex).getByteValue();
		}
		
		public char getCharValue() {
			return memoryLogger.getEvents().get(eventIndex).getCharValue();
		}
		
		public short getShortValue() {
			return memoryLogger.getEvents().get(eventIndex).getShortValue();
		}

		public Object getObjectValue() {
			return memoryLogger.getEvents().get(eventIndex).getObjectValue();
		}

		public Class<?> getValueType() {
			return memoryLogger.getEvents().get(eventIndex).getValueType();
		}

		public Descriptor getDataIdValueDesc() {
			int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
			return weaveLog.getDataEntries().get(dataId).getValueDesc();
		}
		
		public String getAttributes() {
			int dataId = memoryLogger.getEvents().get(eventIndex).getDataId();
			return weaveLog.getDataEntries().get(dataId).getAttributes();
		}
	}
	
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
		Assert.assertEquals(EventType.NEW_OBJECT_INITIALIZED, it.getEventType());
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
	
	@Test
	public void testWeaving() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();
		
		EventIterator it = new EventIterator();

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
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
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

	@Test
	public void testArray() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();
		
		EventIterator it = new EventIterator();

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
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
		Assert.assertEquals(Descriptor.Object, it.getDataIdValueDesc());
		Assert.assertEquals(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
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
	
	@Test
	public void testException() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();
		
		EventIterator it = new EventIterator();

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

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
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

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LOAD_INDEX, it.getEventType());
		Assert.assertEquals(0, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ARRAY_LOAD_FAIL, it.getEventType());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());

		Assert.assertTrue(it.next());
		int throwDataId = it.getDataId();
		Assert.assertEquals(EventType.THROW, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_EXCEPTIONAL_EXIT_LABEL, it.getEventType());
		Assert.assertEquals(throwDataId, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_EXCEPTIONAL_EXIT, it.getEventType());
		Assert.assertSame(result, it.getObjectValue());
		
		Assert.assertFalse(it.next());
	}
	
	@Test
	public void testSynchronization() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();
		
		EventIterator it = new EventIterator();

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

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.MONITOR_ENTER, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CALL, it.getEventType());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ACTUAL_PARAM, it.getEventType());
		Assert.assertEquals(1.0, it.getDoubleValue(), 0);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.ACTUAL_PARAM, it.getEventType());
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

	@Test
	public void testRead() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();
		
		EventIterator it = new EventIterator();

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

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.GET_STATIC_FIELD, it.getEventType());
		Assert.assertSame(1, it.getIntValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertSame(1, it.getIntValue());

		Assert.assertFalse(it.next());
	}
	
	@Test
	public void testMultiarray() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();
		
		EventIterator it = new EventIterator();

		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method exception = wovenClass.getMethod("multiarray", new Class<?>[]{byte.class, char.class});
		Object ret = exception.invoke(o, (byte)2, (char)2);

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("multiarray", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
		Assert.assertEquals(2, it.getByteValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
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

	@Test
	public void testConstString() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();
		
		EventIterator it = new EventIterator();

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

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CONSTANT_OBJECT_LOAD, it.getEventType());
		Assert.assertSame(ret, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertSame(ret, it.getObjectValue());
		
		Assert.assertFalse(it.next());
	}

	@Test
	public void testTypeCheck() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.newInstance();
		
		EventIterator it = new EventIterator();

		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method exception = wovenClass.getMethod("typeCheck", new Class<?>[] {Object.class});
		String param = "Test";
		Object ret = exception.invoke(o, param);
		
		Assert.assertTrue(((Boolean)ret).booleanValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_ENTRY, it.getEventType());
		Assert.assertEquals("typeCheck", it.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", it.getClassName());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
		Assert.assertSame(o, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.FORMAL_PARAM, it.getEventType());
		Assert.assertSame(param, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.INSTANCEOF, it.getEventType());
		Assert.assertSame(param, it.getObjectValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.INSTANCEOF_RESULT, it.getEventType());
		Assert.assertTrue(it.getBooleanValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, it.getEventType());
		Assert.assertTrue(it.getBooleanValue());

		Assert.assertFalse(it.next());
	}
	/*
	 * test cases:
	 * 型として float, double, boolean, byte を使う
	 * 例外を発生させての終了
	 * 例外を発生させての CATCH への移動
	 * 例外を THROWする
	 * メソッド呼び出しで例外を発生させる
	 * メソッド呼び出しから正常に戻り値を受け取る
	 * CALL - INVOKEVIRTUAL, INVOKE DYNAMIC
// ExcetpionalEXIT_LABEL とrecordLabel の関係が不明瞭。 LABEL あれば　EXIT_LABELは必要ないはず?
	 * 内部クラスに対する値の設定
	PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION,

	INVOKE_VIRTUAL に対応するCALL
	INVOKE DYNAMIC に対応するCALL
	新規オブジェクト作成、（できれば NEW 多段構成） NEW_OBJECT, NEW_OBJECT_CREATION_COMPLETED,
	インスタンス判定 INSTANCEOF, 	INSTANCEOF_RESULT,
	
	setUp が共通化できないもの：
	ラベル通過	LABEL,	JUMP,
	ローカル変数操作	LOCAL_LOAD, LOCAL_STORE, 
	RET;

	 */
	
	public static class WeaveClassLoader extends ClassLoader {
		
		public Class<?> createClass(String name, byte[] bytecode) {
			Class<?> c = defineClass(name, bytecode, 0, bytecode.length);
			resolveClass(c);
			return c;
		}
	}
	
}
