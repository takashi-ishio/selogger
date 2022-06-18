package selogger.logging.util;

public class ObjectId {

	private long id;
	private String className;
	private String content;
	
	public ObjectId(long id, String className, String content) {
		this.id = id;
		this.className = className;
		this.content = content;
	}

	public long getId() {
		return id;
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getContent() {
		return content;
	}
}
