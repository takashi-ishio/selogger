package selogger.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import selogger.EventType;
import selogger.logging.io.EventDataStream;
import selogger.logging.io.EventStreamLogger;


public class EventReader {
	
	private static int bufferSize = EventDataStream.BYTES_PER_EVENT * EventDataStream.MAX_EVENTS_PER_FILE;

	private File[] logFiles;
	protected ObjectTypeMap objectTypeMap;
	protected long nextEventId;
	protected boolean processParams;
	protected ByteBuffer buffer;
	private int fileIndex;
	private DataIdMap dataIdMap;
	private LinkedList<Event> unprocessed;
	

	public EventReader(File dir, DataIdMap dataIdMap) {
		this.logFiles =  SequentialFileList.getSortedList(dir, EventStreamLogger.LOG_PREFIX, EventStreamLogger.LOG_SUFFIX);
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
	 * @param processParams
	 */
	public void setProcessParams(boolean processParams) {
		this.processParams = processParams;
	}
	
	protected boolean load() {
		if (fileIndex >= logFiles.length) {
			buffer.clear();
			buffer.flip();
			return false;
		}
		try {
			buffer.clear();
			FileInputStream stream = new FileInputStream(logFiles[fileIndex]);
			stream.getChannel().read(buffer);
			stream.close();
			buffer.flip();
			fileIndex++;
			return true;
		} catch (IOException e) {
			buffer.clear();
			buffer.flip();
			return false;
		}
	}
	
	public void cancelRead(Event e) {
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
	
	
	public Event nextEvent() {
		Event e = (unprocessed.size() > 0) ? unprocessed.removeFirst() : readEventFromBuffer(); 
		if (e != null && processParams) readSubevents(e);
		return e;
	}
	
	protected void readSubevents(Event e) {
		if (e == null) return;  // end of event stream
		
		EventType sub;
		switch (e.getEventType()) {
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
			return;
		}

		// Load following parameter events
		int paramCount = e.getParamCount();
		Event[] params = new Event[paramCount];
		e.setParams(params);
		int count = 0;
		Event candidate = e;
		while (count < paramCount) {
			candidate = nextEvent();
			if (candidate == null) { // end of trace
				break;
			} else if (candidate.getThreadId() == e.getThreadId() && 
					candidate.getEventType() == sub) {
				params[count++] = candidate;
			} else { // if params are not recorded 
				unprocessed.add(candidate);
				break;
			}
		}
	}

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
	 * Move to a paritcular event.
	 * The specified eventId is obtained by the next readEvent call.
	 * @param eventId
	 */
	public void seek(long eventId) {
		if (eventId == nextEventId) return;
		if ((eventId / EventDataStream.MAX_EVENTS_PER_FILE) != fileIndex-1) { // != on memory file
			fileIndex = (int)(eventId / EventDataStream.MAX_EVENTS_PER_FILE);
			nextEventId = fileIndex * EventDataStream.MAX_EVENTS_PER_FILE;
			boolean success = load(); // load a file and fileIndex++
			if (!success) return;
		}
		int pos = (int)(EventDataStream.BYTES_PER_EVENT * (eventId % EventDataStream.MAX_EVENTS_PER_FILE));
		buffer.position(pos);
		nextEventId = eventId;
	}
	
}
