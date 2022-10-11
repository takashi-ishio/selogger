package selogger.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import selogger.weaver.ClassInfo;
import selogger.weaver.DataInfo;
import selogger.weaver.MethodInfo;
import selogger.weaver.Weaver;

/**
 * This class is to access class/method/data ID files created by the weaver.
 */
public class DataIdMap {

	private ArrayList<ClassInfo> classes;
	private ArrayList<MethodInfo> methods;
	private ArrayList<DataInfo> dataIds;
	private ObjectTypeMap objects;

	/**
	 * Create an instance by loading files from the specified directory.
	 * @param dir is a directory including the weaver result files.
	 * @throws IOException
	 */
	public DataIdMap(File dir) throws IOException {
		classes = new ArrayList<>(1024);
		methods = new ArrayList<>(1024 * 1024);
		dataIds = new ArrayList<>(4 * 1024 * 1024);
		loadClassEntryFile(dir);
		loadMethodEntryFile(dir);
		loadDataIdEntryFile(dir);
		
		objects = new ObjectTypeMap(dir);
		
	}

	/**
	 * Get the information of a class corresponding to a given classId.
	 */
	public ClassInfo getClassEntry(int classId) {
		return classes.get(classId);
	}

	/**
	 * Get the information of a method corresponding to a given classId.
	 */
	public MethodInfo getMethod(int methodId) {
		return methods.get(methodId);
	}

	/**
	 * Get the information of a data ID corresponding to a given classId.
	 */
	public DataInfo getDataId(int dataId) {
		return dataIds.get(dataId);
	}
	
	/**
	 * Get the object type of an object corresponding to a given object Id.
	 */
	public String getObjectType(long objectId) {
		return objects.getObjectTypeName(objectId);
	}
	
	/**
	 * Load ClassInfo objects from a file in a specified directory.
	 * @param dir specifies a directory.
	 */
	private void loadClassEntryFile(File dir) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(dir, Weaver.CLASS_ID_FILE)));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			// Skip header
			if (classes.size() == 0 && line.equals(ClassInfo.getColumnNames())) continue; 
			// Read content
			classes.add(ClassInfo.parse(line));
		}
		reader.close();

		for (int i=0; i<classes.size(); i++) {
			assert classes.get(i).getClassId() == i: "Index must be consistent with Class ID.";
		}
	}
	
	/**
	 * Load MethodInfo objects from a file in a specified directory.
	 * @param dir specifies a directory.
	 */
	private void loadMethodEntryFile(File dir) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(dir, Weaver.METHOD_ID_FILE)));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			// Skip header
			if (methods.size() == 0 && line.equals(MethodInfo.getColumnNames())) continue; 
			// Read content
			methods.add(MethodInfo.parse(line));
		}
		reader.close();

		for (int i=0; i<methods.size(); i++) {
			MethodInfo m = methods.get(i);
			assert m.getMethodId() == i: "Index must be consistent with Class ID.";
			assert m.getClassName().equals(classes.get(m.getClassId()).getClassName()): "MethodEntry must be consistent with ClassEntry";
		}
	}

	/**
	 * Load DataInfo objects from a file in a specified directory.
	 * @param dir specifies a directory.
	 */
	private void loadDataIdEntryFile(File dir) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(dir, Weaver.DATA_ID_FILE)));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			// Skip header
			if (dataIds.size() == 0 && line.equals(DataInfo.getColumnNames())) continue; 
			// Read content
			dataIds.add(DataInfo.parse(line));
		}
		reader.close();
	}
	
}
