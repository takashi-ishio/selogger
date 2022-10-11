package selogger.weaver.method;

import java.util.ArrayList;

/**
 * Attributes of an instruction. 
 * This is provided through dataids.txt and/or recentdata.txt.
 */
public class InstructionAttributes {

	public static interface AttrProc {
		public void process(String key, String value);
		public void process(String key, int value);
	}
	
	private static interface Attribute {
		public String getKey();
		public void doAction(AttrProc proc);
	}

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
	
	public InstructionAttributes() {
		values = new ArrayList<>(5);
	}
	
	public static InstructionAttributes of(String key, String value) {
		InstructionAttributes attr = new InstructionAttributes();
		return attr.and(key, value);
	}

	public static InstructionAttributes of(String key, int value) {
		InstructionAttributes attr = new InstructionAttributes();
		return attr.and(key, value);
	}

	public InstructionAttributes and(String key, String value) {
		values.add(new StrAttr(key, value));
		return this;
	}

	public InstructionAttributes and(String key, int value) {
		values.add(new IntAttr(key, value));
		return this;
	}
	
	public void foreach(AttrProc proc) {
		for (Attribute v: values) {
			v.doAction(proc);
		}
	}
	
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
