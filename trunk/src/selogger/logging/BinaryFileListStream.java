package selogger.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


import selogger.Config;

public abstract class BinaryFileListStream implements IEventWriter {

	public static final int EVENTS_PER_FILE = 10000000;

	SequentialFileName filenames;
	
	private boolean closed;
		
	private int bytesPerEvent;

	protected long counter;
	protected ByteBuffer buffer;
	private boolean compressEvents;
	private boolean discardEvents;
	
	private boolean ioexceptionThrown;
	private String ioexceptionMessage;
	
	private int threadCount;
	ExecutorService service;
	private ByteBuffer[] buffers;
	private int bufferIndex = 0;
	
	public BinaryFileListStream(SequentialFileName filenames, Config.OutputOption outputOption, int threads, int bytesPerEvent) {
		this.filenames = filenames;
		this.compressEvents = outputOption.isCompressEnabled();
		this.discardEvents = outputOption.isDiscardEnabled();
		this.bytesPerEvent = bytesPerEvent;
		
		threadCount = threads;
		
		buffers = new ByteBuffer[1 + threadCount];
		for (int i=0; i<buffers.length; ++i) {
			buffers[i] = ByteBuffer.allocate(bytesPerEvent * EVENTS_PER_FILE);
		}
		buffer = buffers[0];

		if (threadCount > 0) {
			service = Executors.newFixedThreadPool(threadCount);
		}
	}
	
	
	public void close() {
		closed = true;
		try {
			if (service != null) {
				service.shutdown();
				service.awaitTermination(1, TimeUnit.DAYS);
			}
		} catch (InterruptedException interrupt) {
			// ignore the exception
		}
		if (counter > 0) {
			save(); // never throws IOException
		}
	}
	
	protected void save() {
		try {
			final File f = filenames.getNextFile();
			
			final ByteBuffer toBeSaved = buffer;
			try {
				if (!closed && threadCount > 0) {
					service.execute(new Runnable() {
						
						@Override
						public void run() {
							synchronized(toBeSaved) {
								try {
									writeToFile(toBeSaved, f);
								} catch (IOException e) {
									recordException(e);
								}
							}
						}
					});
				} else { 
					writeToFile(toBeSaved, f);
				}
			} catch (IOException e) {
				recordException(e);
			}
			
			if (closed && threadCount > 0) { 
				// clean up all buffers, to wait all writeTOFile are completed 
				for (int i=0; i<buffers.length; ++i) {
					buffers[i].clear();
				}
			}
			
			bufferIndex++;
			if (bufferIndex >= buffers.length) bufferIndex = 0;
			buffer = buffers[bufferIndex];
			synchronized (buffer) {
				buffer.clear();
			}
			counter = 0;
		} catch (RuntimeException e) {
			recordException(e);
			throw e;
		}
	}
	
	public boolean hasError() {
		return ioexceptionThrown;
	}
	
	public String getErrorMessage() {
		return ioexceptionMessage;
	}
	
	private synchronized void recordException(Throwable e) {
		ioexceptionThrown = true;
		ioexceptionMessage = e.getLocalizedMessage();
	}
	
	
	
	public void writeToFile(ByteBuffer buf, File f) throws IOException {
		buf.flip();
		if (!discardEvents) {
			if (compressEvents) {
				// The size of a compression buffer and the compression level are experimentally related to each other. 
				CompressionBuffer t = new CompressionBuffer((bytesPerEvent / 4) * EVENTS_PER_FILE);
				DeflaterOutputStream out = new DeflaterOutputStream(t, new Deflater(3));
				out.write(buf.array(), buf.position(), buf.limit()-buf.position());
				out.close();
				FileOutputStream w = new FileOutputStream(f);
				w.getChannel().write(t.getByteBuffer());
				w.close();
			} else {
				FileOutputStream w = new FileOutputStream(f);
				w.getChannel().write(buf);
				w.close();
			}
		}
	}

	public abstract void registerEventWithoutData(int eventType, long eventId, int threadId, long locationId);
	public abstract void registerLong(int eventType, long eventId, int threadId, long locationId, long longData);
	public abstract void registerLongInt(int eventType, long eventId, int threadId, long locationId, long longData, int intData);
	public abstract void registerLongValue(int eventType, long eventId, int threadId, long locationId, long longData, double value);
	public abstract void registerLongValue(int eventType, long eventId, int threadId, long locationId, long longData, float value);
	public abstract void registerLongValue(int eventType, long eventId, int threadId, long locationId, long longData, int value);
	public abstract void registerLongValue(int eventType, long eventId, int threadId, long locationId, long longData, long value);
	public abstract void registerIntValue(int eventType, long eventId, int threadId, long locationId, int intData, double value);
	public abstract void registerIntValue(int eventType, long eventId, int threadId, long locationId, int intData, float value);
	public abstract void registerIntValue(int eventType, long eventId, int threadId, long locationId, int intData, int value);
	public abstract void registerIntValue(int eventType, long eventId, int threadId, long locationId, int intData, long value);
	public abstract void registerLongIntValue(int eventType, long eventId, int threadId, long locationId, long longData, int intData, double value);
	public abstract void registerLongIntValue(int eventType, long eventId, int threadId, long locationId, long longData, int intData, float value);
	public abstract void registerLongIntValue(int eventType, long eventId, int threadId, long locationId, long longData, int intData, int value);
	public abstract void registerLongIntValue(int eventType, long eventId, int threadId, long locationId, long longData, int intData, long value);
	public abstract void registerValue(int eventType, long eventId, int threadId, long locationId, double value);
	public abstract void registerValue(int eventType, long eventId, int threadId, long locationId, float value);
	public abstract void registerValue(int eventType, long eventId, int threadId, long locationId, int value);
	public abstract void registerValue(int eventType, long eventId, int threadId, long locationId, long value);
	public abstract void registerValueVoid(int eventType, long eventId, int threadId, long locationId);

}
