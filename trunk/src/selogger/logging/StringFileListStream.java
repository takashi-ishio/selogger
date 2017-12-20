package selogger.logging;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class StringFileListStream {

	private SequentialFileName filenames;
	private long itemPerFile;
		
	private long itemCount;
	private boolean compress;

	private ByteArrayOutputStream buffer;
	
	public StringFileListStream(SequentialFileName filenames, long itemPerFile, int bufferSize, boolean compress) {
		this.filenames = filenames;
		this.itemPerFile = itemPerFile;
		this.compress = compress;
		
		this.itemCount = 0;
		
		buffer = new ByteArrayOutputStream(bufferSize);
	}
	
	public synchronized void write(String s) {
		try {
			if (s != null) buffer.write(s.getBytes());
		} catch (IOException e) {
			// Ignore IOException because ByteArrayOutputStream does not likely throws an exception.
		}
		itemCount++;
		if (itemCount == itemPerFile) {
			save();
			buffer.reset();
			itemCount = 0;
		}
	}
	
	public synchronized void close() {
		if (itemCount > 0) {
			try {
				buffer.close();
				save();
			} catch (IOException e) {
			}
		}
	}
	
	private void save() {
		File f = filenames.getNextFile();
		try {
			if (compress) {
				GZIPOutputStream w = new GZIPOutputStream(new FileOutputStream(f));
				buffer.writeTo(w);
				w.close();
			} else {
				BufferedOutputStream w = new BufferedOutputStream(new FileOutputStream(f));
				buffer.writeTo(w);
				w.close();
			}
		} catch (IOException e) {
			throw new RuntimeException("ERROR: failed to open a file: " + f.getAbsolutePath());
		}
		
	}
}
