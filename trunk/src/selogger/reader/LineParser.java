package selogger.reader;

import selogger.weaver.WeavingInfo;

public class LineParser {

	private String line;
	private int index;
	public LineParser(String line) {
		this.line = line;
	}
	
	public int readInt() {
		char c = line.charAt(index);
		int value = 0;
		while (c != WeavingInfo.SEPARATOR_CHAR) {
			value *= 10;
			value += Character.digit(c, 10);
			index++;
			c = line.charAt(index);
		}
		index++; // to dispose the separator
		return value;
	}
	
	public long readLong() {
		char c = line.charAt(index);
		long value = 0;
		while (c != WeavingInfo.SEPARATOR_CHAR) {
			value *= 10;
			value += Character.digit(c, 10);
			index++;
			c = line.charAt(index);
		}
		index++; // to dispose ','
		return value;
	}
	
	public String readString() {
		int next = line.indexOf(WeavingInfo.SEPARATOR_CHAR, index);
		if (next == -1) {
			String token = line.substring(index);
			index = line.length();
			return token;
		} else {
			String token = line.substring(index, next);
			index = next + 1;
			return token;
		}
	}

}
