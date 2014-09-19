package selogger.testdata;

public class Target {
 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Target();

		Target t = new Target();
		String s = new String("Test Target program");
		System.out.println(s);
		String l = new String(new String("t" + "est"));
		System.out.println(l);
		
		
		int[] a = new int[100];
		a[0] = 0;
		a[1] = 1;
		for (int i=2; i<a.length; ++i) {
			a[i] = a[i-1] + a[i-2];
		}
		System.out.println(a[99]);
		
		// to check IfNull
		if (a[99] > 100) a = null;
		if (a != null) {
			System.out.println("TEST");
		}
	}
	
	public void testByteChar() {
		char c = 100;
		byte b = 121;
		short s = 1000;
		StringBuilder buf = new StringBuilder();
		buf.append(c);
		buf.append(b);
		buf.append(s);
		System.err.println(buf.toString());
	}
	
	public Target() { 
		this(new String(s));
	}

	public Target(String msg) { 
		try {
			System.out.println(msg);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
	
	private static String s;
	static {
		s = "x "  + "y";
	}
}
