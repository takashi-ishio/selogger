package selogger.logging.util;

import java.io.File;
import java.io.IOException;


public class ObjectIdFile extends ObjectIdMap {

	private final String lineSeparator = "\n";
	
	private StringFileListStream objectIdList;
	private TypeIdMap typeToId;
	private FileNameGenerator filenames;
	private StringFileListStream exceptionList;
	
	private StringContentFile stringContentList;

	public static final long ID_NOT_FOUND = -1;
	
	public static long cacheHit = 0;
	public static long cacheMiss = 0;

	public ObjectIdFile(File outputDir, boolean recordString, TypeIdMap typeToId) throws IOException {
		super(16 * 1024 * 1024);
		this.typeToId = typeToId;
		
		filenames = new FileNameGenerator(outputDir, "LOG$ObjectTypes", ".txt");
		objectIdList = new StringFileListStream(filenames, 10000000, 100 * 1024 * 1024, false);

		exceptionList = new StringFileListStream(new FileNameGenerator(outputDir, "LOG$Exceptions", ".txt"), 1000000, 100 * 1024 * 1024, false);
		
		if (recordString) {
			stringContentList = new StringContentFile(outputDir);
		}
	}
	
	@Override
	protected void onNewObject(Object o) {
		typeToId.getTypeIdString(o.getClass());
	}

	@Override
	protected void onNewObjectId(Object o, long id) {
		String typeId = typeToId.getTypeIdString(o.getClass());
		objectIdList.write(Long.toString(id));
		objectIdList.write(",");
		objectIdList.write(typeId);
		objectIdList.write(lineSeparator);
		
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
				
				StringBuilder builder = new StringBuilder(1028);
				builder.append(Long.toString(id));
				builder.append(",M,");
				builder.append(t.getMessage());
				builder.append("\n");
				builder.append(Long.toString(id));
				builder.append(",CS,");
				builder.append(Long.toString(causeId));
				for (int i=0; i<suppressedId.length; ++i) {
					builder.append(",");
					builder.append(Long.toString(suppressedId[i]));
				}
				builder.append("\n");

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
				exceptionList.write(builder.toString());
				
			} catch (Throwable e) {
				// ignore all exceptions
			}
		}
	}
	
	public void close() {
		objectIdList.close();
		exceptionList.close();
		if (stringContentList != null) stringContentList.close();
	}
	
}
