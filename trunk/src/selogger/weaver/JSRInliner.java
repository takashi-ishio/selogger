package selogger.weaver;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 * This class enables a wrapped visitor to analyze bytecode instructions after JSR inlining. 
 * @author ishio
 */
public class JSRInliner extends JSRInlinerAdapter {

	private MethodTransformer analysis;
	
	/**
	 * @param mv specifies a wrapped visitor that analyzes inlined bytecode instructions. 
	 */
	public JSRInliner(MethodTransformer mv, int access, String name, String desc, String signature, String[] exceptions) {
		super(Opcodes.ASM5, null, access, name, desc, signature, exceptions);
		this.analysis = mv;
	}
	
	@Override
	public void visitEnd() {
		// Inline JSR instructions 
		super.visitEnd();
		
		// Provide the resultant instruction list for creating a list of labels in the method 
		analysis.makeLabelList(instructions); 
		
		// Analyze the inlined method
		super.accept(analysis);
	}	
	
}
