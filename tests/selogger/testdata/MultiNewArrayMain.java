package selogger.testdata;

public class MultiNewArrayMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		dumpArray(new int[10]);
		dumpArray(new int[10][5]);
		dumpArray(new String[4]);
		dumpArray(new Object[2][3]);
		dumpArray(new String[4][3][2]);
		dumpArray(new int[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
	}
	
	public static void dumpArray(Object multiArray) {
		if (multiArray == null) {
			System.out.println("Element: null");
			return;
		}
		if (multiArray instanceof Object[]) {
			System.out.println("Object-Array: " + multiArray.getClass().getCanonicalName());
			Object[] asArray = (Object[])multiArray;
			for (Object o: asArray) {
				dumpArray(o);
			}
		} else {
			System.out.println("Element: " + multiArray.getClass().getCanonicalName());
		}
	}
}
