package selogger.weaver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.TryCatchBlockSorter;

import selogger.logging.util.TypeIdUtil;
import selogger.weaver.method.JSRInliner;
import selogger.weaver.method.MethodTransformer;

/**
 * This class weaves logging code into a Java class file. 
 * The constructors execute the weaving process.
 */
public class ClassTransformer extends ClassVisitor {

	/**
	 * This constructor weaves the given class and provides the result. 
	 * @param weaver specifies the state of the weaver.
	 * @param config specifies the configuration.
	 * @param inputClass specifies a byte array containing the target class.
	 * @param loader specifies a class loader that loaded the target class.
	 * @throws IOException may be thrown if an error occurs during the weaving.
	 */
	public ClassTransformer(WeaveLog weaver, WeaveConfig config, byte[] inputClass, ClassLoader loader) throws IOException {
		this(weaver, config, new ClassReader(inputClass), loader);
	}
	
	/**
	 * This constructor weaves the given class and provides the result. 
	 * @param weaver specifies the state of the weaver.
	 * @param config specifies the configuration.
	 * @param reader specifies a class reader to read the target class.
	 * @param loader specifies a class loader that loaded the target class.
	 */
	public ClassTransformer(WeaveLog weaver, WeaveConfig config, ClassReader reader, ClassLoader loader) {
		// Create a writer for the target class
		this(weaver, config, new MetracerClassWriter(reader, loader));
		// Start weaving, and store the result to a byte array
        reader.accept(this, ClassReader.EXPAND_FRAMES);
        weaveResult = classWriter.toByteArray();
        classLoaderIdentifier = TypeIdUtil.getClassLoaderIdentifier(loader, weaver.getFullClassName());
	}

	/**
	 * Initializes the object as a ClassVisitor.
	 * @param weaver specifies the state of the weaver.
	 * @param config specifies the configuration.
	 * @param cw specifies the class writer (MetracerClassWriter).
	 */
	protected ClassTransformer(WeaveLog weaver, WeaveConfig config, ClassWriter cw) {
		super(Opcodes.ASM9, cw);
		this.weavingInfo = weaver;
		this.config = config;
		this.classWriter = cw;
		this.annotations = new ArrayList<String>();
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
	private String classLoaderIdentifier;
	private ArrayList<String> annotations;
	
	private String PACKAGE_SEPARATOR = "/";
	
	/**
	 * @return the weaving result.
	 */
	public byte[] getWeaveResult() {
		return weaveResult;
	}
	
	/**
	 * @return the full class name including the package name and class name
	 */
	public String getFullClassName() {
		return fullClassName;
	}
	
	/**
	 * @return the class name without the package name 
	 */
	public String getClassName() {
		return className;
	}
	
	/**
	 * @return the package name
	 */
	public String getPackageName() {
		return packageName;
	}
	
	/**
	 * @return the class loader identifier
	 */
	public String getClassLoaderIdentifier() {
		return classLoaderIdentifier;
	}
	
	/**
	 * @return the annotation of the class.
	 */
	public List<String> getAnnotation() {
		return annotations;
	}
	
	/**
	 * A call back from the ClassVisitor.  
	 * Record the class information to fields.
	 */
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
	
	/**
	 * A call back from the ClassVisitor.
	 * Record the source file name.
	 */
	@Override
	public void visitSource(String source, String debug) {
		super.visitSource(source, debug);
		sourceFileName = source;
	}
	
	/**
	 * A call back from the ClassVisitor.
	 * Record the outer class name if this class is an inner class.
	 */
	@Override
	public void visitInnerClass(String name, String outerName,
			String innerName, int access) {
		super.visitInnerClass(name, outerName, innerName, access);
		if (name.equals(fullClassName)) {
			outerClassName = outerName;
		}
	}
	
	/**
	 * A call back from the ClassVisitor.
	 * Create an instance of a MethodVisitor that inserts logging code into a method.
	 */
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

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		this.annotations.add(descriptor);
		return super.visitAnnotation(descriptor, visible);
	}
}
