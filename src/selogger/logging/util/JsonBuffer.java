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
	
	/**
	 * Initialize a buffer
	 */
	public JsonBuffer() {
		buf = new StringBuilder(8192);
	}
	
	/**
	 * Start a JSON object.
	 * After writing fields, writeEndObject is needed.
	 */
	public void writeStartObject() {
		if (needSeparator) buf.append(",");
		buf.append("{");
		needSeparator = false;
	}
	
	/**
	 * End a JSON object.
	 */
	public void writeEndObject() {
		buf.append("}");
		needSeparator = true;
	}
	
	/**
	 * Write a field whose value is a JSON object.
	 * After writing fields, writeEndObject is needed.
	 * @param field specifies a field name.
	 */
	public void writeObjectFieldStart(String field) {
		if (needSeparator) buf.append(",");
		buf.append("\"");
		buf.append(field);
		buf.append("\":{");
		needSeparator = false;
	}
	
	/**
	 * Write a string that does not need quotation
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
	
	/**
	 * Write a field name and it value
	 * @param key specifies a field name
	 * @param value 
	 */
	public void writeNumberField(String key, long value) {
		if (needSeparator) buf.append(",");
		buf.append("\"");
		buf.append(key);
		buf.append("\":");
		buf.append(value);
		needSeparator = true;
	}
	
	/**
	 * Write a field name whose value is an array.
	 * After writing values, writeEndArray is needed.
	 * @param field specifeis a field name.
	 */
	public void writeArrayFieldStart(String field) {
		if (needSeparator) buf.append(",");
		buf.append("\"");
		buf.append(field);
		buf.append("\":[");
		needSeparator = false;
	}
	
	/**
	 * Write an end of array.
	 */
	public void writeEndArray() {
		buf.append("]");
		needSeparator = true;
	}
	
	/**
	 * Write a numeric value
	 */
	public void writeNumber(long value) {
		if (needSeparator) buf.append(",");
		buf.append(value);
		needSeparator = true;
	}
	
	/**
	 * Write a float value
	 */
	public void writeNumber(float value) {
		if (needSeparator) buf.append(",");
		buf.append(value);
		needSeparator = true;
	}
	
	/**
	 * Write a double value
	 */
	public void writeNumber(double value) {
		if (needSeparator) buf.append(",");
		buf.append(value);
		needSeparator = true;
	}

	/**
	 * Write a "null"
	 */
	public void writeNull() {
		if (needSeparator) buf.append(",");
		buf.append("null");
		needSeparator = true;
	}
	
	/**
	 * Write a boolean value
	 */
	public void writeBoolean(boolean value) {
		if (needSeparator) buf.append(",");
		buf.append(value);
		needSeparator = true;
	}

	/**
	 * Write a string value in the JSON format.
	 */
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

	/**
	 * @return a JSON fragment in the buffer. 
	 */
	@Override
	public String toString() {
		return buf.toString();
	}
	
	/**
	 * An interface to quickly write integer attributes
	 */
	@Override
	public void process(String key, int value) {
		writeNumberField(key, value);
	}

	/**
	 * An interface to quickly write String attributes
	 */
	@Override
	public void process(String key, String value) {
		writeStringField(key, value);
	}


}
