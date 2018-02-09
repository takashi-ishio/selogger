package selogger.weaver;

public class WeaveClassLoader extends ClassLoader {
	
	public Class<?> createClass(String name, byte[] bytecode) {
		Class<?> c = defineClass(name, bytecode, 0, bytecode.length);
		resolveClass(c);
		return c;
	}
}
