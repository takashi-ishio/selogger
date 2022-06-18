package selogger.weaver;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import selogger.logging.Logging;
import selogger.logging.io.MemoryLogger;
import selogger.testutil.WeaveClassLoader;

public class WeaverComparisonTest {

	/**
	 * Execute the main method of a given class
	 * @param c
	 * @return a byte array including STDOUT
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 */
	private byte[] executeMain(Class<?> c) throws NoSuchMethodException, IllegalAccessException {
		Object o = null;
		String[] args = new String[0];
		Method m = c.getMethod("main", String[].class);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintStream myStream = new PrintStream(output);
		PrintStream backup = System.out;
		System.setOut(myStream);
		try {
			m.invoke(o, new Object[] { args });
		} catch (InvocationTargetException e) {
		} finally {
			System.setOut(backup);
		}
		myStream.close();
		return output.toByteArray();
	}
	
	private MemoryLogger comparePrograms(String weaveConfig, String mainClassName) {
		WeaveConfig config = new WeaveConfig(weaveConfig); 
		MemoryLogger logger = new MemoryLogger();
		Logging.setLogger(logger);
		
		try {
			// Weave a class and then read the behavior
			WeaveClassLoader loader = new WeaveClassLoader(config);
			Class<?> wovenClass = loader.loadAndWeaveClass(mainClassName);
			byte[] wovenResult = executeMain(wovenClass);
			wovenClass = null;
			loader = null;
	
			// Load the same class without weaving
			loader = new WeaveClassLoader(config); 
			Class<?> normalClass = loader.loadClassFromResource(mainClassName, mainClassName.replace('.', '/') + ".class");
			byte[] normalResult = executeMain(normalClass);
			
			// The output should be the same
			Assert.assertArrayEquals(normalResult, wovenResult);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		return logger;
	}
	
	
	/**
	 * Test these weaving do not change the program output
	 */
	@Test
	public void testComparison() {
		comparePrograms(WeaveConfig.KEY_RECORD_ALL, "selogger.testdata.SimpleTarget");
		comparePrograms(WeaveConfig.KEY_RECORD_ALL, "selogger.testdata.ArrayReadWriteMain");
		comparePrograms(WeaveConfig.KEY_RECORD_ALL, "selogger.testdata.MultiNewArrayMain");
		comparePrograms(WeaveConfig.KEY_RECORD_ALL, "selogger.testdata.TestMain");

		MemoryLogger logger = comparePrograms(WeaveConfig.KEY_RECORD_NONE, "selogger.testdata.SimpleTarget");
		Assert.assertEquals(0, logger.getEvents().size());
		logger = comparePrograms(WeaveConfig.KEY_RECORD_NONE, "selogger.testdata.ArrayReadWriteMain");
		Assert.assertEquals(0, logger.getEvents().size());
		logger = comparePrograms(WeaveConfig.KEY_RECORD_NONE, "selogger.testdata.MultiNewArrayMain");
		Assert.assertEquals(0, logger.getEvents().size());
		logger = comparePrograms(WeaveConfig.KEY_RECORD_NONE, "selogger.testdata.TestMain");
		Assert.assertEquals(0, logger.getEvents().size());
	}
	
}
