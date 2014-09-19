package selogger.reader;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;

import selogger.logging.LogWriter;


public class ObjectTypeMap {

	private static final int LIST_PER_ITEMS = 128 * 1024 * 1024;
	
	//private TLongIntHashMap objectTypeMap; 
	private ArrayList<int[]> objectTypes;
	private TypeList typeList;
	long count = 0;
	
	public ObjectTypeMap(File logfileDir) {
		objectTypes = new ArrayList<>(1024);
		objectTypes.add(new int[LIST_PER_ITEMS]);
		register(0, -1); // no type information is available for null
		SequentialFileList filenames = new SequentialFileList(logfileDir, "LOG$ObjectTypes", ".txt");
		try {
			for (File f: filenames.getFiles()) {
				LineNumberReader reader = new LineNumberReader(new FileReader(f));
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					int idx = line.indexOf(',');
					if (idx >= 0) {
						String objIdString = line.substring(0, idx);
						String typeIdString = line.substring(idx+1);
						try {
							long objId = Long.parseLong(objIdString);
							int typeId = Integer.parseInt(typeIdString);
							register(objId, typeId);
						} catch (NumberFormatException e) {
							// ignore the line
						}
					}
				}
				reader.close();
			}
			typeList = new TypeList(new File(logfileDir, LogWriter.FILENAME_TYPEID)); 
		} catch (IOException e) {
		}
	}
	
	private void register(long objId, int typeId) {
		assert objId == count: "objId is not sequential. objId=" + Long.toString(objId) + " count=" + Long.toString(count);
		count++;
		int listIndex = (int)(objId / LIST_PER_ITEMS);
		int index = (int)(objId % LIST_PER_ITEMS);
		if (objectTypes.size() == listIndex) {
			objectTypes.add(new int[LIST_PER_ITEMS]);
		}
		objectTypes.get(listIndex)[index] = typeId;
	}
	
	/**
	 * Return type ID for a specified object.
	 * @return
	 */
	public int getObjectTypeId(long objectId) {
		int listIndex = (int)(objectId / LIST_PER_ITEMS);
		int index = (int)(objectId % LIST_PER_ITEMS);
		return objectTypes.get(listIndex)[index];
	}

	public String getObjectTypeName(long objectId) {
		int typeId = getObjectTypeId(objectId);
		return typeList.getType(typeId);
	}
	
	/**
	 * Return a type name for a specified type ID.
	 * @param typeId
	 * @return
	 */
	public String getTypeName(int typeId) {
		return typeList.getType(typeId);
	}
	
}
