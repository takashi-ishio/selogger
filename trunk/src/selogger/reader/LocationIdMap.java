package selogger.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import selogger.EventType;
import selogger.weaver.WeavingInfo;
import selogger.weaver.method.Descriptor;

/**
 * This class uses "long" to represent a location ID for future extensibility.
 * @author ishio
 *
 */
public class LocationIdMap {

	private ArrayList<ClassInfo> classes;
	private ArrayList<LocationId> locations;
	private ArrayList<MethodInfo> methods;
	private UniqueString strings;
	
	public LocationIdMap(File dir) throws IOException {
		classes = new ArrayList<ClassInfo>(1024 * 1024);
		locations = new ArrayList<LocationId>(4 * 1024 * 1024);
		methods = new ArrayList<MethodInfo>(1024 * 1024);
		strings = new UniqueString();
		
		loadClassIdFile(dir);
		loadMethodIdFile(dir);
			
		File[] files = getLocationFiles(dir);
		FileListReader reader = new FileListReader(files);
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			processDataId(line);
		}
		reader.close();
	}
	
	private void loadClassIdFile(File dir) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(dir, WeavingInfo.CLASS_ID_FILE)));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			classes.add(ClassInfo.parse(line));
			assert classes.get(classes.size()-1).getId() == classes.size(): "Class Index must be consistent with Class ID.";
		}
		reader.close();
	}
	
	/**
	 * The data format is defined in {@link WeavingInfo#startMethod(String, String, String, int, String)}.
	 * @param dir
	 * @throws IOException
	 */
	private void loadMethodIdFile(File dir) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(dir, WeavingInfo.METHOD_ID_FILE)));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {

			LineParser parser = new LineParser(line);
			int classId = parser.readInt();
			int methodId = parser.readInt();
			String methodOwnerClass = strings.getSharedInstance(parser.readString());
			String methodName = strings.getSharedInstance(parser.readString());
			String methodDesc = strings.getSharedInstance(parser.readString());
			int access = parser.readInt();
			String sourceFileName = strings.getSharedInstance(parser.readString());
			if (sourceFileName.length() == 0) sourceFileName = null;
			
			MethodInfo methodInfo;
			if (0 <= classId && classId < classes.size()) {
				ClassInfo c = classes.get(classId-1);
				assert c.getClassName().equals(methodOwnerClass): "Class Name (" + c.getClassName() +") is inconsistent to method owner class (" + methodOwnerClass + ")";
				methodInfo = new MethodInfo(c, methodOwnerClass, methodId, methodName, methodDesc, access, sourceFileName);
			} else {
				methodInfo = new MethodInfo(classId, methodOwnerClass, methodId, methodName, methodDesc, access, sourceFileName);
			}
			methods.add(methodInfo);
			assert methods.get(methodId-1) == methodInfo;
		}
		reader.close();
	}
	
	/**
	 * Read a data ID.  The format is defined in {@link WeavingInfo#nextDataId(int, int, EventType, Descriptor, String)}.
	 * @param line
	 */
	private void processDataId(String line) {
		LocationId l = new LocationId();
		
		// TODO Replace LineParser with java.util.Scanner
		LineParser parser = new LineParser(line);
		l.locationId = parser.readLong();
		int classId = parser.readInt();
		int methodId = parser.readInt();
		l.methodId = methodId;
		l.methodInfo = methods.get(methodId-1);
		assert classId == l.methodInfo.getClassId();
		l.line = parser.readInt();
		l.instructionIndex = parser.readInt();
		l.eventType = EventType.values()[parser.readInt()];
		String valueDesc = parser.readString();
		l.valueType = Descriptor.get(valueDesc);
		l.label = strings.getSharedInstance(parser.readRemaining());
		
		locations.add(l);
		assert locations.size() == l.locationId;
	}
	
	
	private static class LocationId {
		private long locationId;
		private long relevantLocationId;
		private MethodInfo methodInfo;
		private int line;
		private int instructionIndex;
		private EventType eventType;
		private String label;
		private int methodId; 
		private Descriptor valueType;
	}
	
	/**
	 * @return the maximum valid location ID.
	 */
	public long getMaxId() {
		return locations.size()-1;
	}
	
	private LocationId getLocation(long dataId) {
		assert 1 <= dataId && dataId <= Integer.MAX_VALUE;
		return locations.get((int)dataId-1);
	}
	
	/**
	 * This value is valid during a single execution.
	 * @param dataId
	 * @return
	 */
	public int getMethodId(long dataId) {
		return getLocation(dataId).methodId;
	}
	
	public MethodInfo getMethodInfo(long dataId) {
		return getLocation(dataId).methodInfo;
	}
	
	public long getRelevantLocationId(long dataId) {
		return getLocation(dataId).relevantLocationId;
	}
	
	public EventType getEventType(long dataId) {
		return getLocation(dataId).eventType;
	}

	public Descriptor getValueType(long dataId) {
		return getLocation(dataId).valueType;
	}

	/**
	 * Return an instruction index. 
	 * This is valid only for events related to method invocation, field access and execution labels.
	 * The index can be used for an argument of org.objectweb.asm.tree.InsnList#get(int).
	 * @param dataId
	 * @return
	 */
	public int getInstructionIndex(long dataId) {
		return getLocation(dataId).instructionIndex;
	}

	public String getSourceFileName(long dataId) {
		return getLocation(dataId).methodInfo.getSourceFileName();
	}

	public int getLineNumber(long dataId) {
		return getLocation(dataId).line;
	}

	public String getLabel(long dataId) {
		return getLocation(dataId).label;
	}
	
	private File[] getLocationFiles(File dir) throws FileNotFoundException {
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(WeavingInfo.LOCATION_ID_PREFIX) && 
							name.endsWith(WeavingInfo.LOCATION_ID_SUFFIX);
			}
		});
		if (files != null) {
			Arrays.sort(files, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			return files;
		} else {
			throw new FileNotFoundException("Location files are not found in " + dir.getAbsolutePath());
		}
	}
	

}
