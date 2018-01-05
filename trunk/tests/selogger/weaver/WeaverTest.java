package selogger.weaver;

import java.io.IOException;

import org.junit.Test;
import org.objectweb.asm.ClassReader;


public class WeaverTest {

	@Test
	public void testWeaving() throws IOException {
		String className = "selogger/testdata/SimpleTarget";
		ClassReader r = new ClassReader(className);
		WeaveLog log = new WeaveLog(0, 0, 0);
		WeaverConfig config = new WeaverConfig(WeaverConfig.KEY_RECORD_DEFAULT); 
		ClassTransformer c = new ClassTransformer(log, config, r, this.getClass().getClassLoader());
		new WeaveClassLoader().createClass("selogger.testdata.SimpleTarget", c.getWeaveResult());
	}
	
	public static class WeaveClassLoader extends ClassLoader {
		
		public Class<?> createClass(String name, byte[] bytecode) {
			Class<?> c = defineClass(name, bytecode, 0, bytecode.length);
			resolveClass(c);
			return c;
		}
	}
	
}
