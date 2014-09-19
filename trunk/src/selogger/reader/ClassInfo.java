package selogger.reader;

import selogger.weaver.WeavingInfo;

public class ClassInfo {

	private int id;
	private String container;
	private String filename;
	private String className;
	private String loglevel;
	private String md5hash;

	public static ClassInfo parse(String line) {
		String[] values = line.split(WeavingInfo.SEPARATOR);
		int id = Integer.parseInt(values[0]);
		ClassInfo c = new ClassInfo(id, values[1], values[2], values[3], values[4], values[5]);
		return c;
	}
	
	private ClassInfo(int id, String container, String filename, String className, String loglevel, String md5hash) {
		this.id = id;
		this.container = container;
		this.filename = filename;
		this.className = className;
		this.loglevel = loglevel;
		this.md5hash = md5hash;
	}
	
	public int getId() {
		return id;
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getContainer() {
		return container;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public String getLoglevel() {
		return loglevel;
	}
	
	public String getMd5hash() {
		return md5hash;
	}
}
