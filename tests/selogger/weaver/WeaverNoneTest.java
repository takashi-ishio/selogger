package selogger.weaver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import selogger.logging.Logging;
import selogger.logging.io.MemoryLogger;
import selogger.testutil.WeaveClassLoader;



public class WeaverNoneTest {

	@Test
	public void testDivideClass() throws IOException {
		// Execute a weaving 
		WeaveConfig config = new WeaveConfig(WeaveConfig.KEY_RECORD_NONE); 
		WeaveClassLoader loader = new WeaveClassLoader(config);
		Class<?> wovenClass = loader.loadAndWeaveClass("selogger.testdata.DivideClass");
		try {
			Object[] o = new Object[0];
			wovenClass.getConstructors()[0].newInstance(o);
		} catch (InvocationTargetException|IllegalAccessException|InstantiationException e) {
			Assert.fail();
		}
	}

	@Test
	public void testSimpleTarget() throws IOException {
		// Execute a weaving 
		PrintStream backup = System.out;
		try {
			MemoryLogger memoryLogger = new MemoryLogger();
			Logging.setLogger(memoryLogger);

			WeaveConfig config = new WeaveConfig(WeaveConfig.KEY_RECORD_NONE); 
			
			// Weave a class and then read the behavior
			WeaveClassLoader loader = new WeaveClassLoader(config);
			Class<?> wovenClass = loader.loadAndWeaveClass("selogger.testdata.SimpleTarget");
			Object o = null;
			String[] args = new String[0];
			Method m = wovenClass.getMethod("main", String[].class);
			ByteArrayOutputStream output1 = new ByteArrayOutputStream();
			PrintStream myStream = new PrintStream(output1);
			System.setOut(myStream);
			m.invoke(o, new Object[] { args });
			myStream.close();
			wovenClass = null;
			loader = null;

			// Load the same class without weaving
			loader = new WeaveClassLoader(config); 
			Class<?> normalClass = loader.loadClass("selogger.testdata.SimpleTarget");
			m = normalClass.getMethod("main", String[].class);
			ByteArrayOutputStream output2 = new ByteArrayOutputStream();
			PrintStream myStream2 = new PrintStream(output2);
			System.setOut(myStream2);
			m.invoke(o, new Object[] { args });
			myStream2.close();
			
			// The output should be the same
			Assert.assertArrayEquals(output2.toByteArray(), output1.toByteArray());
			
			// No events are recorded
			Assert.assertEquals(0, memoryLogger.getEvents().size());
		} catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException|ClassNotFoundException e) {
			Assert.fail();
		} finally {
			System.setOut(backup);
		}
	}
	
}
