package selogger.logging;

import java.io.File;
import java.io.IOException;


public class StringContentFile {
	
	private StringFileListStream stringList;

	public StringContentFile(File outputDir) throws IOException {
		SequentialFileName filenames = new SequentialFileName(outputDir, "LOG$String", ".txt", 6);
		stringList = new StringFileListStream(filenames, 100000, 200 * 1024 * 1024, false);
	}

	public void write(long objectId, String content) {
		StringBuilder builder = new StringBuilder(content.length() + 32);
		builder.append(Long.toString(objectId));
		builder.append(",");
		builder.append(Integer.toString(content.length()));
		builder.append(",");
		builder.append(content);
		builder.append("\0\n");
		stringList.write(builder.toString());
	}
	
	public void close() {
		stringList.close();
	}
	
}
