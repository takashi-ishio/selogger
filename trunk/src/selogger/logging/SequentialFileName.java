package selogger.logging;

import java.io.File;

public class SequentialFileName {

	private File dir;
	private String prefix;
	private String suffix;
	private int zeroNum;
	private String zeroFiller;
	private int maxCount;

	private int fileCount;

	public SequentialFileName(File dir, String prefix, String suffix, int zeroNum) {
		this.dir = dir;
		this.prefix = prefix;
		this.suffix = suffix;
		this.zeroNum = zeroNum;

		// compute zero string (e.g. "00000")
		StringBuilder zero = new StringBuilder();
		int count = 1;
		for (int i=0; i<zeroNum; ++i) {
			zero.append("0");
			count *= 10;
		}
		zeroFiller = zero.toString();
		maxCount = count;
		
		assert zeroFiller.length() == zeroNum;
	}
	
	public File getNextFile() {
		fileCount++;
		String count = zeroFiller + Integer.toString(fileCount);
		if (fileCount < maxCount) {
			count = count.substring(count.length() - zeroNum);
		}
		String filename = prefix + count + suffix;
		return new File(dir, filename);
	}
	
}
