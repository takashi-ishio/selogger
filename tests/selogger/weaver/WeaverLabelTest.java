package selogger.weaver;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import selogger.EventType;
import selogger.logging.Logging;
import selogger.logging.io.MemoryLogger;
import selogger.testutil.WeaveClassLoader;


/**
 * This class tests the events recorded by the 
 * default + local variable access configuration.
 * (WeaverTestAll checks only the frequency of events) 
 */
public class WeaverLabelTest {

	private Class<?> wovenClass;
	private MemoryLogger memoryLogger;
	private EventIterator it;
	
	@Before
	public void setup() throws IOException {
		WeaveConfig config = new WeaveConfig(WeaveConfig.KEY_RECORD_LABEL); 
		WeaveClassLoader loader = new WeaveClassLoader(config);
		wovenClass = loader.loadAndWeaveClass("selogger.testdata.SimpleTarget");
	
		memoryLogger = new MemoryLogger();
		Logging.setLogger(memoryLogger);
		it = new EventIterator(memoryLogger, loader.getWeaveLog());
	}
	
	@After
	public void tearDown() {
		wovenClass = null;
	}

	private void testBaseEvents(EventIterator it, Object instance) {
		// Labels in <clinit> 
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals("<clinit>", it.getMethodName());

		// Labels in <init>
		// call Object.<init>
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals("<init>", it.getMethodName());

		// Initialize a field
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals("<init>", it.getMethodName());

		// Return to the caller
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals("<init>", it.getMethodName());

	}
	
