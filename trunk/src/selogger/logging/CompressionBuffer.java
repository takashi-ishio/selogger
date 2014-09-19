package selogger.logging;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class CompressionBuffer extends ByteArrayOutputStream {

	public CompressionBuffer(int bufferSize) {
		super(bufferSize);
	}
	
	public ByteBuffer getByteBuffer()  {
		return ByteBuffer.wrap(buf, 0, size());
	}

}
