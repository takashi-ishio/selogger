package selogger.logging.io;

import java.io.File;

public class FileNameGenerator implements IFileNames {
	
	private int fileCount = 0;
	private File dir;
	
	public FileNameGenerator(File dir) {
		this.dir = dir;
	}

	@Override
	public File getNextFile() {
		return new File(dir, "log-" + String.format("%05d", ++fileCount) + ".slg");
	}
}
