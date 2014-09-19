package selogger.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;



public class FileListReader {

	private int fileIndex;
	private BufferedReader reader;
	private File[] files;
	
	private static final int BUFFER_SIZE = 4 * 1024 * 1024;
	
	public FileListReader(File[] eventFiles) throws IOException {
		this.fileIndex = 0;
		this.files = eventFiles;
		reader = openFile();
	}
	
	/**
	 * Open a stream for the next file.
	 * @return
	 * @throws IOException
	 */
	private BufferedReader openFile() throws IOException {
		InputStream stream = new FileInputStream(files[fileIndex]);
		if (files[fileIndex].getName().endsWith(".gz")) {
			stream = new GZIPInputStream(stream);
		} 
		return new BufferedReader(new InputStreamReader(stream, "US-ASCII"), BUFFER_SIZE);	
	}
	
	/**
	 * @return a line read from a file.  This method returns null if files have no more lines. 
	 * @throws IOException
	 */
	public String readLine() throws IOException {
		while (!reader.ready()) {
			fileIndex++;
			reader.close();
			if (fileIndex < files.length) {
				reader = openFile();
			} else {
				reader = null;
				return null;
			}
		} 
		return reader.readLine();
	}


	public void close() throws IOException {
		if (reader != null) reader.close();
	}
}
