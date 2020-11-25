package selogger.reader;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import selogger.logging.io.EventStreamLogger;

/**
 * This class is to read Object-Type ID map created by ObjectIdFile class.
 */
public class ObjectTypeMap {

	private static final int LIST_PER_ITEMS = 128 * 1024 * 1024;
	
	//private TLongIntHashMap objectTypeMap; 
	private ArrayList<int[]> objectTypes;
	private TypeList typeList;
	long count = 0;
	
	/**
	 * Load files from a specified directory.
	 * @param logfileDir is the directory including object type files.
	 */
	public ObjectTypeMap(File logfileDir) {
		objectTypes = new ArrayList<>(1024);
		objectTypes.add(new int[LIST_PER_ITEMS]);
		register(0, -1); // no type information is available for null
		File[] filenames = SequentialFileList.getSortedList(logfileDir, "LOG$ObjectTypes", ".txt");
		try {
			for (File f: filenames) {
				BufferedReader reader = new BufferedReader(new FileReader(f));
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
			typeList = new TypeList(new File(logfileDir, EventStreamLogger.FILENAME_TYPEID)); 
		} catch (IOException e) {
		}
	}
	
	/**
	 * This method records the pair of object ID and type ID loaded from files.
	 * @param objId is an object ID.
	 * @param typeId is a type ID.
	 */
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
	 * @param specifies an object ID.
	 * @return type ID for the specified object.
	 */
	public int getObjectTypeId(long objectId) {
		int listIndex = (int)(objectId / LIST_PER_ITEMS);
		int index = (int)(objectId % LIST_PER_ITEMS);
		return objectTypes.get(listIndex)[index];
	}

	/**
	 * @param specifies an object ID.
	 * @return type name for the specified object.
	 */
	public String getObjectTypeName(long objectId) {
		int typeId = getObjectTypeId(objectId);
		return typeList.getType(typeId);
	}
	
	/**
	 * @param specifies a type ID.
	 * @return type name for the specified type ID.
	 */
	public String getTypeName(int typeId) {
		return typeList.getType(typeId);
	}
	
}
