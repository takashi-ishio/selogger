package selogger.testdata;

/**
 * A test data class including methods manipulated by the weaver. 
 * The main method of this class works correctly if class paths 
 * include only test-classes.  This code throws an exception if 
 * the class path includes SELogger classes because a Jackson 
 * class without shading (i.e., a logging target) is used for 
 * logging.
 */
public class TestMain {

	public static void main(String[] args) {
		TestMain obj = new TestMain();
		obj.print();
	}

	private String f;
	private int g;
	private Integer h;

	public TestMain() {
		f = "0";
		g = 1;
		h = Integer.valueOf(2);
	}

	public void print() {
		System.out.println(getF());
		System.out.println(getG());
		try {
			int k = 0;
			int zeroDivide = 1 * 2/ k;
			System.out.println(zeroDivide);
		} catch (ArithmeticException e) {
			System.out.println("2");
		}
		System.out.println(getH(0));
	}

	public String getF() {
		return f;
	}
	
	public int getG() {
		return g;
	}

	public Integer getH(int i) {
		h = 1/ (2 + 1 - 3);
		return h;
	}
	
}
