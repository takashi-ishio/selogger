package selogger.logging.util;

import java.io.File;

public class FileNameGenerator {
	
	private int fileCount = 0;
	private File dir;
	private String prefix;
	private String suffix;
	
	public FileNameGenerator(File dir, String prefix, String suffix) {
		this.dir = dir;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	public File getNextFile() {
		return new File(dir, prefix + String.format("%05d", ++fileCount) + suffix);
	}
}
