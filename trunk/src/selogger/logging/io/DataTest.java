package selogger.logging.io;

import java.io.File;

public class DataTest {

	public static void main(String[] args) {
		EventDataStream stream = new EventDataStream(new FileNameGenerator(new File("F:/")), new IErrorLogger() {
			@Override
			public void record(Throwable t) {
				t.printStackTrace(System.err);
			}
			@Override
			public void close() {
			}
			@Override
			public void record(String msg) {
			}
		});
		long t = System.currentTimeMillis();
		int count = 0;
		while (count < 20000000) {
			count++;
			stream.write(count, ((long)count) << 32);
		}
		stream.close();
		System.err.println(System.currentTimeMillis() - t);
		t = System.currentTimeMillis();
		System.err.println(count); 
			
	}

}
