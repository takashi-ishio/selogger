package selogger.weaver;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Test;

import selogger.testutil.WeaveClassLoader;



public class WeaverNoneTest {

	@Test
	public void testLine() throws IOException {
		// Execute a weaving 
		WeaveConfig config = new WeaveConfig(WeaveConfig.KEY_RECORD_NONE); 
		WeaveClassLoader loader = new WeaveClassLoader(config);
		Class<?> wovenClass = loader.loadAndWeaveClass("selogger.testdata.DivideClass");
		try {
			Object o = null;
			wovenClass.getConstructors()[0].newInstance(o);
		} catch (InvocationTargetException|IllegalAccessException|InstantiationException e) {
			Assert.fail();
		}
	}

	
}
