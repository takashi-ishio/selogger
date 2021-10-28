package selogger.logging.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

/**
 * A utility class to write strings to files. 
 */
public class StringFileListStream {

	private FileNameGenerator filenames;
	private long itemPerFile;
		
	private long itemCount;
	private boolean compress;

	private PrintWriter writer;
	
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
		
		prepareNextFile();
	}
	
	private void prepareNextFile() {
		if (writer != null) {
			writer.close();
		}
		File f = filenames.getNextFile();
		try {
			if (compress) {
				GZIPOutputStream w = new GZIPOutputStream(new FileOutputStream(f));
				writer = new PrintWriter(new OutputStreamWriter(w));
			} else {
				BufferedOutputStream w = new BufferedOutputStream(new FileOutputStream(f));
				writer = new PrintWriter(new OutputStreamWriter(w));
			}
		} catch (IOException e) {
			writer = null;
		}
	}
	
	/**
	 * Write a string.
	 * @param s is a String.
	 */
	public synchronized void write(String s) {
		if (writer != null) {
			writer.print(s);
			itemCount++;
			if (itemCount >= itemPerFile) {
				prepareNextFile();
				itemCount = 0;
			}
		}
	}
	
	/**
	 * Output strings in the internal buffer to a file, 
	 * and then close the stream.
	 */
	public synchronized void close() {
		writer.close();
		writer = null;
	}
	
}
