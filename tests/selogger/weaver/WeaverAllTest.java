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

import selogger.EventType;
import selogger.logging.Logging;
import selogger.logging.io.MemoryLogger;

/**
 * This test class tries different configurations of weaving
 * and compare the frequency of events.
 * (The same number of events must be observed)
 */
public class WeaverAllTest {

	/**
	 * Counting the number of events
	 */
	private static class Counter {
		private int c = 0;
		public void increment() {
			c++;
		}
		
		public int count() {
			return c;
		}
	}
	
	/**
	 * Count the number of events for each EventType
	 */
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
	

	/**
	 * Execute a weaving for a given configuration,
	 * execute the "testAll" method,
	 * and then return the observed event frequency. 
	 * @param config specifies a weaving configuration
	 * @return the observed events
	 * @throws IOException
	 */
	public Counters getEventFrequency(WeaveConfig config) throws IOException {
		// Weave the classes
		MemoryLogger memoryLogger = Logging.initializeForTest();
		WeaveClassLoader loader = new WeaveClassLoader(config);
		Class<?> wovenClass = loader.loadAndWeaveClass("selogger.testdata.SimpleTarget");
		loader.loadAndWeaveClass("selogger.testdata.SimpleTarget$StringComparator");
		
		try {
			// Execute the testAll method
			Object o = wovenClass.getConstructor().newInstance();
			Method method = wovenClass.getMethod("testAll", new Class<?>[0]);
			method.invoke(o);
	
			// Count the events
			Counters counters = new Counters();
			EventIterator it = new EventIterator(memoryLogger, loader.getWeaveLog());
			while (it.next()) {
				counters.increment(it.getEventType());
			}
			return counters;
			
		} catch (Exception e) {
			return null;
			
		} finally {
			// unload classes
			wovenClass = null;
			loader = null;
		}
	}
	
	/**
	 * Check that two Counters have the same count for given event types  
	 */
	private void assertSameCount(Counters c1, Counters c2, Set<EventType> types) {
		for (EventType t: EventType.values()) {
			if (types.contains(t)) {
				Assert.assertEquals(t.name(), c1.count(t), c2.count(t));
			} else {
				Assert.assertEquals(t.name(), 0, c2.count(t));
			}
		}
	}

	/**
	 * Events recorded when "EXEC" option is given
	 */
	private HashSet<EventType> execEvents;
	private HashSet<EventType> callEvents;
	private HashSet<EventType> execParamEvents;
	private HashSet<EventType> callParamEvents;
	private HashSet<EventType> localEvents;
	private HashSet<EventType> fieldEvents;
	private HashSet<EventType> arrayEvents;
	private HashSet<EventType> syncEvents;
	private HashSet<EventType> objectEvents;
	private HashSet<EventType> labelEvents;
	private HashSet<EventType> lineEvents;
	
	/**
	 * Prepare events object 
	 */
	@Before 
	public void setUp() {
		execEvents = new HashSet<>(Arrays.asList(new EventType[] {EventType.METHOD_ENTRY, EventType.METHOD_NORMAL_EXIT, EventType.METHOD_EXCEPTIONAL_EXIT, EventType.METHOD_THROW, EventType.METHOD_OBJECT_INITIALIZED}));
		execParamEvents = new HashSet<>(execEvents);
		execParamEvents.addAll(Arrays.asList(new EventType[] {EventType.METHOD_PARAM}));
		callEvents = new HashSet<>(Arrays.asList(new EventType[] {EventType.CALL, EventType.CALL_RETURN, EventType.CATCH, EventType.CATCH_LABEL, EventType.INVOKE_DYNAMIC, EventType.INVOKE_DYNAMIC_RESULT}));
		callParamEvents = new HashSet<>(callEvents);
		callParamEvents.addAll(Arrays.asList(new EventType[] {EventType.NEW_OBJECT, EventType.NEW_OBJECT_CREATED, EventType.CALL_PARAM, EventType.INVOKE_DYNAMIC_PARAM}));
		localEvents = new HashSet<>(Arrays.asList(new EventType[] {EventType.LOCAL_LOAD, EventType.LOCAL_STORE, EventType.LOCAL_INCREMENT}));
		fieldEvents = new HashSet<>(Arrays.asList(new EventType[] {EventType.GET_INSTANCE_FIELD, EventType.GET_INSTANCE_FIELD_RESULT, EventType.GET_STATIC_FIELD, EventType.PUT_INSTANCE_FIELD, EventType.PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION, EventType.PUT_INSTANCE_FIELD_VALUE, EventType.PUT_STATIC_FIELD, EventType.CATCH, EventType.CATCH_LABEL}));
		arrayEvents = new HashSet<>(Arrays.asList(new EventType[] {EventType.ARRAY_LENGTH, EventType.ARRAY_LENGTH_RESULT, EventType.ARRAY_LOAD, EventType.ARRAY_LOAD_INDEX, EventType.ARRAY_LOAD_RESULT, EventType.ARRAY_STORE, EventType.ARRAY_STORE_INDEX, EventType.ARRAY_STORE_VALUE, EventType.MULTI_NEW_ARRAY, EventType.MULTI_NEW_ARRAY_ELEMENT, EventType.MULTI_NEW_ARRAY_OWNER, EventType.NEW_ARRAY, EventType.NEW_ARRAY_RESULT, EventType.CATCH, EventType.CATCH_LABEL}));
		syncEvents = new HashSet<>(Arrays.asList(new EventType[] {EventType.MONITOR_ENTER, EventType.MONITOR_ENTER_RESULT, EventType.MONITOR_EXIT, EventType.CATCH, EventType.CATCH_LABEL}));
		objectEvents = new HashSet<>(Arrays.asList(new EventType[] {EventType.OBJECT_CONSTANT_LOAD, EventType.OBJECT_INSTANCEOF, EventType.OBJECT_INSTANCEOF_RESULT}));
		labelEvents = new HashSet<>(Arrays.asList(new EventType[] {EventType.LABEL, EventType.CATCH, EventType.CATCH_LABEL}));
		lineEvents = new HashSet<>(Arrays.asList(new EventType[] {EventType.LINE_NUMBER}));
	}

