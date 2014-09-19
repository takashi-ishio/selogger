package selogger.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;


public class TypeIdMap {
	
	public static final int TYPEID_NULL = -1;
	public static final int TYPEID_VOID = 0;
	public static final int TYPEID_BOOLEAN = 1;
	public static final int TYPEID_BYTE = 2;
	public static final int TYPEID_CHAR = 3;
	public static final int TYPEID_DOUBLE = 4;
	public static final int TYPEID_FLOAT = 5;
	public static final int TYPEID_INT = 6;
	public static final int TYPEID_LONG = 7;
	public static final int TYPEID_SHORT = 8;
	public static final int TYPEID_OBJECT = 9;
	
	private static final String[] BASIC_TYPES = { 
		"void", "boolean", "byte", "char", "double", 
		"float", "int", "long", "short", "java.lang.String" };
	private static final Class<?>[] BASIC_TYPE_CLASS = {
		void.class, boolean.class, byte.class, char.class, double.class,
		float.class, int.class, long.class, short.class, String.class
	};
	
	private int nextId;
	private HashMap<Class<?>, String> classToIdMap;
	private ArrayList<String> typenames;

	
	public TypeIdMap() {
		assert BASIC_TYPES.length == BASIC_TYPE_CLASS.length: "Coding error: different number of basic types are used in source code.";
		
		classToIdMap = new HashMap<>(65536);
		typenames = new ArrayList<String>(65536);
		for (int i=0; i<BASIC_TYPES.length; ++i) {
			int id = nextId++;
			classToIdMap.put(BASIC_TYPE_CLASS[i], Integer.toString(id));
			typenames.add(BASIC_TYPES[i]);
		}
	}

	/**
	 * Return a string representing a type ID number.
	 * This is to generate a type ID list file. 
	 */
	public String getTypeIdString(Class<?> type) {
		if (type == null) {
			return Integer.toString(TYPEID_NULL);
		} else {
			if (classToIdMap.containsKey(type)) { 
				return classToIdMap.get(type);
			}
			
			// assign an ID to each Class<?> even if two Class<?> have the same name
			String id = Integer.toString(nextId++);
			classToIdMap.put(type, id);
			typenames.add(getTypeNameFromClass(type));
			return id;
		}
	}
		
	private String getTypeNameFromClass(Class<?> type) {
		if (type.isArray()) {
			int count = 0;
			while (type.isArray()) {
				count++;
				type = type.getComponentType();
			}
			StringBuilder b = new StringBuilder(type.getName().length() + count * 2);
			b.append(type.getName());
			for (int i=0; i<count; ++i) {
				b.append("[]");
			}
			return b.toString();
		} else {
			return type.getName();
		}
	}
	
	public void save(File f) {
		try {
			FileWriter fileWriter = new FileWriter(f);
			PrintWriter writer = new PrintWriter(fileWriter);
			for (int i=0; i<typenames.size(); ++i) {
				writer.println(i + "," + typenames.get(i));
			}
			writer.close();
		} catch (IOException e) {
		}
	}
	
	public static boolean isPrimitiveTypeOrNull(final int typeId) {
		switch (typeId) {
		case TypeIdMap.TYPEID_VOID:
		case TypeIdMap.TYPEID_BYTE:
		case TypeIdMap.TYPEID_CHAR:
		case TypeIdMap.TYPEID_DOUBLE:
		case TypeIdMap.TYPEID_FLOAT:
		case TypeIdMap.TYPEID_INT:
		case TypeIdMap.TYPEID_LONG:
		case TypeIdMap.TYPEID_SHORT:
		case TypeIdMap.TYPEID_BOOLEAN:
		case TypeIdMap.TYPEID_NULL:
			return true;
		default:
			return false;
		}
	}

}
