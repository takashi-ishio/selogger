package selogger.reader;

import java.io.File;
import java.io.IOException;

import selogger.weaver.DataInfo;
import selogger.weaver.MethodInfo;

/**
 * Print events to stdout in a textual format.
 * @author ishio
 */
public class LogPrinter {

	private static final String OPTION_FROM = "-from=";
	private static final String OPTION_NUM = "-num=";
	private static final String OPTION_THREAD = "-thread=";  
	private static final String OPTION_PROCESSPARAMS = "-processparams";
	
	public static void main(String[] args) {
		long from = 0;
		long to = Long.MAX_VALUE;
		boolean processParams = false;
		int[] threads = null;
		String logDir = ".";
		for (String s: args) {
			if (s.startsWith(OPTION_FROM)) {
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
			} else if (s.startsWith(OPTION_THREAD)) {
				String[] th = s.substring(OPTION_THREAD.length()).split(",");
				threads = new int[th.length];
				for (int i=0; i<th.length; ++i) {
					threads[i] = Integer.parseInt(th[i]);
				}
			} else if (s.equals(OPTION_PROCESSPARAMS)) {
				processParams = true;
			} else {
				logDir = s;
			}
		}
		
		try {
			File dir = new File(logDir);
			DataIdMap map = new DataIdMap(dir);
			EventReader reader = new EventReader(dir, map);

			if (from > 0) {
				reader.seek(from);
			}
			reader.setProcessParams(processParams);
			for (Event event = reader.nextEvent(); event != null && event.getEventId() < to; event = reader.nextEvent()) {

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
				System.out.print(event.toString());
				System.out.print(",");
				MethodInfo m = event.getMethodEntry();
				System.out.print(m.getClassName() + ":" + m.getMethodName() + ",");
				DataInfo d = event.getDataIdEntry();
				System.out.print(m.getSourceFileName() + ":" + d.getLine() + ":" + d.getInstructionIndex());
				System.out.println();
				
				// Output parameters associated with the event
				Event[] params = event.getParams();
				if (params != null) {
					for (Event p: params) {
						if (p != null) {
							System.out.println("  param[" + p.getParamIndex() + "] " + p.toString());
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Usage: LogPrinter log-directory [-from=N] [-num=M] [-locationdir=LocationFileDir] [-thread=ThreadList] [-processparams]");
		}
	}
	
}
