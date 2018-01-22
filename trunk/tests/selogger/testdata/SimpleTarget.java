package selogger.testdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntUnaryOperator;

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
	private int FIELD = 2;
	
	public SimpleTarget() {
	}
	
	public int getField() {
		return FIELD;
	}
	
	public short[] createArray(int size) {
		short[] array = new short[size];
		for (int i=0; i<array.length; i++) {
			array[i] = (short)i;
		}
		array[0]++;
		array[1] = 2;
		return array;
	}
	
	public boolean exception() {
		try {
			boolean[] array = new boolean[0];
			return array[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw e;
		}
	}
	
	public double synchronization() {
		synchronized (this) {
			return Math.max(1.0, 2.0);
		}
	}

	public int read() {
		return INDEX;
	}

	public int[][][] multiarray(byte b, char c) {
		return new int[b][c][1];
	}
	
	public String constString() {
		return "TEST";
	}
	
	public boolean typeCheck(Object o) {
		if (o instanceof String) {
			return true;
		} else {
			return false;
		}
	}
	
	public class StringComparator implements Comparator<String> {
		@Override
		public int compare(String o1, String o2) {
			return -o1.compareTo(o2);
		}
	}
	
	public void sort(ArrayList<String> items) {
		Collections.sort(items, new StringComparator());
	}
	
	public long invokeVirtual() {
		return getLong(getField());
	}

	public long getLong(long t) {
		return t + 1;
	}

	public int invokeDynamic() {
		IntUnaryOperator f = i -> i * 2; // the body is defined as a synthetic static method
		return f.applyAsInt(1); // Invoke a wrapper instance (f); the f executes the synthetic method.  
	}

	public int invokeDynamic2() {
		IntUnaryOperator f = i -> i * FIELD; // the body is defined as a synthetic instance method
		return f.applyAsInt(1); // Invoke a wrapper instance (f); the f executes the synthetic method.  
	}

	public String invokeInterface() {
		List<String> list = new ArrayList<>();
		return list.get(0);
	}

	public float getFloat() {
		return 1.0F;
	}
	
	public void exceptionInCall() {
		exception();
	}

}
