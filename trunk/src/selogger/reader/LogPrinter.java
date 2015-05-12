package selogger.reader;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Print events to stdout in a textual format.
 * @author ishio
 */
public class LogPrinter {

	private static final String OPTION_FROM = "-from=";
	private static final String OPTION_NUM = "-num=";
	private static final String OPTION_LOG = "-dir=";
	private static final String OPTION_LOCATION = "-locationdir=";
	private static final String OPTION_THREAD = "-thread=";  
	private static final String OPTION_PROCESSPARAMS = "-processparams";
	
	public static void main(String[] args) {
		long from = 0;
		long to = Long.MAX_VALUE;
		boolean processParams = false;
		int[] threads = null;
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
			} else if (s.startsWith(OPTION_THREAD)) {
				String[] th = s.substring(OPTION_THREAD.length()).split(",");
				threads = new int[th.length];
				for (int i=0; i<th.length; ++i) {
					threads[i] = Integer.parseInt(th[i]);
				}
			} else if (s.equals(OPTION_PROCESSPARAMS)) {
				processParams = true;
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
			reader.setProcessParams(processParams);
			for (Event event = reader.readEvent(); event != null && event.getEventId() < to; event = reader.readEvent()) {

				// Check the thread of the event
				if (threads != null) {
					boolean found = false;
					for (int th: threads) {
						if (event.getThreadId() == th) {
							found = true;
							break;
						}
					}
					if (!found) continue;
				}
				
				// Output the event
				if (locations != null) {
					MethodInfo m = locations.getMethodInfo(event.getLocationId());
					int line = locations.getLineNumber(event.getLocationId());
					System.out.print(m.toString() + "," + line + ",");
				}
				System.out.println(event.toString());
				
				// Output parameters associated with the event
				List<Event> params = event.getParams();
				if (params != null) {
					for (Event p: params) {
						System.out.println("  param[" + p.getParamIndex() + "] " + p.toString());
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Usage: LogPrinter [-dir=LogDirectory] [-from=N] [-num=M] [-locationdir=LocationFileDir] [-thread=ThreadList] [-processparams]");
		}
	}
	
}
