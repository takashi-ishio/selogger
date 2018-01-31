package selogger.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
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
	
	private static final Class<?>[] BASIC_TYPE_CLASS = {
		void.class, boolean.class, byte.class, char.class, double.class,
		float.class, int.class, long.class, short.class, Object.class, String.class
	};
	
	private int nextId;
	private HashMap<Class<?>, String> classToIdMap;
	private ArrayList<String> typeRecords;
	private static final String SEPARATOR = ",";
	
	public TypeIdMap() {
		classToIdMap = new HashMap<>(65536);
		typeRecords = new ArrayList<>(65536);
		for (int i=0; i<BASIC_TYPE_CLASS.length; ++i) {
			String id = createTypeRecord(BASIC_TYPE_CLASS[i]);
			assert id.equals(Integer.toString(i));
		}
	}
	
	private String createTypeRecord(Class<?> type) {
		String superClass = getTypeIdString(type.getSuperclass());
		String componentType = getTypeIdString(type.getComponentType());
		String classLocation = getClassLocation(type);
		
		String id = Integer.toString(nextId++);
		classToIdMap.put(type, id);
		StringBuilder record = new StringBuilder(512);
		record.append(id);
		record.append(SEPARATOR);
		record.append(getTypeNameFromClass(type));
		record.append(SEPARATOR);
		record.append(classLocation);
		record.append(SEPARATOR);
		record.append(superClass);
		record.append(SEPARATOR);
		record.append(componentType);
		typeRecords.add(record.toString());
		return id;
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
			return createTypeRecord(type);
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
	
	/**
	 * Obtain a string where a class is loaded from.
	 * The original version is found at http://stackoverflow.com/questions/227486/find-where-java-class-is-loaded-from/19494116#19494116
	 * getCanonicalName() is replaced with getTypeNmae() in order to return the correct result for inner classes.
	 */
	private String getClassLocation(Class<?> c) {
		ClassLoader loader = c.getClassLoader();
		if ( loader == null ) {
			// Try the bootstrap class loader - obtained from the ultimate parent of the System Class Loader.
			loader = ClassLoader.getSystemClassLoader();
			while ( loader != null && loader.getParent() != null ) {
				loader = loader.getParent();
			}
		}
		if (loader != null) {
			String name = c.getTypeName();
			if (name != null) {
				URL resource = loader.getResource(name.replace(".", "/") + ".class");
				if ( resource != null ) {
					return resource.toString();
				}
			}
		}
		return "";
	}
	
	public void save(File f) {
		try {
			FileWriter fileWriter = new FileWriter(f);
			PrintWriter writer = new PrintWriter(fileWriter);
			for (int i=0; i<typeRecords.size(); ++i) {
				writer.println(typeRecords.get(i));
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
