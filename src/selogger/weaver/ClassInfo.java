package selogger.weaver;

import java.util.Scanner;

public class ClassInfo {

	private static final String SEPARATOR = ",";
	
	private int classId;
	private String container;
	private String filename;
	private String className;
	private LogLevel loglevel;
	private String hash;

	public ClassInfo(int classId, String container, String filename, String className, LogLevel level, String hash) {
		this.classId = classId;
		this.container = container;
		this.filename = filename;
		this.className = className;
		this.loglevel = level;
		this.hash = hash;
	}
	
	public int getClassId() {
		return classId;
	}
	
	/**
	 * @return class name including its package name.
	 */
	public String getClassName() {
		return className;
	}
	
	/**
	 * @return a container where a class is loaded.
	 */
	public String getContainer() {
		return container;
	}
	
	/**
	 * @return
	 */
	public String getFilename() {
		return filename;
	}
	
	public String getHash() {
		return hash;
	}
	
	public LogLevel getLoglevel() {
		return loglevel;
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(classId);
		buf.append(SEPARATOR);
		buf.append(container);
		buf.append(SEPARATOR);
		buf.append(filename);
		buf.append(SEPARATOR);
		buf.append(className);
		buf.append(SEPARATOR);
		buf.append(loglevel.name());
		buf.append(SEPARATOR);
		buf.append(hash);
		return buf.toString();
	}
	
	public static ClassInfo parse(String s) {
		Scanner sc = new Scanner(s);
		sc.useDelimiter(SEPARATOR);
		int classId = sc.nextInt();
		String container = sc.next();
		String filename = sc.next();
		String className = sc.next();
		LogLevel level = LogLevel.valueOf(sc.next());
		String hash = sc.next();
		sc.close();
		return new ClassInfo(classId, container, filename, className, level, hash);
	}
}