	/**
	 * Check that all event types are recorded in a trace 
	 */
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

	/**
	 * Check that default mode records the same number of events as ALL mode except for some event types 
	 */
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
				t != EventType.LINE_NUMBER &&
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
				t != EventType.LABEL &&
				t != EventType.LINE_NUMBER) {
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

		Counters call = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_CALL));
		assertSameCount(all, call, callEvents);

		HashSet<EventType> execCallEvents = new HashSet<>();
		execCallEvents.addAll(execEvents);
		execCallEvents.addAll(callEvents);
		Counters execcall = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_EXEC + WeaveConfig.KEY_RECORD_CALL));
		assertSameCount(all, execcall, execCallEvents);

		Counters execParam = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_EXEC + WeaveConfig.KEY_RECORD_PARAMETERS));
		assertSameCount(all, execParam, execParamEvents);

		Counters callParam = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_CALL + WeaveConfig.KEY_RECORD_PARAMETERS));
		assertSameCount(all, callParam, callParamEvents);

		Counters execCallParam = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_EXEC + WeaveConfig.KEY_RECORD_CALL + WeaveConfig.KEY_RECORD_PARAMETERS));
		execCallEvents.addAll(execParamEvents);
		execCallEvents.addAll(callParamEvents);
		assertSameCount(all, execCallParam, execCallEvents);
	}


	@Test
	public void testFieldArrayConfigurations() throws IOException {
		Counters all = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_ALL));

		Counters field = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_FIELD));
		assertSameCount(all, field, fieldEvents);

		Counters array = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_ARRAY));
		assertSameCount(all, array, arrayEvents);
	}
	
	@Test
	public void testLocalConfigurations() throws IOException {
		Counters all = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_ALL));
		Counters local = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_LOCAL));
		assertSameCount(all, local, localEvents);
	}

	@Test
	public void testLabelConfigurations() throws IOException {
		Counters all = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_ALL));
		Counters label = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_LABEL));
		assertSameCount(all, label, labelEvents);
	}

	@Test
	public void testObjectConfigurations() throws IOException {
		Counters all = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_ALL));
		Counters object = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_OBJECT));
		assertSameCount(all, object, objectEvents);
	}

	@Test
	public void testSyncConfigurations() throws IOException {
		Counters all = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_ALL));
		Counters sync = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_SYNC));
		assertSameCount(all, sync, syncEvents);
	}

	@Test
	public void testLineConfigurations() throws IOException {
		Counters all = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_ALL));
		Counters line = getEventFrequency(new WeaveConfig(WeaveConfig.KEY_RECORD_LINE));
		Assert.assertTrue(line.count(EventType.LINE_NUMBER) > 0);
		assertSameCount(all, line, lineEvents);
	}

	@Test
	public void testNoneEvents() throws IOException {
		WeaveConfig w = new WeaveConfig(WeaveConfig.KEY_RECORD_ALL);
		Counters all = getEventFrequency(w);
		Counters exec = getEventFrequency(new WeaveConfig(w, LogLevel.OnlyEntryExit));
		assertSameCount(all, exec, execEvents);

		WeaveConfig w2 = new WeaveConfig(WeaveConfig.KEY_RECORD_CALL + WeaveConfig.KEY_RECORD_PARAMETERS);
		Counters none = getEventFrequency(new WeaveConfig(w2, LogLevel.OnlyEntryExit));
		assertSameCount(all, none, new HashSet<>());
		
		WeaveConfig noneConfig = new WeaveConfig(WeaveConfig.KEY_RECORD_NONE);
		none = getEventFrequency(noneConfig);
		Assert.assertTrue(noneConfig.isValid());
		assertSameCount(all, none, new HashSet<>());
	}

	
}
