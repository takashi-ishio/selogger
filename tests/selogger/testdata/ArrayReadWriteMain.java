package selogger.testdata;

/**
 * A test data class including methods manipulated by the weaver. 
 */
public class ArrayReadWriteMain {

	private static char[] c = new char[10];
	private static short[] s = new short[10];
	private static boolean[] z = new boolean[10];
	private static byte[] b = new byte[10];
	
	public static void main(String[] args) {
		for (int i=0; i<5; ++i) {
			c[i] = 'A';
			s[i] = (short)(i * i);
			b[i] = (byte)(i + i);
			z[i] = i == 4;
		}
		
		for (int i=4; i<8; ++i) {
			System.out.println(Short.toString(getShort(i)));
			System.out.println(Byte.toString(getByte(i)));
			System.out.println(Character.toString(getChar(i)));
			System.out.println(Boolean.toString(getBoolean(i)));
		}
	}
	
	private static short getShort(int index) {
		return s[index];
	}

	private static char getChar(int index) {
		return c[index];
	}

	private static boolean getBoolean(int index) {
		return z[index];
	}

	private static byte getByte(int index) {
		return b[index];
	}

}
