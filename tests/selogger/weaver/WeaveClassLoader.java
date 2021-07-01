package selogger.weaver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * A class loader for testing a woven class
 */
public class WeaveClassLoader extends ClassLoader {
	
	/**
	 * Read a given class as a Java class 
	 * @param name specifies a class name
	 * @param bytecode is the bytecode of the class
	 * @return a Class object to create an instance of the Java class.
	 * This method returns null if the bytecode is null 
	 */
	public Class<?> createClass(String name, byte[] bytecode) {
		if (bytecode != null) {
			Class<?> c = defineClass(name, bytecode, 0, bytecode.length);
			resolveClass(c);
			return c;
		} else {
			return null;
		}
	}
	
	/**
	 * Read a class from system resource.  
	 * This is separated from regular class loading so that the class 
	 * can be discarded after the test
	 * @param name specifies a class resource
	 * @return a Class object.  null is returned if this method could not load the resource 
	 */
	public Class<?> loadClassFromResource(String className, String resourceName) {
		byte[] buf = readAllBytesOfClass(ClassLoader.getSystemResourceAsStream(resourceName));
		return createClass(className, buf);
	}
	
	/**
	 * Read bytecode from a given input stream
	 * @param is specifies a class file resource
	 * @return the byte array
	 */
	public static byte[] readAllBytesOfClass(InputStream is) {
		try {
			// Copy the content to ByteArrayOutputStream 
			ByteArrayOutputStream array = new ByteArrayOutputStream(); 
			byte[] buf = new byte[4096]; 
			int read;
			while ((read = is.read(buf, 0, buf.length)) > 0) {
				array.write(buf, 0, read);
			}
			is.close();
			// Generate a byte array
			return array.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

}
