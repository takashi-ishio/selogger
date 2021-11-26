package selogger.logging.util;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.io.JsonStringEncoder;


/**
 * This class is to record the contents of String objects.
 */
public class StringContentFile {
	
	private StringFileListStream stringList;

	/**
	 * Create an instance.
	 * @param outputDir specifies a directory for storing output files.
	 * @throws IOException
	 */
	public StringContentFile(File outputDir) throws IOException {
		FileNameGenerator filenames = new FileNameGenerator(outputDir, "LOG$String", ".txt");
		stringList = new StringFileListStream(filenames);
	}

	/**
	 * Record a String. 
	 * @param objectId specifies the object ID of the content object.
	 * @param content specifies the string to be recorded.
	 * TODO Improve the file format 
	 */
	public void write(long objectId, String content) {
		StringBuilder builder = new StringBuilder(content.length() + 32);
		builder.append(objectId);
		builder.append(",");
		builder.append(content.length());
		builder.append(",");
		builder.append("\"");
		JsonStringEncoder.getInstance().quoteAsString(content, builder);
		builder.append("\"");
		builder.append("\n");
		stringList.write(builder.toString());
	}

	/**
	 * Close the stream.
	 */
	public void close() {
		stringList.close();
	}
	
}
