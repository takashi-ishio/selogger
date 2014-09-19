package selogger.weaver;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 * This class enable a wrapped visitor to analyze bytecode instructions after JSR inlining.    
 * @author ishio
 */
public class JSRInliner extends JSRInlinerAdapter {

	private MethodVisitor analysis;
	
	/**
	 * @param mv specifies a wrapped visitor that analyzes inlined bytecode instructions. 
	 */
	public JSRInliner(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions) {
		super(Opcodes.ASM5, null, access, name, desc, signature, exceptions);
		this.analysis = mv;
	}
	
	@Override
	public void visitEnd() {
		// Inline JSR instructions 
		super.visitEnd();
		
		// Analyze the inlined method
		super.accept(analysis);
	}	
	
}
