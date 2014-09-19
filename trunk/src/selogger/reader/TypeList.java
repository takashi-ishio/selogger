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

	private ArrayList<String> typenames;
	/**
	 * Load an existing type list from a file. 
	 * @param load
	 */
	public TypeList(File load) throws IOException {
		typenames = new ArrayList<String>(65536);
			
		FileReader reader = new FileReader(load);
		LineNumberReader lines = new LineNumberReader(reader);
		for (String line = lines.readLine(); line != null; line = lines.readLine()) {
			String[] tokens = line.split(",");
			if (tokens.length == 2) {
				try {
					int id = Integer.parseInt(tokens[0]);
					String typename = tokens[1];
					typenames.add(typename);
					if (typenames.size() != id+1) { 
						lines.close();
						throw new IOException("The file " + load.getAbsolutePath() + " is not a type id file.");
					}
				} catch (NumberFormatException e) {
					// skip the line
				}
			}
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
	 * 
	 * @param id
	 * @return the type name corresponding to the specified ID. 
	 * This method may return null if a type id has not been successfully recorded.
	 * The same string may be returned for different IDs if two class loaders use classes whose names are the same.
	 */
	public String getType(int id) {
		if (id == TypeIdMap.TYPEID_NULL) {
			return "null";
		} else if (id >= typenames.size()) {
			// invalid type id
			return null; 
		} else {
			return typenames.get(id);
		}
	}
	

}
