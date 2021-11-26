package selogger.logging.util;

import java.io.File;
import java.io.IOException;


/**
 * This class added type ID management and file save features to ObjectIdMap class. 
 */
public class ObjectIdFile extends ObjectIdMap {

	public enum ExceptionRecording { Disabled, Message, MessageAndStackTrace };
	
	private final String lineSeparator = "\n";

	private StringFileListStream objectIdList;
	private TypeIdMap typeToId;
	private FileNameGenerator filenames;
	private StringFileListStream exceptionList;
	private ExceptionRecording recordExceptions;
	
	private StringContentFile stringContentList;

	public static final long ID_NOT_FOUND = -1;
	
	public static long cacheHit = 0;
	public static long cacheMiss = 0;

	/**
	 * Create an instance to record object types.
	 * @param outputDir is a directory for output files.
	 * @param recordString is a flag to recording string contents.
	 * If the flag is true, this object records the contents of String objects in files.
	 * @param typeToId is an object to translate a type into an integer representing a type.
	 * @throws IOException
	 */
	public ObjectIdFile(File outputDir, boolean recordString, TypeIdMap typeToId) throws IOException {
		this(outputDir, recordString, recordString ? ExceptionRecording.MessageAndStackTrace : ExceptionRecording.Disabled, typeToId);
	}

	/**
	 * Create an instance to record object types.
	 * @param outputDir is a directory for output files.
	 * @param recordString is a flag to recording string contents.
	 * If the flag is true, this object records the contents of String objects in files.
	 * @param typeToId is an object to translate a type into an integer representing a type.
	 * @throws IOException
	 */
	public ObjectIdFile(File outputDir, boolean recordString, ExceptionRecording recordExceptions, TypeIdMap typeToId) throws IOException {
		super(16 * 1024 * 1024);
		this.typeToId = typeToId;
		
		filenames = new FileNameGenerator(outputDir, "LOG$ObjectTypes", ".txt");
		objectIdList = new StringFileListStream(filenames);

		this.recordExceptions = recordExceptions;
		if (this.recordExceptions != ExceptionRecording.Disabled) {
			exceptionList = new StringFileListStream(new FileNameGenerator(outputDir, "LOG$Exceptions", ".txt"));
		}
		
		if (recordString) {
			stringContentList = new StringContentFile(outputDir);
		}
	}

	/**
	 * Register a type for each new object.
	 * This is separated from onNewObjectId because this method 
	 * calls TypeIdMap.createTypeRecord that may call a ClassLoader's method.
	 * If the ClassLoader is also monitored by SELogger,  
	 * the call indirectly creates another object ID.
	 */
	@Override
	protected void onNewObject(Object o) {
		typeToId.getTypeIdString(o.getClass());
	}

	/**
	 * Record an object ID and its Type ID in a file.
	 * In case of String and Throwable, this method also record their textual contents.
	 */
	@Override
	protected void onNewObjectId(Object o, long id) {
		String typeId = typeToId.getTypeIdString(o.getClass());
		StringBuilder element = new StringBuilder(typeId.length() + 24);
		element.append(id);
		element.append(",");
		element.append(typeId);
		element.append(lineSeparator);
		objectIdList.write(element.toString());
		
		if (o instanceof String) {
			if (stringContentList != null) {
				stringContentList.write(id, (String)o);
			}
		} else if (o instanceof Throwable) {
			try {
				Throwable t = (Throwable)o;
				long causeId = getId(t.getCause());
				Throwable[] suppressed = t.getSuppressed();
				long[] suppressedId = new long[suppressed.length];
				for (int i=0; i<suppressedId.length; ++i) {
					suppressedId[i] = getId(suppressed[i]); 
				}
				
				if (exceptionList != null && recordExceptions != ExceptionRecording.Disabled) {
					StringBuilder builder = new StringBuilder(1028);
					// Record exception message
					builder.append(Long.toString(id));
					builder.append(",M,");
					builder.append(t.getMessage());
					builder.append("\n");

					if (recordExceptions == ExceptionRecording.MessageAndStackTrace) {
						// Record cause objects
						builder.append(Long.toString(id));
						builder.append(",CS,");
						builder.append(Long.toString(causeId));
						for (int i=0; i<suppressedId.length; ++i) {
							builder.append(",");
							builder.append(Long.toString(suppressedId[i]));
						}
						builder.append("\n");

						// Record sack traces
						StackTraceElement[] trace = t.getStackTrace();
						for (int i=0; i<trace.length; ++i) {
							builder.append(Long.toString(id));
							builder.append(",S,");
							StackTraceElement e = trace[i];
							builder.append(e.isNativeMethod() ? "T," : "F,");
							builder.append(e.getClassName());
							builder.append(",");
							builder.append(e.getMethodName());
							builder.append(",");
							builder.append(e.getFileName());
							builder.append(",");
							builder.append(Integer.toString(e.getLineNumber()));
							builder.append("\n");
						}
					}
					exceptionList.write(builder.toString());
				}
				
			} catch (Throwable e) {
				// ignore all exceptions
			}
		}
	}
	
	/**
	 * Close the files written by this object.
	 */
	public synchronized void close() {
		objectIdList.close();
		if (exceptionList != null) exceptionList.close();
		if (stringContentList != null) stringContentList.close();
	}
	
}
