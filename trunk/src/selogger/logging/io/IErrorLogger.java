package selogger.logging.io;

public interface IErrorLogger {

	public void record(Throwable t);
	public void record(String msg);
	public void close();
}
