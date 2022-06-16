package selogger.logging.util;

import com.fasterxml.jackson.core.io.JsonStringEncoder;

import selogger.weaver.method.InstructionAttributes.AttrProc;

/**
 * This class is to generate a JSON fragment without 
 * an overhead of Jackson's JsonGenerator class.
 */
public class JsonBuffer implements AttrProc {

	private StringBuilder buf;
	private boolean needSeparator;
	
	public JsonBuffer() {
		buf = new StringBuilder(8192);
	}
	
	public void writeStartObject() {
		if (needSeparator) buf.append(",");
		buf.append("{");
		needSeparator = false;
	}
	
	public void writeEndObject() {
		buf.append("}");
		needSeparator = true;
	}
	
	public void writeObjectFieldStart(String field) {
		if (needSeparator) buf.append(",");
		buf.append("\"");
		buf.append(field);
		buf.append("\":{");
		needSeparator = false;
	}
	
	/**
	 * Write a string that does not need qutoation
	 * @param key
	 * @param value
	 */
	public void writeStringField(String key, String value) {
		if (needSeparator) buf.append(",");
		buf.append("\"");
		buf.append(key);
		buf.append("\":\"");
		buf.append(value);
		buf.append("\"");
		needSeparator = true;
	}

	/**
	 * Write a string that may include control code
	 * @param key
	 * @param value
	 */
	public void writeEscapedStringField(String key, String value) {
		if (needSeparator) buf.append(",");
		buf.append("\"");
		buf.append(key);
		buf.append("\":\"");
		JsonStringEncoder.getInstance().quoteAsString(value, buf);
		buf.append("\"");
		needSeparator = true;
	}
	
	public void writeNumberField(String key, long value) {
		if (needSeparator) buf.append(",");
		buf.append("\"");
		buf.append(key);
		buf.append("\":");
		buf.append(value);
		needSeparator = true;
	}
	
	public void writeArrayFieldStart(String field) {
		if (needSeparator) buf.append(",");
		buf.append("\"");
		buf.append(field);
		buf.append("\":[");
		needSeparator = false;
	}
	
	public void writeEndArray() {
		buf.append("]");
		needSeparator = true;
	}
	
	public void writeNumber(long value) {
		if (needSeparator) buf.append(",");
		buf.append(value);
		needSeparator = true;
	}
	
	public void writeNumber(float value) {
		if (needSeparator) buf.append(",");
		buf.append(value);
		needSeparator = true;
	}
	
	public void writeNumber(double value) {
		if (needSeparator) buf.append(",");
		buf.append(value);
		needSeparator = true;
	}

	public void writeNull() {
		if (needSeparator) buf.append(",");
		buf.append("null");
		needSeparator = true;
	}
	
	public void writeBoolean(boolean value) {
		if (needSeparator) buf.append(",");
		buf.append(value);
		needSeparator = true;
	}

	public void writeString(String value) {
		if (needSeparator) buf.append(",");
		if (value != null) {
			buf.append("\"");
			JsonStringEncoder.getInstance().quoteAsString(value, buf);
			buf.append("\"");
		} else {
			buf.append("null");
		}
		needSeparator = true;
	}

	
	@Override
	public String toString() {
		return buf.toString();
	}
	
	@Override
	public void process(String key, int value) {
		writeNumberField(key, value);
	}

	@Override
	public void process(String key, String value) {
		writeStringField(key, value);
	}


}
