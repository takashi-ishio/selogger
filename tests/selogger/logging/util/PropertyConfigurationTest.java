package selogger.logging.util;

import org.junit.Assert;
import org.junit.Test;

public class PropertyConfigurationTest {

	@Test
	public void testGetBufferSizeDefault() {
		System.clearProperty("selogger.buffer.size");
    	Assert.assertEquals("Default value should be used if the property is empty", PropertyConfiguration.DEFAULT_BUFFER_SIZE * PropertyConfiguration.MEGABYTES, PropertyConfiguration.getBufferSize());
	}
	
	@Test
	public void testGetBufferSizeConfig() {
		System.setProperty("selogger.buffer.size", "10000");
		Assert.assertEquals("Too large value should be replaced with the maximum value", PropertyConfiguration.MAX_BUFFER_SIZE * PropertyConfiguration.MEGABYTES, PropertyConfiguration.getBufferSize());
		
		System.setProperty("selogger.buffer.size", "16");
		Assert.assertEquals("Property should be used if defined ", 16 * PropertyConfiguration.MEGABYTES, PropertyConfiguration.getBufferSize());

		System.setProperty("selogger.buffer.size", "0");
		Assert.assertEquals("Too small value should be replaced with the minimum value", PropertyConfiguration.MIN_BUFFER_SIZE * PropertyConfiguration.MEGABYTES, PropertyConfiguration.getBufferSize());
		
	}
}
