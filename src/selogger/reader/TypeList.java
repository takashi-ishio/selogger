package selogger.reader;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;

import selogger.logging.TypeIdMap;

/**
 * This is a class to read Type IDs created by TypeIdMap.
 * @author ishio
 */
public class TypeList {

	private ArrayList<TypeRecord> records;
	
	public static final int UNAVAILABLE = -1;

	/**
	 * Load an existing type list from a file. 
	 * @param load
	 */
	public TypeList(File load) throws IOException {
		records = new ArrayList<TypeRecord>(65536);
			
		FileReader reader = new FileReader(load);
		LineNumberReader lines = new LineNumberReader(reader);
		for (String line = lines.readLine(); line != null; line = lines.readLine()) {
			TypeRecord t = new TypeRecord(line); 
			if (records.size() != t.id) {
				lines.close();
				throw new IOException("The file " + load.getAbsolutePath() + " is not a type id file.");
			}
			records.add(t);
		}
		lines.close();

		assert getType(TypeIdMap.TYPEID_VOID).equals("void");
		assert getType(TypeIdMap.TYPEID_BOOLEAN).equals("boolean");
		assert getType(TypeIdMap.TYPEID_BYTE).equals("byte");
		assert getType(TypeIdMap.TYPEID_CHAR).equals("char");
		assert getType(TypeIdMap.TYPEID_DOUBLE).equals("double");
		assert getType(TypeIdMap.TYPEID_FLOAT).equals("float");
		assert getType(TypeIdMap.TYPEID_INT).equals("int");
		assert getType(TypeIdMap.TYPEID_LONG).equals("long");
		assert getType(TypeIdMap.TYPEID_SHORT).equals("short");
	}
	
	/**
	 * @return the number of types in the list.
	 */
	public int size() {
		return records.size();
	}
	
	/**
	 * @param id
	 * @return the type name corresponding to the specified ID. 
	 * This method may return null if a type id has not been successfully recorded.
	 * The same string may be returned for different IDs if two class loaders use classes whose names are the same.
	 */
	public String getType(int id) {
		if (id == TypeIdMap.TYPEID_NULL) {
			return "null";
		} else if (id >= records.size()) {
			// invalid type id
			return null; 
		} else {
			return records.get(id).name;
		}
	}
	
	/**
	 * @param id
	 * @return a URL which the class is loaded from.
	 * You can use the URL with URLClassLoader to load the class.
	 * Note that this method returns a URL followed by a path inside a jar, 
	 * while URLClassLoader accepts a simple jar URL ("jar:...!/") without its inner path.
	 */
	public String getClassLocation(int id) {
		if (id == TypeIdMap.TYPEID_NULL) {
			return null;
		} else if (id >= records.size()) {
			return null; 
		} else {
			return records.get(id).url;
		}
	}

	public int getParentClass(int id) {
		if (id == TypeIdMap.TYPEID_NULL) {
			return UNAVAILABLE;
		} else if (id >= records.size()) {
			return UNAVAILABLE;
		} else {
			return records.get(id).parentTypeId;
		}
	}

	/**
	 * @param id
	 * @return a component type ID indicating the type of an element of an array.
	 */
	public int getComponentClass(int id) {
		if (id == TypeIdMap.TYPEID_NULL) {
			return UNAVAILABLE;
		} else if (id >= records.size()) {
			return UNAVAILABLE;
		} else {
			return records.get(id).componentTypeId;
		}
	}

	private static class TypeRecord {
		private int id;
		private String name;
		private String url;
		private int parentTypeId;
		private int componentTypeId;
		
		public TypeRecord(String line) {
			String[] tokens = line.split(",");
			if (tokens.length == 5) {
				id = Integer.parseInt(tokens[0]);
				name = tokens[1];
				url = (tokens[2].equals("")) ? null: tokens[2];
				parentTypeId = Integer.parseInt(tokens[3]);
				componentTypeId = Integer.parseInt(tokens[4]);
			} else if (tokens.length == 2) { // an old format
				id = Integer.parseInt(tokens[0]);
				name = tokens[1];
				url = null;
				parentTypeId = UNAVAILABLE;
				componentTypeId = UNAVAILABLE;
			}
		}
	}

}
