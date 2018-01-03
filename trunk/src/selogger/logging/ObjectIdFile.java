package selogger.logging;

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
	protected void onNewObject(Object o, long id) {
		objectIdList.write(Long.toString(id));
		objectIdList.write(",");
		objectIdList.write(typeToId.getTypeIdString(o.getClass()));
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
				
				exceptionList.write(Long.toString(id));
				exceptionList.write(",M,");
				exceptionList.write(t.getMessage());
				exceptionList.write("\n");
				exceptionList.write(Long.toString(id));
				exceptionList.write(",CS,");
				exceptionList.write(Long.toString(causeId));
				for (int i=0; i<suppressedId.length; ++i) {
					exceptionList.write(",");
					exceptionList.write(Long.toString(suppressedId[i]));
				}
				exceptionList.write("\n");

				StackTraceElement[] trace = t.getStackTrace();
				for (int i=0; i<trace.length; ++i) {
					exceptionList.write(Long.toString(id));
					exceptionList.write(",S,");
					StackTraceElement e = trace[i];
					exceptionList.write(e.isNativeMethod() ? "T," : "F,");
					exceptionList.write(e.getClassName());
					exceptionList.write(",");
					exceptionList.write(e.getMethodName());
					exceptionList.write(",");
					exceptionList.write(e.getFileName());
					exceptionList.write(",");
					exceptionList.write(Integer.toString(e.getLineNumber()));
					exceptionList.write("\n");
				}
				
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
