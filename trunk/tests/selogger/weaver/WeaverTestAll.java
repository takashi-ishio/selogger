package selogger.weaver;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import selogger.EventType;
import selogger.logging.EventLogger;
import selogger.logging.io.MemoryLogger;


public class WeaverTestAll {

	private static class Counter {
		private int c = 0;
		public void increment() {
			c++;
		}
		
		public int count() {
			return c;
		}
	}
	
	private static class Counters {
		
		private Map<EventType, Counter> counters;
		
		public Counters() {
			counters = new HashMap<>();
		}
		
		public void increment(EventType t) {
			Counter count = counters.get(t);
			if (count == null) {
				count = new Counter();
				counters.put(t, count);
			}
			count.increment();
		}
		
		public int count(EventType t) {
			Counter c = counters.get(t);
			if (c != null) return c.count();
			else return 0;
		}
	}
	
	
	
	public Counters getEventFrequency(WeaveConfig config) throws IOException {
		WeaveLog weaveLog = new WeaveLog(0, 0, 0);
		String className = "selogger/testdata/SimpleTarget";
		ClassReader r = new ClassReader(className);
		ClassTransformer c = new ClassTransformer(weaveLog, config, r, this.getClass().getClassLoader());
		WeaveClassLoader loader = new WeaveClassLoader();
		Class<?> wovenClass = loader.createClass("selogger.testdata.SimpleTarget", c.getWeaveResult());
		MemoryLogger memoryLogger = EventLogger.initializeForTest();
		
		ClassReader r2 = new ClassReader("selogger/testdata/SimpleTarget$StringComparator");
		ClassTransformer c2 = new ClassTransformer(weaveLog, config, r2, this.getClass().getClassLoader());
		Class<?> innerClass = loader.createClass("selogger.testdata.SimpleTarget$StringComparator", c2.getWeaveResult());
		
		try {
			Counters counters = new Counters();
			Object o = wovenClass.newInstance();
			Method method = wovenClass.getMethod("testAll", new Class<?>[0]);
			method.invoke(o);
	
			EventIterator it = new EventIterator(memoryLogger, weaveLog);
			while (it.next()) {
				counters.increment(it.getEventType());
			}
			return counters;
			
		} catch (Exception e) {
			return null;
			
		} finally {
			innerClass = null;
			wovenClass = null;
			loader = null;
		}
	}
	
	private void assertSameCount(Counters c1, Counters c2, Set<EventType> types) {
		for (EventType t: EventType.values()) {
			if (types.contains(t)) {
				Assert.assertEquals(t.name(), c1.count(t), c2.count(t));
			} else {
				Assert.assertEquals(t.name(), 0, c2.count(t));
			}
		}
	}

	private HashSet<EventType> execEvents;
	private HashSet<EventType> callEvents;
	private HashSet<EventType> localEvents;
	
	@Before 
	public void setUp() {
		execEvents = new HashSet<>();
		execEvents.addAll(Arrays.asList(new EventType[] {EventType.METHOD_ENTRY, EventType.METHOD_NORMAL_EXIT, EventType.METHOD_EXCEPTIONAL_EXIT, EventType.METHOD_THROW, EventType.METHOD_OBJECT_INITIALIZED}));
		callEvents = new HashSet<>();
		callEvents.addAll(Arrays.asList(new EventType[] {EventType.CALL, EventType.CALL_RETURN, EventType.CATCH}));
		localEvents = new HashSet<>();
		localEvents.addAll(Arrays.asList(new EventType[] {EventType.LOCAL_LOAD, EventType.LOCAL_STORE, EventType.LOCAL_INCREMENT}));
	}

	@Test
	public void testMethodImplementation() throws IOException {
		Counters all = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_ALL));
		for (EventType t: EventType.values()) {
			if (t != EventType.RESERVED && 
				t != EventType.DIVIDE && 
				t != EventType.RET && 
				t != EventType.JUMP) {
				Assert.assertTrue(t.name() + " should be included in a test case", all.count(t) > 0);
			}
		}
	}

	@Test
	public void testDefaultMode() throws IOException {
		Counters all = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_ALL));
		Counters defaultEvents = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_DEFAULT));
		HashSet<EventType> events = new HashSet<>();
		for (EventType t: EventType.values()) {
			if (t != EventType.RESERVED && 
				t != EventType.DIVIDE && 
				t != EventType.RET && 
				t != EventType.JUMP && 
				t != EventType.LABEL &&
				!localEvents.contains(t)) {
				events.add(t);
			}
		}
		assertSameCount(all, defaultEvents, events);
	}

	@Test
	public void testDefaultPlusLocal() throws IOException {
		Counters all = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_ALL));
		Counters defaultEvents = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_DEFAULT_PLUS_LOCAL));
		HashSet<EventType> events = new HashSet<>();
		for (EventType t: EventType.values()) {
			if (t != EventType.RESERVED && 
				t != EventType.DIVIDE && 
				t != EventType.RET && 
				t != EventType.JUMP && 
				t != EventType.LABEL) {
				events.add(t);
			}
		}
		assertSameCount(all, defaultEvents, events);
	}

	@Test
	public void testConfigurations() throws IOException {
		
		Counters all = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_ALL));

		Counters exec = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_EXEC));
		assertSameCount(all, exec, execEvents);

		// TODO The test cases failed
//		HashSet<EventType> execCallEvents = new HashSet<>();
//		execCallEvents.addAll(execEvents);
//		execCallEvents.addAll(callEvents);
//		Counters execcall = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_EXEC + WeaveConfig.KEY_RECORD_CALL));
//		assertSameCount(all, execcall, execCallEvents);
//
//		Counters call = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_CALL));
//		assertSameCount(all, call, callEvents);

	}

	
}
