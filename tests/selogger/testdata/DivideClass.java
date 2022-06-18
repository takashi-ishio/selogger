package selogger.testdata;

/**
 * This simple class caused a VerifyError (Issue #8)
 */
public class DivideClass {

	private int x;
	
	@SuppressWarnings("unused")
	public void c(int row, boolean[] list) {
		int r = row / 2;
		if (x != 0)
			list[0] = true;
	}

}
