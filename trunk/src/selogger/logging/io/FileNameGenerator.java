package selogger.logging.io;

import java.io.File;

public class FileNameGenerator implements IFileNames {
	
	public static final String FILE_PREFIX = "log-";
	public static final String FILE_SUFFIX = ".slg";
	
	private int fileCount = 0;
	private File dir;
	
	public FileNameGenerator(File dir) {
		this.dir = dir;
	}

	@Override
	public File getNextFile() {
		return new File(dir, FILE_PREFIX + String.format("%05d", ++fileCount) + FILE_SUFFIX);
	}
}
