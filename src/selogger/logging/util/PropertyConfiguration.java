package selogger.logging.util;

public class PropertyConfiguration {

	public static final String PROPERTY_BUFFER_SIZE = "selogger.buffer.size";
	public static final int MEGABYTES = 1024 * 1024;
	public static final int MAX_BUFFER_SIZE = 1024;
	public static final int MIN_BUFFER_SIZE = 1;
	public static final int DEFAULT_BUFFER_SIZE = 64;
	
	
	/**
	 * Read a buffer size
	 * @return
	 */
	public static int getBufferSize() {
		int megabytes = DEFAULT_BUFFER_SIZE;
		String sizeStr = System.getProperty(PROPERTY_BUFFER_SIZE);
		if (sizeStr != null) {
			try {
				megabytes = Integer.parseInt(sizeStr);
				megabytes = Math.min(Math.max(megabytes, MIN_BUFFER_SIZE), MAX_BUFFER_SIZE);
			} catch (NumberFormatException e) {
	    	}
		}
		return megabytes * MEGABYTES;
	}
	
}
