package selogger.weaver.method;

import java.util.ArrayList;

import org.objectweb.asm.Type;

/**
 * Attributes of an instruction. 
 * This is provided through dataids.txt and/or recentdata.txt.
 */
public class InstructionAttributes {

	/**
	 * A flag to write a type descriptor with type name.
	 */
	private static boolean writeDescriptor = false; 
	
	/**
	 * NEWOBJECT (created type), INSTANCEOF (checked type), CALL PARAM (parameter type)
	 * MULTI_NEW_ARRAY (created type), CALL_RETURN (return value type),
	 * IINC (variable type), INVOKE_DYNAMIC_PARAM (parameter type),
	 * OBJECT_CONSTANT_LOAD (object type), FIELD (field type), 
	 * LOCAL variables (variable type),
	 * NEW_ARRAY and ANEWARRAY (element type),
	 * LABEL for catch block (exception type)
	 */
	public static final String ATTRIBUTE_TYPE_DESCRIPTOR = "desc";

	/**
	 * Human readable type name
	 */
	public static final String ATTRIBUTE_TYPE_NAME = "type";


	/**
	 * A callback interface to process a field.
	 */
	public static interface AttrProc {
		public void process(String key, String value);
		public void process(String key, int value);
	}
	
	/**
	 * An attribute is a pair of a key and a value.
	 * This interface hides a value type.
	 */
	private static interface Attribute {
		
		/**
		 * @return the field name.
		 */
		public String getKey();
		
		/**
		 * This method calls a given procedure and 
		 * passes the field name and value as parameters. 
		 * @param proc specifies the actual action.
		 */
		public void doAction(AttrProc proc);
	}

	/**
	 * An attribute whose value is a String.
	 */
	private static class StrAttr implements Attribute {

		private String key;
		private String value;
		
		public StrAttr(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String getKey() {
			return key;
		}
		
		@Override
		public void doAction(AttrProc proc) {
			proc.process(key, value);
		}
		
		@Override
		public String toString() {
			return key + "=" + value;
		}
	}

	/**
	 * An attribute whose value is an integer.
	 */
	private static class IntAttr implements Attribute {

		private String key;
		private int value;
		
		public IntAttr(String key, int value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public String getKey() {
			return key;
		}
		
		@Override
		public void doAction(AttrProc proc) {
			proc.process(key, value);
		}

		@Override
		public String toString() {
			return key + "=" + value;
		}
	}

	private ArrayList<Attribute> values;
	
	/**
	 * Create an empty attribute object.
	 */
	public InstructionAttributes() {
		values = new ArrayList<>(5);
	}
	
	/**
	 * Create a String attribute from given parameters. 
	 * @param key specifies a field name.
	 * @param value specifies its value.
	 * @return an attribute object.
	 */
	public static InstructionAttributes of(String key, String value) {
		InstructionAttributes attr = new InstructionAttributes();
		return attr.and(key, value);
	}

	/**
	 * Create an integer attribute from given parameters. 
	 * @param key specifies a field name.
	 * @param value specifies its value.
	 * @return an attribute object.
	 */
	public static InstructionAttributes of(String key, int value) {
		InstructionAttributes attr = new InstructionAttributes();
		return attr.and(key, value);
	}
	
	/**
	 * Create an attribute of a type name
	 * @param desc specifies a type descriptor.
	 * @return an attrbute object
	 */
	public static InstructionAttributes ofType(String desc) {
		InstructionAttributes attr = new InstructionAttributes();
		String typename = Type.getType(desc).getClassName();
		if (writeDescriptor) attr.and(ATTRIBUTE_TYPE_DESCRIPTOR, desc);
		return attr.and(ATTRIBUTE_TYPE_NAME, typename);
	}

	/**
	 * Add a String attribute to the object.
	 * @return the object itself for a method chain.
	 */
	public InstructionAttributes and(String key, String value) {
		values.add(new StrAttr(key, value));
		return this;
	}

	/**
	 * Add an integer attribute to the object.
	 * @return the object itself for a method chain.
	 */
	public InstructionAttributes and(String key, int value) {
		values.add(new IntAttr(key, value));
		return this;
	}
	
	/**
	 * Execute an action for this attributes.
	 * @param proc specifies an action.
	 */
	public void foreach(AttrProc proc) {
		for (Attribute v: values) {
			v.doAction(proc);
		}
	}
	
	/**
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getStringValue(String key, String defaultValue) {
		for (Attribute v: values) {
			if (v.getKey().equals(key)) {
				if (v instanceof StrAttr) {
					return ((StrAttr) v).value;
				} else if (v instanceof IntAttr) {
					return Integer.toString(((IntAttr)v).value);
				}
			}
		}
		return defaultValue;
	}
	
	/**
	 * This method is to test a specified field is included in this instance.
	 * @param key specifies a name.
	 * @param value specifies a value.  
	 * The value is also compared with integer attributes 
	 * (by translating an integer value to a String). 
	 * @return true if it is found.
	 */
	public boolean contains(String key, String value) {
		for (Attribute v: values) {
			if (v.getKey().equals(key)) {
				if (v instanceof StrAttr) {
					return value.equals(((StrAttr) v).value);
				} else if (v instanceof IntAttr) {
					return value.equals(Integer.toString(((IntAttr)v).value));
				}
			}
		}
		return false;
	}

	/**
	 * Create a string representation.
	 */
	public String toString() {
		StringBuilder b = new StringBuilder();
		boolean isFirst = true;
		for (Attribute v: values) {
			if (isFirst) {
				isFirst = false;
			} else {
				b.append(",");
			}
			b.append(v.toString());
		}
		return b.toString();
	}
	
	/**
	 * @return the number of attributes in the object
	 */
	public int getAttributeCount() {
		return values.size();
	}

}
