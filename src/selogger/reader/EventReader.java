package selogger.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import selogger.EventType;
import selogger.logging.io.BinaryStreamLogger;

/**
 * This class is to read a sequence of events from .slg files.
 */
public class EventReader {
	
	private static int bufferSize = BinaryStreamLogger.BYTES_PER_EVENT * BinaryStreamLogger.MAX_EVENTS_PER_FILE;

	private File[] logFiles;
	protected ByteBuffer buffer;
	private int fileIndex;

	protected long nextEventId;
	protected ObjectTypeMap objectTypeMap;
	protected boolean processParams;
	private DataIdMap dataIdMap;


	/**
	 * Events created for some reasons but not yet returned to a client.
	 */
	private LinkedList<Event> unprocessed;
	

	/**
	 * Create an instance for reading files from a specified directory.
	 * @param dir is a directory containing *.slg files.
	 * @param dataIdMap is a DataIdMap to analyze the *.slg files.
	 */
	public EventReader(File dir, DataIdMap dataIdMap) {
		this.logFiles =  SequentialFileList.getSortedList(dir, BinaryStreamLogger.LOG_PREFIX, BinaryStreamLogger.LOG_SUFFIX);
		this.dataIdMap = dataIdMap;
		this.buffer = ByteBuffer.allocate(bufferSize);
		this.fileIndex = 0;
		this.unprocessed = new LinkedList<>();
		load();
	}
	
	
	/**
	 * Associate a entry/call event with its parameter events.
	 * This option may miss some parameter events 
	 * if events are not sequential because of multi-threading.   
	 * @param processParams If true, this reader automatically links parameter events. 
	 */
	public void setProcessParams(boolean processParams) {
		this.processParams = processParams;
	}
	
	/**
	 * Load a file from a file.
	 * @return true if a file is successfully loaded.
	 * False indicates that no more files exist or an error occurred.  
	 */
	protected boolean load() {
		// This method fails if no more files 
		if (fileIndex >= logFiles.length) {
			// Discard the data from the buffer
			buffer.position(0);
			buffer.limit(0);
			return false;
		}
		try {
			// Load a file content to the buffer
			buffer.position(0);
			buffer.limit(buffer.capacity());
			FileInputStream stream = new FileInputStream(logFiles[fileIndex]);
			stream.getChannel().read(buffer);
			stream.close();

			// Make it accessible for nextEvent method
			buffer.flip();
			fileIndex++;
			return true;
		} catch (IOException e) {
			// Discard the data from the buffer
			buffer.position(0);
			buffer.limit(0);
			return false;
		}
	}
	
	
	/**
	 * Obtain the next event on the same thread.
	 * Events skipped by the method are added to an internal buffer.
	 * @param e specifies the base event.  The method returns the next event on the same thread.
	 * @return an event.  The method may return null for EOF.
	 * @deprecated This method may load all the remaining events if e is the last event of a thread.  
	 */
	public Event nextThreadEvent(Event e) {
		if (unprocessed.size() > 0 && e.getEventId() < unprocessed.getLast().getEventId()) {
			for (Iterator<Event> it = unprocessed.iterator(); it.hasNext(); ) {
				Event u = it.next();
				if (u.getEventId() < e.getEventId()) continue;
				if (e.getThreadId() == u.getThreadId()) {
					it.remove();
					if (processParams) {
						readSubevents(u);
					}
					return u;
				}
			}
		}
		Event u = readEventFromBuffer();
		while (u != null && u.getThreadId() != e.getThreadId()) {
			unprocessed.offer(u);
			u = readEventFromBuffer();
		}
		if (u != null && processParams) readSubevents(u);
		return u;
	}
	
	
	/**
	 * Read the next event from a stream of events
	 * @return an event if exists.  This method returns null at the end of the files.
	 */
	public Event nextEvent() {
		Event e = (unprocessed.size() > 0) ? unprocessed.removeFirst() : readEventFromBuffer(); 
		if (e != null && processParams) readSubevents(e);
		return e;
	}
	
	/**
	 * Read parameter events and link them to the main event.
	 * @param main is the event that can have parameters 
	 */
	protected void readSubevents(Event main) {
		if (main == null) return;  // end of event stream
		if (main.getParams() != null) return; // already processed
		
		EventType sub;
		switch (main.getEventType()) {
		case METHOD_ENTRY:
			sub = EventType.METHOD_PARAM;
			break;
		case CALL:
			sub = EventType.CALL_PARAM;
			break;
		case INVOKE_DYNAMIC:
			sub = EventType.INVOKE_DYNAMIC_PARAM;
			break;
		default:
			// the event has no params
			return;
		}

		// Load successive events until parameter events are found
		// Parameter events may not be consecutive due to multi-threading
		int paramCount = main.getParamCount();
		Event[] params = new Event[paramCount];
		main.setParams(params);
		int count = 0;
		Event candidate = main;
		while (count < paramCount) {
			candidate = nextEvent();
			if (candidate == null) { // end of trace
				break;
			} else if (candidate.getThreadId() == main.getThreadId() && 
					candidate.getEventType() == sub) {
				params[count++] = candidate;
			} else { // if params are not recorded 
				unprocessed.add(candidate);
				break;
			}
		}
	}

	/**
	 * Read an event from the internal byte buffer.
	 * If the buffer is empty, this method loads the next file.
	 * @return a new event object.  
	 * Null is returned if the method reached the end of files.
	 */
	protected Event readEventFromBuffer() {
		// try to read the next event from a stream.
		while (buffer != null && buffer.remaining() == 0) {
			boolean result = load();
			if (!result) return null;
		}
		if (buffer == null) return null; // end-of-streams

		int dataId = buffer.getInt();
		int threadId = buffer.getInt();
		long value = buffer.getLong();
		return new Event(nextEventId++, dataId, threadId, value, dataIdMap);
	}


	/**
	 * Move to a particular event.
	 * The specified eventId is obtained by the next readEvent call.
	 * @param eventId specifies the event location.
	 */
	public void seek(long eventId) {
		if (eventId == nextEventId) return;
		if ((eventId / BinaryStreamLogger.MAX_EVENTS_PER_FILE) != fileIndex-1) { // != on memory file
			fileIndex = (int)(eventId / BinaryStreamLogger.MAX_EVENTS_PER_FILE);
			nextEventId = fileIndex * BinaryStreamLogger.MAX_EVENTS_PER_FILE;
			boolean success = load(); // load a file and fileIndex++
			if (!success) return;
		}
		int pos = (int)(BinaryStreamLogger.BYTES_PER_EVENT * (eventId % BinaryStreamLogger.MAX_EVENTS_PER_FILE));
		buffer.position(pos);
		nextEventId = eventId;
	}

	/**
	 * Put an event object back to the reader.
	 * @param e is an event to be put back.
	 */
	public void cancelRead(Event e) {
		// Search an appropriate event location using the event ID.
		for (ListIterator<Event> it = unprocessed.listIterator(); it.hasNext(); ) {
			Event u = it.next();
			if (u.getEventId() > e.getEventId()) {
				it.previous();
				it.add(e);
				return;
			}
		}
		unprocessed.addLast(e);
	}
	

}
