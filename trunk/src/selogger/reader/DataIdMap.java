package selogger.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;

import selogger.logging.io.EventStreamLogger;
import selogger.weaver.ClassInfo;
import selogger.weaver.DataInfo;
import selogger.weaver.MethodInfo;
import selogger.weaver.Weaver;


public class DataIdMap {

	private ArrayList<ClassInfo> classes;
	private ArrayList<MethodInfo> methods;
	private ArrayList<DataInfo> dataIds;
	private ObjectTypeMap objects;
	private int threadCount;
	
	public DataIdMap(File dir) throws IOException {
		classes = new ArrayList<>(1024);
		methods = new ArrayList<>(1024 * 1024);
		dataIds = new ArrayList<>(4 * 1024 * 1024);
		loadClassEntryFile(dir);
		loadMethodEntryFile(dir);
		loadDataIdEntryFile(dir);
		
		objects = new ObjectTypeMap(dir);
		
		loadThreadCount(dir);
	}

	public ClassInfo getClassEntry(int classId) {
		return classes.get(classId);
	}

	public MethodInfo getMethod(int methodId) {
		return methods.get(methodId);
	}

	public DataInfo getDataId(int dataId) {
		return dataIds.get(dataId);
	}
	
	public String getObjectType(long objectId) {
		return objects.getObjectTypeName(objectId);
	}
	
	public int getThreadCount() {
		return threadCount;
	}
	
	private void loadClassEntryFile(File dir) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(dir, Weaver.CLASS_ID_FILE)));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			classes.add(ClassInfo.parse(line));
		}
		reader.close();

		for (int i=0; i<classes.size(); i++) {
			assert classes.get(i).getClassId() == i: "Index must be consistent with Class ID.";
		}
	}
	
	private void loadMethodEntryFile(File dir) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(dir, Weaver.METHOD_ID_FILE)));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			methods.add(MethodInfo.parse(line));
		}
		reader.close();

		for (int i=0; i<methods.size(); i++) {
			MethodInfo m = methods.get(i);
			assert m.getMethodId() == i: "Index must be consistent with Class ID.";
			assert m.getClassName().equals(classes.get(m.getClassId()).getClassName()): "MethodEntry must be consistent with ClassEntry";
		}
	}

	private void loadDataIdEntryFile(File dir) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(dir, Weaver.DATA_ID_FILE)));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			dataIds.add(DataInfo.parse(line));
		}
		reader.close();
	}
	
	private void loadThreadCount(File dir) {
		try (LineNumberReader reader = new LineNumberReader(new FileReader(new File(dir, EventStreamLogger.FILENAME_THREADID)))) {
			threadCount = Integer.parseInt(reader.readLine());
			reader.close();
		} catch (IOException e) {
			threadCount = 0;
		} catch (NumberFormatException e) {
			threadCount = 0;
		}
	}
	

}
