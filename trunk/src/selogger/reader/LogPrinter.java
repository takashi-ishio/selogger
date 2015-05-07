package selogger.reader;

import java.io.File;
import java.io.IOException;

/**
 * Print events to stdout in a textual format.
 * @author ishio
 */
public class LogPrinter {

	private static final String OPTION_FROM = "-from=";
	private static final String OPTION_NUM = "-num=";
	private static final String OPTION_LOG = "-dir=";
	private static final String OPTION_LOCATION = "-locationdir=";
	
	public static void main(String[] args) {
		long from = 0;
		long to = Long.MAX_VALUE;
		String logDir = ".";
		String locationDir = null;
		for (String s: args) {
			if (s.startsWith(OPTION_LOG)) {
				logDir = s.substring(OPTION_LOG.length());
			} else if (s.startsWith(OPTION_FROM)) {
				try {
					from = Long.parseLong(s.substring(OPTION_FROM.length()));
				} catch (NumberFormatException e) {
					from = 0;
				}
			} else if (s.startsWith(OPTION_NUM)) {
				try {
					to = from + Long.parseLong(s.substring(OPTION_NUM.length()));
				} catch (NumberFormatException e) {
					to = Long.MAX_VALUE;
				}
			} else if (s.startsWith(OPTION_LOCATION)) {
				locationDir = s.substring(OPTION_LOCATION.length());
			}
		}
		
		try {
			LocationIdMap locations = null;
			if (locationDir != null) {
				locations = new LocationIdMap(new File(locationDir));
			}

			LogDirectory dir = new LogDirectory(new File(logDir));
			EventReader reader = dir.getReader();
			if (from > 0) {
				reader.seek(from);
			}
			for (Event event = reader.readEvent(); event != null && event.getEventId() < to; event = reader.readEvent()) {
				if (locations == null) {
					System.out.println(event.toString());
				} else {
					MethodInfo m = locations.getMethodInfo(event.getLocationId());
					int line = locations.getLineNumber(event.getLocationId());
					System.out.println(m.toString() + "," + line + "," + event.toString());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
