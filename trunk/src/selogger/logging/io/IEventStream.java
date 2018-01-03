package selogger.logging.io;

public interface IEventStream {

	public void write(int dataId, long value);
	public void close();

}
