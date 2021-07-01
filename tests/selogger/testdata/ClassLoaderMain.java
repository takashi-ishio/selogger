package selogger.testdata;

import java.net.URL;

import selogger.weaver.WeaveClassLoader;

/**
 * Test case for #1.
 * Executing the main method with selogger resulted in a broken ObjectTypes file. 
 */
public class ClassLoaderMain {
	
	public static class CustomizedCLassLoader extends ClassLoader {
		
		/**
		 * Create a class C whose getClassLoader() returns this custom class loader instance. 
		 */
		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if (name.startsWith("java.")) return super.loadClass(name);
			byte[] buf = WeaveClassLoader.readAllBytesOfClass(ClassLoader.getSystemResourceAsStream(name + ".class"));
			if (buf != null) {
				System.out.println(name);
				Class<?> c = defineClass(name, buf, 0, buf.length);
				return c;
			} else {
				throw new ClassNotFoundException(name);
			}
		}
		
		/**
		 * Override a method used by TypeIdMap.
		 * The class loads a class and make a new instance.
		 * Consequently, selogger has to generate a new type ID and 
		 * a new object ID for the instance.
		 */
		@Override
		public URL getResource(String name) {
			B b = new B();
			b.getClass();
			if (name.startsWith("java.")) return super.getResource(name);
			else return null;
		}
		
	}

	public static class A {
		public A() {
			
		}
	}
	
	public static class B {
		
	}
	
	public static void main(String[] args) {
		CustomizedCLassLoader cl = new CustomizedCLassLoader();
		try {
			Class<?> c = cl.loadClass("selogger.testdata.ClassLoaderMain$A");
			Object o = c.getDeclaredConstructor().newInstance();
			System.out.println(o.toString());
		} catch (Throwable e) {
			e.printStackTrace();
		}

	}
	
}
