package selogger.weaver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.TryCatchBlockSorter;

import selogger.weaver.method.MethodTransformer;

/**
 * A class implements weaves logging code into a Java class file. 
 * Please use static transform(byte[]) or transform(inputFilename, outputFilename) method.
 *
 * @author ishio
 */
public class ClassTransformer extends ClassVisitor {

	/**
	 * A utility method to load a data from a stream.
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	public static byte[] streamToByteArray(InputStream stream) throws IOException {
		byte[] buf = new byte[4096];
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int n;
		while ((n = stream.read(buf, 0, buf.length)) > 0) {
			buffer.write(buf, 0, n);
		}
		return buffer.toByteArray();
	}

	/**
	 * @param writer
	 * @param inputClassStream specifies a class data.  It should be noted that 
	 * this method does NOT close the stream.
	 * @throws IOException
	 */
	public ClassTransformer(WeaveLog weaver, WeaveConfig config, InputStream inputClassStream, ClassLoader loader) throws IOException {
		this(weaver, config, streamToByteArray(inputClassStream), loader);
	}
	
	public ClassTransformer(WeaveLog weaver, WeaveConfig config, byte[] inputClass, ClassLoader loader) throws IOException {
		this(weaver, config, new ClassReader(inputClass), loader);
	}
	
	public ClassTransformer(WeaveLog weaver, WeaveConfig config, ClassReader reader, ClassLoader loader) {
		this(weaver, config, new MetracerClassWriter(reader, loader));
		//this(writer, new ClassWriter(ClassWriter.COMPUTE_MAXS), logLevel);
		//ClassReader cr = new ClassReader(inputClass);
        reader.accept(this, ClassReader.EXPAND_FRAMES);
        weaveResult = classWriter.toByteArray();
	}

	protected ClassTransformer(WeaveLog weavingInfo, WeaveConfig config, ClassWriter cw) {
		super(Opcodes.ASM5, cw);
		this.classWriter = cw;
		this.weavingInfo = weavingInfo;
		this.config = config;
	}
	
	private WeaveLog weavingInfo;
	private WeaveConfig config;
	private String fullClassName;
	private String className;
	private String outerClassName;
	private String packageName;
	private String sourceFileName;
	private ClassWriter classWriter;
	private byte[] weaveResult;
	
	private String PACKAGE_SEPARATOR = "/";
	
	public byte[] getWeaveResult() {
		return weaveResult;
	}
	
	public String getFullClassName() {
		return fullClassName;
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getPackageName() {
		return packageName;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		this.fullClassName = name;
		this.weavingInfo.setFullClassName(fullClassName);
		int index = name.lastIndexOf(PACKAGE_SEPARATOR);
		if (index >= 0) {
			packageName = name.substring(0, index);
			className = name.substring(index+1);
		}
		
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	@Override
	public void visitSource(String source, String debug) {
		super.visitSource(source, debug);
		sourceFileName = source;
	}
	
	@Override
	public void visitInnerClass(String name, String outerName,
			String innerName, int access) {
		super.visitInnerClass(name, outerName, innerName, access);
		if (name.equals(fullClassName)) {
			outerClassName = outerName;
		}
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        if (mv != null) {
        	mv = new TryCatchBlockSorter(mv, access, name, desc, signature, exceptions);
        	MethodTransformer trans = new MethodTransformer(weavingInfo, config, sourceFileName, fullClassName, outerClassName, access, name, desc, signature, exceptions, mv);
        	return new JSRInliner(trans, access, name, desc, signature, exceptions);
        } else {
        	return null;
        }
	}
	
}
