package selogger.reader;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import selogger.weaver.WeavingInfo;

/**
 * This class uses "long" to represent a location ID for future extensibility.
 * @author ishio
 *
 */
public class LocationIdMap {

	private ArrayList<ClassInfo> classes;
	private ArrayList<LocationId> locations;
	private TObjectIntHashMap<String> methodIds;
	
	public LocationIdMap(File dir) throws IOException {
		classes = new ArrayList<ClassInfo>(1024 * 1024);
		locations = new ArrayList<LocationId>(4 * 1024 * 1024);
		methodIds = new TObjectIntHashMap<String>();
		
		try {
			
			LineNumberReader reader = new LineNumberReader(new FileReader(new File(dir, WeavingInfo.CLASS_ID_FILE)));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				classes.add(ClassInfo.parse(line));
				assert classes.get(classes.size()-1).getId() == classes.size() - 1: "Class Index must be consistent with Class ID.";
			}
			reader.close();
		} catch (IOException e) {
		}

		File[] files = getLocationFiles(dir);
		FileListReader reader = new FileListReader(files);
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			processLocationId(line);
		}
		reader.close();
	}
	
	private void processLocationId(String line) {
		LocationId l = new LocationId();
		
		int idx = 0;
		
		String[] values = line.split(WeavingInfo.SEPARATOR);
		if (values.length == 8) { // the first version
			l.locationId = Long.parseLong(values[idx++]);
			l.methodInfo = new MethodInfo(values[idx], values[idx+1], values[idx+2], Integer.parseInt(values[idx+3]));
			idx += 4;
			l.sourceFileName = values[idx++];
			l.line = Integer.parseInt(values[idx++]);
			l.label = values[idx++];
			
		} else if (values.length == 9) { // before 2013/11
			l.locationId = Long.parseLong(values[idx++]);
			l.methodInfo = new MethodInfo(values[idx], values[idx+1], values[idx+2], Integer.parseInt(values[idx+3]));
			idx += 4;
			l.sourceFileName = values[idx++];
			l.line = Integer.parseInt(values[idx++]);
			l.instructionIndex = Integer.parseInt(values[idx++]);
			l.label = values[idx++];
			
		} else if (values.length == 10) { // 2013/11 version
			l.locationId = Long.parseLong(values[idx++]);
			int classId = Integer.parseInt(values[idx]);
			if (0 <= classId && classId < classes.size()) {
				ClassInfo c = classes.get(classId);
				assert c.getClassName().equals(values[idx+1]);
				l.methodInfo = new MethodInfo(c, values[idx+1], values[idx+2], values[idx+3], Integer.parseInt(values[idx+4]));
			} else {
				l.methodInfo = new MethodInfo(classId, values[idx+1], values[idx+2], values[idx+3], Integer.parseInt(values[idx+4]));
			}
			idx += 5;
			l.sourceFileName = values[idx++];
			l.line = Integer.parseInt(values[idx++]);
			l.instructionIndex = Integer.parseInt(values[idx++]);
			l.label = values[idx++];
			
		} else {
			assert false: "unknown format";
		}
		
		String methodKey = l.methodInfo.toString();
		if (methodIds.containsKey(methodKey)) {
			int methodId = methodIds.get(methodKey);
			l.methodId = methodId;
		} else {
			int nextId = methodIds.size();
			methodIds.put(methodKey, nextId);
			l.methodId = nextId;
		}
		locations.add(l);
		assert locations.size() == l.locationId + 1;
	}
	
	private static class LocationId {
		private long locationId;
		private MethodInfo methodInfo;
		private String sourceFileName;
		private int line;
		private int instructionIndex;
		private String label;
		private int methodId; 
	}
	
	/**
	 * @return the maximum valid location ID.
	 */
	public long getMaxId() {
		return locations.size()-1;
	}
	
	/**
	 * This value is valid during a single execution.
	 * @param locationId
	 * @return
	 */
	public int getMethodId(long locationId) {
		assert locationId <= Integer.MAX_VALUE;
		return locations.get((int)locationId).methodId;
	}
	
	public MethodInfo getMethodInfo(long locationId) {
		assert locationId <= Integer.MAX_VALUE;
		LocationId l = locations.get((int)locationId);
		return l.methodInfo;
	}
	
	/**
	 * Return an instruction index. 
	 * This is valid only for events related to method invocation, field access and execution labels.
	 * The index can be used for an argument of org.objectweb.asm.tree.InsnList#get(int).
	 * @param locationId
	 * @return
	 */
	public int getInstructionIndex(long locationId) {
		assert locationId <= Integer.MAX_VALUE;
		return locations.get((int)locationId).instructionIndex;
	}
	
	public String getLabel(long locationId) {
		assert locationId <= Integer.MAX_VALUE;
		LocationId l = locations.get((int)locationId);
		return l.label;
	}
	
	private File[] getLocationFiles(File dir) {
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(WeavingInfo.LOCATION_ID_PREFIX) && 
							name.endsWith(WeavingInfo.LOCATION_ID_SUFFIX);
			}
		});
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return files;
	}

}
