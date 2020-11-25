package selogger.reader;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

/**
 * A utility class to read files in a directory having the same prefix and suffix
 */
public class SequentialFileList {
	
	/**
	 * Get a list of files in a directory whose file names are in 
	 * a [PREFIX][NUMBER][.SUFFIX] format such as "log-00001.slg".
	 * @param dir is a directory including files.  No subdirectories are searched.
	 * @param prefix is the common prefix of the files.
	 * @param suffix is the common suffix of the files.
	 * @return a list files sorted by the number part between the prefix and suffix.
	 */
	public static File[] getSortedList(File dir, final String prefix, final String suffix) {
		File[] f = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(prefix) && name.endsWith(suffix);
			}
		});
		FileIndex[] sorter = new FileIndex[f.length];
		for (int i=0; i<f.length; ++i) {
			sorter[i] = new FileIndex(f[i], prefix, suffix);
		}
		Arrays.sort(sorter);
		
		for (int i=0; i<f.length; ++i) {
			f[i] = sorter[i].file;
		}
		return f;
	}
	

	/**
	 * A sort key object for files having the same prefix and suffix
	 */
	private static class FileIndex implements Comparable<FileIndex> {
		
		private File file;
		private int index;
		
		public FileIndex(File f, String prefix, String suffix) {
			assert f.getName().startsWith(prefix) && f.getName().endsWith(suffix);
			this.file = f;
			
			String suffixRemoved = f.getName().substring(0, f.getName().length()-suffix.length());
			String order = suffixRemoved.substring(prefix.length());
			this.index = Integer.parseInt(order);
		}
		
		@Override
		public int compareTo(FileIndex o) {
			if (o == null) return 1;
			else return this.index - o.index;
		}
	}

}
