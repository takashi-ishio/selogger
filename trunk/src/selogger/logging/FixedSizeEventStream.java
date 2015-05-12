package selogger.logging;


import selogger.Config;

public class FixedSizeEventStream extends BinaryFileListStream implements IEventWriter {

	public static final int BYTES_PER_EVENT = 30;

	public FixedSizeEventStream(SequentialFileName filenames, Config.OutputOption outputOption, int threads) {
		super(filenames, outputOption, threads, BYTES_PER_EVENT);
	}
	
	public void registerEventWithoutData(int eventType, int threadId, long locationId) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(0L).putInt(0).putLong(0L);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}
	
	public void registerLong(int eventType, int threadId, long locationId, long longData) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(longData).putInt(0).putLong(0L);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}
	
	public void registerLongInt(int eventType, int threadId, long locationId, long longData, int intData) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(longData).putInt(intData).putLong(0L);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}
	
	// registerLongValue
	public void registerLongValue(int eventType, int threadId, long locationId, long longData, double value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(longData).putInt(0).putDouble(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerLongValue(int eventType, int threadId, long locationId, long longData, float value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(longData).putInt(0).putFloat(value).putInt(0);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerLongValue(int eventType, int threadId, long locationId, long longData, int value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(longData).putInt(0).putInt(value).putInt(0);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerLongValue(int eventType, int threadId, long locationId, long longData, long value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(longData).putInt(0).putLong(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	// registerIntValue-------------------------------
	public void registerIntValue(int eventType, int threadId, long locationId, int intData, double value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(0L).putInt(intData).putDouble(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerIntValue(int eventType, int threadId, long locationId, int intData, float value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(0L).putInt(intData).putFloat(value).putInt(0);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerIntValue(int eventType, int threadId, long locationId, int intData, int value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(0L).putInt(intData).putInt(value).putInt(0);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerIntValue(int eventType, int threadId, long locationId, int intData, long value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(0L).putInt(intData).putLong(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	
	// registerLongIntValue-----
	public void registerLongIntValue(int eventType, int threadId, long locationId, long longData, int intData, double value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(longData).putInt(intData).putDouble(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerLongIntValue(int eventType, int threadId, long locationId, long longData, int intData, float value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(longData).putInt(intData).putFloat(value).putInt(0);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerLongIntValue(int eventType, int threadId, long locationId, long longData, int intData, int value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(longData).putInt(intData).putInt(value).putInt(0);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerLongIntValue(int eventType, int threadId, long locationId, long longData, int intData, long value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(longData).putInt(intData).putLong(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}


	//---------
	public void registerValue(int eventType, int threadId, long locationId, double value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(0L).putInt(0).putDouble(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerValue(int eventType, int threadId, long locationId, float value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(0L).putInt(0).putFloat(value).putInt(0);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerValue(int eventType, int threadId, long locationId, int value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(0L).putInt(0).putInt(value).putInt(0);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerValue(int eventType, int threadId, long locationId, long value) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(0L).putInt(0).putLong(value);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerValueVoid(int eventType, int threadId, long locationId) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putLong(0L).putInt(0).putLong(0L);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	//----------------
	public void registerParams(int eventType, int threadId, long locationId, int types, int param1, int param2, int param3) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putInt(types).putInt(param1).putInt(param2).putInt(param3).putInt(0);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}
	
	public void registerParams(int eventType, int threadId, long locationId, int types, int param1, int param2, long param3) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putInt(types).putInt(param1).putInt(param2).putLong(param3);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerParams(int eventType, int threadId, long locationId, int types, int param1, long param2, int param3) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putInt(types).putInt(param1).putLong(param2).putInt(param3);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerParams(int eventType, int threadId, long locationId, int types, long param1, int param2, int param3) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putInt(types).putLong(param1).putInt(param2).putInt(param3);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

	public void registerParams(int eventType, int threadId, long locationId, int types, long param1, long param2) {
		buffer.putShort((short)eventType).putInt(threadId).putInt((int)locationId).putInt(types).putLong(param1).putLong(param2);
		counter++;
		if (counter == EVENTS_PER_FILE) save();
	}

}
