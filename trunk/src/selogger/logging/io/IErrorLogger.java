package selogger.logging.io;

public interface IErrorLogger {

	public void log(Throwable t);
	public void log(String msg);
	public void close();
}
