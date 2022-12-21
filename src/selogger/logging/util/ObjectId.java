package selogger.logging.util;

/**
 * This object represents an object ID.
 * It comprises an identification number and a class name.
 * It also keeps a string content if the class is either String or Throwable.
 */
public class ObjectId {

	private long id;
	private String className;
	private String content;
	
	/**
	 * Create an object ID.
	 * @param id specifies an identification number.
	 * @param className specifies a class name.
	 * @param content specifies a textual content.
	 */
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
