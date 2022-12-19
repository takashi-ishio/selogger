package selogger.testdata;

public class Demo {

	public static void main(String[] args) {
		int s = 0;
		for (int i=0; i<10; i++) {
			s = Integer.sum(s, i);
		}
		System.out.println(s);
	}
}
