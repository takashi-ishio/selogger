package selogger.logging.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * A utility class to write strings to files. 
 */
public class StringFileListStream {

	private FileNameGenerator filenames;
	private long itemPerFile;
		
	private long itemCount;
	private boolean compress;

	private ByteArrayOutputStream buffer;
	
	/**
	 * 
	 *  
	 * @param filenames specifies a file name generator for files to be written. 
	 * @param itemPerFile specifies the number of strings stored in a single file.  
	 * @param bufferSize specifies the size of an internal buffer used by this object for performance.  
	 * It should be a sufficiently large number.
	 * @param compress option enables to compress the output file in GZip. 
	 */
	public StringFileListStream(FileNameGenerator filenames, long itemPerFile, int bufferSize, boolean compress) {
		this.filenames = filenames;
		this.itemPerFile = itemPerFile;
		this.compress = compress;
		
		this.itemCount = 0;
		
		buffer = new ByteArrayOutputStream(bufferSize);
	}
	
	/**
	 * Write a string.
	 * The string is temporaliry stored in an internal buffer. 
	 * @param s is a String.
	 */
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
	
	/**
	 * Output strings in the internal buffer to a file, 
	 * and then close the stream.
	 */
	public synchronized void close() {
		if (itemCount > 0) {
			try {
				buffer.close();
				save();
			} catch (IOException e) {
			}
		}
	}
	
	/**
	 * Save the internal buffer to a file.
	 */
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
