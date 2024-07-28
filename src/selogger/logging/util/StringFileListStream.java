package selogger.logging.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

/**
 * A utility class to write strings to files.
 */
public class StringFileListStream {

	/**
	 * File names for a file sequence
	 */
	private FileNameGenerator filenames;
	
	/**
	 * Individual file size
	 */
	private long maxFileSize;

	/**
	 * Enable data compression
	 */
	private boolean compress;

	/**
	 * An internal buffer to keep content
	 */
	private ByteArrayOutputStream buffer;

	/**
	 * The number of bytes in the buffer
	 */
	private long byteCount;


	/**
	 * The default configuration of StringFileListStream.
	 * It splits a stream into fixed size arrays (and then compressed if the compression is enabled).
	 * @param filenames specifies a file name generator for files to be written.
	 */
	public StringFileListStream(FileNameGenerator filenames) {
		this(filenames, PropertyConfiguration.getBufferSize(), false);
	}

	/**
	 * @param filenames   specifies a file name generator for files to be written.
	 * @param maxFileSize specifies the number of bytes stored in a single file.
	 *                    This specifies the size of an internal buffer used by this
	 *                    object for performance. It should be a sufficiently large
	 *                    number.
	 * @param compress    option enables to compress the output file in GZip.
	 */
	public StringFileListStream(FileNameGenerator filenames, int maxFileSize, boolean compress) {
		this.filenames = filenames;
		this.maxFileSize = maxFileSize;
		this.compress = compress;
		this.byteCount = 0;
		this.buffer = new ByteArrayOutputStream(maxFileSize);
	}

	/**
	 * Write a string in UTF-8 format.
	 * @param s is a String.  null and empty strings are ignored.
	 * 
	 */
	public synchronized void write(String s) {
		try {
			if (s != null) {
				byte[] content = s.getBytes(StandardCharsets.UTF_8);
				if (byteCount + content.length > maxFileSize) {
					save();
					buffer.reset();
					byteCount = 0;
				}
				buffer.write(content);
				byteCount += content.length;
			}
		} catch (IOException e) {
			buffer = null;
		}
	}

	/**
	 * Output strings in the internal buffer to a file, and then close the stream.
	 */
	public synchronized void close() {
		if (byteCount > 0) {
			try {
				if (buffer != null) {
					buffer.close();
					save();
				}
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Save the contents of the internal buffer
	 */
	private void save() {
		if (buffer == null) return;
		File f = filenames.getNextFile();
		try {
			if (compress) {
				GZIPOutputStream w = new GZIPOutputStream(new FileOutputStream(f));
				buffer.writeTo(w);
				w.close();
			} else {
				FileOutputStream w = new FileOutputStream(f);
				buffer.writeTo(w);
				w.close();
			}
		} catch (IOException e) {
			throw new RuntimeException("ERROR: failed to open a file: " + f.getAbsolutePath());
		}

	}

}
