package selogger.weaver;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import selogger.EventType;
import selogger.logging.EventLogger;
import selogger.logging.io.MemoryLogger;
import selogger.weaver.method.Descriptor;


public class WeaverTest {

	
	private static EventType getEventType(WeaveLog log, MemoryLogger.Event event) {
		return log.getDataEntries().get(event.getDataId()).getEventType();
	}
	
	@Test
	public void testWeaving() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		String className = "selogger/testdata/SimpleTarget";
		ClassReader r = new ClassReader(className);
		WeaveLog log = new WeaveLog(0, 0, 0);
		WeaverConfig config = new WeaverConfig(WeaverConfig.KEY_RECORD_DEFAULT); 
		ClassTransformer c = new ClassTransformer(log, config, r, this.getClass().getClassLoader());
		
		MemoryLogger m = EventLogger.initializeForTest();
		Class<?> wovenClass = new WeaveClassLoader().createClass("selogger.testdata.SimpleTarget", c.getWeaveResult());

		// Event generation
		Object o = wovenClass.newInstance();

		// Check events
		Assert.assertEquals(8, m.getEvents().size());
		ArrayList<MemoryLogger.Event> events = m.getEvents();
		DataIdEntry d0 = log.getDataEntries().get(events.get(0).getDataId());
		MethodEntry entry = log.getMethods().get(d0.getMethodId());
		Assert.assertEquals(EventType.METHOD_ENTRY, d0.getEventType());
		Assert.assertEquals("<clinit>", entry.getMethodName());
		
		MemoryLogger.Event e1 = events.get(1);
		DataIdEntry d1 = log.getDataEntries().get(e1.getDataId());
		Assert.assertEquals(EventType.PUT_STATIC_FIELD, d1.getEventType());
		Assert.assertEquals(1, e1.getIntValue());

		MemoryLogger.Event e2 = events.get(2);
		DataIdEntry d2 = log.getDataEntries().get(e2.getDataId());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, d2.getEventType());
		Assert.assertEquals(Descriptor.Void, d2.getValueDesc());
		
		MemoryLogger.Event e3 = events.get(3);
		DataIdEntry d3 = log.getDataEntries().get(e3.getDataId());
		Assert.assertEquals(EventType.METHOD_ENTRY, d3.getEventType());
		MethodEntry entry3 = log.getMethods().get(d3.getMethodId());
		Assert.assertEquals("<init>", entry3.getMethodName());
		Assert.assertEquals("selogger/testdata/SimpleTarget", entry3.getClassName());
		
		MemoryLogger.Event e4 = events.get(4);
		DataIdEntry d4 = log.getDataEntries().get(e4.getDataId());
		Assert.assertEquals(EventType.CALL, d4.getEventType());
		MethodEntry entry4 = log.getMethods().get(d4.getMethodId());
		Assert.assertEquals("<init>", entry4.getMethodName());
		Assert.assertTrue(d4.getAttributes().contains("java/lang/Object"));

		MemoryLogger.Event e5 = events.get(5);
		DataIdEntry d5 = log.getDataEntries().get(e5.getDataId());
		Assert.assertEquals(EventType.NEW_OBJECT_INITIALIZED, d5.getEventType());
		Assert.assertSame(o, e5.getObjectValue());
		
		
		
		MemoryLogger.Event e6 = events.get(6);
		DataIdEntry d6 = log.getDataEntries().get(e6.getDataId());
		Assert.assertEquals(EventType.PUT_INSTANCE_FIELD, d6.getEventType());
		Assert.assertSame(o, e6.getObjectValue());
		
		MemoryLogger.Event e7 = events.get(7);
		DataIdEntry d7 = log.getDataEntries().get(e7.getDataId());
		Assert.assertEquals(EventType.PUT_INSTANCE_FIELD_VALUE, d7.getEventType());
		Assert.assertEquals(Descriptor.Integer, d7.getValueDesc());
		Assert.assertEquals(2, e7.getIntValue());

		MemoryLogger.Event e8 = events.get(8);
		DataIdEntry d8 = log.getDataEntries().get(e8.getDataId());
		Assert.assertEquals(EventType.METHOD_NORMAL_EXIT, d8.getEventType());
		Assert.assertEquals(Descriptor.Void, d8.getValueDesc());
		
//		Method method = wovenClass.getMethod("getField", new Class<?>[0]);
//		method.invoke(o, (Object[])null);
		
	}
	
	public static class WeaveClassLoader extends ClassLoader {
		
		public Class<?> createClass(String name, byte[] bytecode) {
			Class<?> c = defineClass(name, bytecode, 0, bytecode.length);
			resolveClass(c);
			return c;
		}
	}
	
}
