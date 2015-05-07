package selogger.testdata;

public class SimpleTarget {

	public static void main(String[] args) {
		SimpleTarget instance = new SimpleTarget();
		int field = instance.getField();
		int[] values = new int[] {100, 200, 300};
		int v1 = values[field];
		int v2 = values[INDEX];
		INDEX = 2;
		values[INDEX] = v1 + v2;
		System.out.println(values[0]);
		System.out.println(values[1]);
		System.out.println(values[2]);
		System.out.println(values.length);
	}
	
	private static int INDEX = 1;
	private int FIELD = 0;
	
	public SimpleTarget() {
	}
	
	public int getField() {
		return FIELD;
	}
}
