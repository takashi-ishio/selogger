package selogger.logging;

/**
 * An interface to specify target events 
 */
public interface ILoggingTarget {

	/**
	 * This method is called for filtering events.
	 * @param dataid represents an event.
	 * @return whether a given event is a "target" event or not.  
	 */
	public boolean isTarget(int dataid);
}
