package selogger.logging;

import java.io.IOException;

import selogger.Config;

public class StringContentFile {
	
	private StringFileListStream stringList;
	private boolean enabled;

	public StringContentFile(Config config) throws IOException {
		this.enabled = config.isStringOutputEnabled();
		if (enabled) {
			SequentialFileName filenames = new SequentialFileName(config.getOutputDir(), "LOG$String", ".txt", 6);
			stringList = new StringFileListStream(filenames, 100000, 200 * 1024 * 1024, false);
		}
	}

	public void write(long objectId, String content) {
		if (enabled) {
			StringBuilder builder = new StringBuilder(content.length() + 32);
			builder.append(Long.toString(objectId));
			builder.append(",");
			builder.append(Integer.toString(content.length()));
			builder.append(",");
			builder.append(content);
			builder.append("\0\n");
			stringList.write(builder.toString());
		}
	}
	
	public void close() {
		if (enabled) stringList.close();
	}
	
}
