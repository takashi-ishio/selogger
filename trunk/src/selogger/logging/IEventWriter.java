package selogger.logging;


public interface IEventWriter {

	public boolean hasError();
	public String getErrorMessage();
	
	public void close();
	public void registerEventWithoutData(int eventType, long eventId, int threadId, long locationId);
	public void registerLong(int eventType, long eventId, int threadId, long locationId, long longData);
	public void registerLongInt(int eventType, long eventId, int threadId, long locationId, long longData, int intData);
	public void registerLongValue(int eventType, long eventId, int threadId, long locationId, long longData, double value);
	public void registerLongValue(int eventType, long eventId, int threadId, long locationId, long longData, float value);
	public void registerLongValue(int eventType, long eventId, int threadId, long locationId, long longData, int value);
	public void registerLongValue(int eventType, long eventId, int threadId, long locationId, long longData, long value);
	public void registerIntValue(int eventType, long eventId, int threadId, long locationId, int intData, double value);
	public void registerIntValue(int eventType, long eventId, int threadId, long locationId, int intData, float value);
	public void registerIntValue(int eventType, long eventId, int threadId, long locationId, int intData, int value);
	public void registerIntValue(int eventType, long eventId, int threadId, long locationId, int intData, long value);
	public void registerLongIntValue(int eventType, long eventId, int threadId, long locationId, long longData, int intData, double value);
	public void registerLongIntValue(int eventType, long eventId, int threadId, long locationId, long longData, int intData, float value);
	public void registerLongIntValue(int eventType, long eventId, int threadId, long locationId, long longData, int intData, int value);
	public void registerLongIntValue(int eventType, long eventId, int threadId, long locationId, long longData, int intData, long value);
	public void registerValue(int eventType, long eventId, int threadId, long locationId, double value);
	public void registerValue(int eventType, long eventId, int threadId, long locationId, float value);
	public void registerValue(int eventType, long eventId, int threadId, long locationId, int value);
	public void registerValue(int eventType, long eventId, int threadId, long locationId, long value);
	public void registerValueVoid(int eventType, long eventId, int threadId, long locationId);

}
