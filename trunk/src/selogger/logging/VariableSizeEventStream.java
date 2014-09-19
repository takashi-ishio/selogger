package selogger.logging;

import selogger.Config;

/**
 * This class writes events in a compact format with variable record size.
 * Since each event is encoded in a compact size, 
 * the output size is 20 percent smaller than fixed record size, if no compression is applied.
 * @author ishio
 *
 */
public class VariableSizeEventStream extends BinaryFileListStream {

	public static final int BYTES_PER_EVENT = 48;

	public VariableSizeEventStream(SequentialFileName filenames, Config.OutputOption outputOption, int threads) {
		super(filenames, outputOption, threads, BYTES_PER_EVENT);
	}
	
	public void registerEventWithoutData(int eventType, long eventId, int threadId, long locationId) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}
	
	public void registerLong(int eventType, long eventId, int threadId, long locationId, long longData) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putLong(longData);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}
	
	public void registerLongInt(int eventType, long eventId, int threadId, long locationId, long longData, int intData) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putLong(longData);
		buffer.putInt(intData);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}
	
	// registerLongValue
	public void registerLongValue(int eventType, long eventId, int threadId, long locationId, long longData, double value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putLong(longData);
		buffer.putDouble(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerLongValue(int eventType, long eventId, int threadId, long locationId, long longData, float value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putLong(longData);
		buffer.putFloat(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerLongValue(int eventType, long eventId, int threadId, long locationId, long longData, int value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putLong(longData);
		buffer.putInt(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerLongValue(int eventType, long eventId, int threadId, long locationId, long longData, long value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putLong(longData);
		buffer.putLong(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	
	
	// registerIntValue-------------------------------
	public void registerIntValue(int eventType, long eventId, int threadId, long locationId, int intData, double value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putInt(intData);
		buffer.putDouble(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerIntValue(int eventType, long eventId, int threadId, long locationId, int intData, float value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putInt(intData);
		buffer.putFloat(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerIntValue(int eventType, long eventId, int threadId, long locationId, int intData, int value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putInt(intData);
		buffer.putInt(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerIntValue(int eventType, long eventId, int threadId, long locationId, int intData, long value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putInt(intData);
		buffer.putLong(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	
	// registerLongIntValue-----
	public void registerLongIntValue(int eventType, long eventId, int threadId, long locationId, long longData, int intData, double value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putLong(longData).putInt(intData);
		buffer.putDouble(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerLongIntValue(int eventType, long eventId, int threadId, long locationId, long longData, int intData, float value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putLong(longData).putInt(intData);
		buffer.putFloat(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerLongIntValue(int eventType, long eventId, int threadId, long locationId, long longData, int intData, int value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putLong(longData).putInt(intData);
		buffer.putInt(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerLongIntValue(int eventType, long eventId, int threadId, long locationId, long longData, int intData, long value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putLong(longData).putInt(intData);
		buffer.putLong(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}


	//---------
	public void registerValue(int eventType, long eventId, int threadId, long locationId, double value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putDouble(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerValue(int eventType, long eventId, int threadId, long locationId, float value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putFloat(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerValue(int eventType, long eventId, int threadId, long locationId, int value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putInt(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerValue(int eventType, long eventId, int threadId, long locationId, long value) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		buffer.putLong(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerValueVoid(int eventType, long eventId, int threadId, long locationId) {
		buffer.putShort((short)eventType).putLong(eventId).putInt(threadId).putInt((int)locationId);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

}
