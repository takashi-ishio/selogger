package selogger.testdata;

public class ConstructorMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		C c = new C();
		D d = new D();
		System.out.println(c.toString());
		System.out.println(d.toString());
		
		int[] x = new int[2];
		int[][] y = new int[3][2];
		System.out.println(System.identityHashCode(x.getClass()));
		System.out.println(System.identityHashCode(y.getClass()));
		System.out.println(System.identityHashCode(y[0].getClass()));
		
	}

	
	public static class C {
	
		public C() {
			System.out.println(this.getClass().getName());
		}
	}
	
	public static class D extends C {
		public D() {
			super();
			System.out.println(this.getClass().getName());
		}
	}
	
}