	@Test
	public void testCatchLabel() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);
		
		// Execute a method that causes an exception
		Method method = wovenClass.getMethod("exception", new Class<?>[0]);
		Throwable result = null;
		try {
			method.invoke(o);
		} catch (InvocationTargetException e) {
			result = e.getCause();
		}
		Assert.assertNotNull(result);
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals("exception", it.getMethodName());
		int firstLine = it.getLine();

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals("exception", it.getMethodName());
		Assert.assertEquals(firstLine + 1, it.getLine());
		int index = it.getInstructionIndex();

		// The code executes 4 instructions: line numbre, ALOAD, ICONST_0, and BALOAD
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH_LABEL, it.getEventType());
		Assert.assertEquals(index+4, it.getIntValue());

		// Final label in the catch block
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());

		Assert.assertFalse(it.next());
	}

	@Test
	public void testNestedIfConditions() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		/* boolean nestedConditions(int x, int y)
		 * 0: (L00000)
		 * 1: (line=185)
		 * 2: ILOAD 1 (x)
		 * 3: IFLE L00013
		 * 4: ILOAD 2 (y)
		 * 5: IFLE L00013
		 * 6: ILOAD 1 (x)
		 * 7: ILOAD 2 (y)
		 * 8: IF_ICMPNE L00013
		 */
		// Event generation
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method m = wovenClass.getMethod("nestedConditions", new Class<?>[]{int.class, int.class});

		// x == y == 1 reaches instruction 9
		Object ret = m.invoke(o, new Object[] {Integer.valueOf(1), Integer.valueOf(1)});
		Assert.assertTrue(((Boolean)ret).booleanValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals(0, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals(8, it.getIntValue());
		Assert.assertEquals(9, it.getInstructionIndex());
		
		Assert.assertFalse(it.next());
	}
	
	@Test
	public void testSecondIfCondition() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method m = wovenClass.getMethod("nestedConditions", new Class<?>[]{int.class, int.class});

		Object ret = m.invoke(o, new Object[] {Integer.valueOf(1), Integer.valueOf(0)});
		Assert.assertFalse(((Boolean)ret).booleanValue());
		
		// x == 1, y == 0 go to Instruction 13 from the second IF instruction (instruction 5)
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals(0, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals(5, it.getIntValue());
		Assert.assertEquals(13, it.getInstructionIndex());
		
		Assert.assertFalse(it.next());
	}
	
	@Test
	public void testFirstIfCondition() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method m = wovenClass.getMethod("nestedConditions", new Class<?>[]{int.class, int.class});

		// x == y == 0 goes to Instruction 13 from the first IF instruction (instruction 3)
		Object ret = m.invoke(o, new Object[] {Integer.valueOf(0), Integer.valueOf(0)});
		Assert.assertFalse(((Boolean)ret).booleanValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals(0, it.getIntValue());

		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals(3, it.getIntValue());
		Assert.assertEquals(13, it.getInstructionIndex());
		
		Assert.assertFalse(it.next());
	}

	@Test
	public void testDivideByZero() throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		Object o = wovenClass.getDeclaredConstructor().newInstance();
		
		// Check events
		testBaseEvents(it, o);

		// Execute a method
		Method m = wovenClass.getMethod("divide", new Class<?>[]{int.class});

		// x == y == 0 goes to Instruction 13 from the first IF instruction (instruction 3)
		Object ret = m.invoke(o, new Object[] {Integer.valueOf(0)});
		Assert.assertEquals(0, ((Integer)ret).intValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());
		Assert.assertEquals(0, it.getIntValue());

		// The instruction causes ArithmeticException: LABEL, LDC 1, ILOAD, IDIV
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.CATCH_LABEL, it.getEventType());
		Assert.assertEquals(4, it.getIntValue());
		
		Assert.assertTrue(it.next());
		Assert.assertEquals(EventType.LABEL, it.getEventType());

		Assert.assertFalse(it.next());
	}

	
	/**
	 * This method confirms that "ALL" records the same LABEL events as "LABEL" configuration.
	 */
	@Test
	public void testLabelUnchanged()  throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		WeaveConfig config = new WeaveConfig(WeaveConfig.KEY_RECORD_ALL); 
		WeaveClassLoader loader = new WeaveClassLoader(config);
		Class<?> allWovenClass = loader.loadAndWeaveClass("selogger.testdata.SimpleTarget");
	
		try {
			memoryLogger = new MemoryLogger();
			Logging.setLogger(memoryLogger);
			it = new EventIterator(memoryLogger, loader.getWeaveLog());

			Object o = allWovenClass.getDeclaredConstructor().newInstance();
			Method m = allWovenClass.getMethod("nestedConditions", new Class<?>[]{int.class, int.class});

			// x == y == 1 reaches instruction 9
			Object ret = m.invoke(o, new Object[] {Integer.valueOf(1), Integer.valueOf(1)});
			Assert.assertTrue(((Boolean)ret).booleanValue());

			// calls the exception method
			Method method = allWovenClass.getMethod("exception", new Class<?>[0]);
			Throwable result = null;
			try {
				method.invoke(o);
			} catch (InvocationTargetException e) {
				result = e.getCause();
			}
			Assert.assertNotNull(result);

			List<EventType> labels = Arrays.asList(EventType.LABEL, EventType.CATCH_LABEL);
			Assert.assertTrue(it.nextSpecifiedEvent(labels));
			Assert.assertEquals(EventType.LABEL, it.getEventType());
			Assert.assertEquals("<clinit>", it.getMethodName());

			Assert.assertTrue(it.nextSpecifiedEvent(labels));
			Assert.assertEquals(EventType.LABEL, it.getEventType());
			Assert.assertEquals("<init>", it.getMethodName());

			Assert.assertTrue(it.nextSpecifiedEvent(labels));
			Assert.assertEquals(EventType.LABEL, it.getEventType());
			Assert.assertEquals("<init>", it.getMethodName());

			Assert.assertTrue(it.nextSpecifiedEvent(labels));
			Assert.assertEquals(EventType.LABEL, it.getEventType());
			Assert.assertEquals("<init>", it.getMethodName());

			Assert.assertTrue(it.nextSpecifiedEvent(labels));
			Assert.assertEquals(EventType.LABEL, it.getEventType());
			Assert.assertEquals("nestedConditions", it.getMethodName());
			Assert.assertEquals(0, it.getIntValue());

			Assert.assertTrue(it.nextSpecifiedEvent(labels));
			Assert.assertEquals(EventType.LABEL, it.getEventType());
			Assert.assertEquals("nestedConditions", it.getMethodName());
			Assert.assertEquals(8, it.getIntValue());
			Assert.assertEquals(9, it.getInstructionIndex());

			Assert.assertTrue(it.nextSpecifiedEvent(labels));
			Assert.assertEquals(EventType.LABEL, it.getEventType());
			Assert.assertEquals("exception", it.getMethodName());
			int firstLine = it.getLine();

			Assert.assertTrue(it.nextSpecifiedEvent(labels));
			Assert.assertEquals(EventType.LABEL, it.getEventType());
			Assert.assertEquals("exception", it.getMethodName());
			Assert.assertEquals(firstLine + 1, it.getLine());
			int index = it.getInstructionIndex();

			Assert.assertTrue(it.nextSpecifiedEvent(labels));
			Assert.assertEquals(EventType.CATCH_LABEL, it.getEventType());
			Assert.assertEquals(index+4, it.getIntValue());

			Assert.assertTrue(it.nextSpecifiedEvent(labels));
			Assert.assertEquals(EventType.LABEL, it.getEventType());

			Assert.assertFalse(it.nextSpecifiedEvent(labels));

		} finally {
			allWovenClass  = null;
		}
	}
}
