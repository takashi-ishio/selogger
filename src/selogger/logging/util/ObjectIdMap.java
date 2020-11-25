package selogger.logging.util;

import java.lang.ref.WeakReference;


/**
 * This object assigns a unique ID to each object reference. 
 * Conceptually, this is a kind of IdentityHashMap from Object to long.
 */
public class ObjectIdMap {

	private long nextId;
	private Entry[] entries;
	private int capacity;
	private int threshold;
	private int andKey; 
	private int size;
	private int INT_MAX_BIT = 30;
	

	/**
	 * Create an instance.
	 * @param initialCapacity is the size of an internal array to manage the contents.
	 */
	public ObjectIdMap(int initialCapacity) {
		size = 0;
		nextId = 1;
		
		// To ensure capacity == 0b100...000, so that andKey == 0b111...111
		for (int i=0; i<INT_MAX_BIT+1; ++i) {
			capacity = 1 << i;
			if (capacity > initialCapacity) {
				break;
			}
		}
		andKey = capacity - 1;
		threshold = capacity / 2;
		entries = new Entry[capacity];
	}
	

	/**
	 * Translate an object into an ID.
	 * @param o is an object used in the logging target program.
	 * @return an ID corresponding to the object.
	 * 0 is returned for null.
	 */
	public long getId(Object o) {
		if (o == null) {
			return 0L;
		} 

		int hash = System.identityHashCode(o);
		
		// Search the object.  If found, return the registered ID.
        int index = hash & andKey;
        Entry e = entries[index];
        while (e != null) {
            if (o == e.reference.get()) {
            	return e.objectId;
            }
            e = e.next;
        }
        
       	// If not found, create a new entry for the given object.
        // First, prepares a new object
        onNewObject(o); 

        // Update an entry.  index is re-computed because andKey may be updated by onNewObject.
       	index = hash & andKey;
       	Entry oldEntry = entries[index];
       	long id = nextId;
       	nextId++;
       	e = new Entry(o, id, oldEntry, hash);
       	entries[index] = e;
       	size++;
       	onNewObjectId(o, id);
        	
        if (size >= threshold) {
        	resize();
        }
        return id;
	}
 
	/**
	 * A placeholder for handling a new object.
	 * This method is called when a new object is found, before a new ID is assigned.
	 * @param o is the object passed to the getId method.
	 */
	protected void onNewObject(Object o) {
	}
	
	/**
	 * A placeholder for handling a new object.
	 * This method is called when a new object is found, after a new ID is assigned.
	 * @param o is the object passed to the getId method.
	 * @param id is the ID assigned to the object.
	 */
	protected void onNewObjectId(Object o, long id) {
	}
 
	/**
	 * Enlarge the internal array for entries.
	 */
	private void resize() {
		if (capacity == (1<<INT_MAX_BIT)) {
			capacity = Integer.MAX_VALUE;
			threshold = Integer.MAX_VALUE;
			andKey = capacity;
		} else {
			capacity = capacity * 2;
			threshold = threshold * 2;
			andKey = capacity - 1;
		}

		Entry[] newEntries = new Entry[capacity];
		// Copy contents of the hash table
		for (int from=0; from<entries.length; ++from) {
			Entry fromEntry = entries[from];
			entries[from] = null;
			while (fromEntry != null) {
				Entry nextEntry = fromEntry.next;
				if (fromEntry.reference.get() != null) {
					// Copy non-null entries 
					int index = fromEntry.hashcode & andKey;
					fromEntry.next = newEntries[index];
					newEntries[index] = fromEntry;
				} else {
					// skip null object entry
					fromEntry.next = null;
					size--;
				}
				fromEntry = nextEntry;
			}
		}
		entries = newEntries;
	}
	
	/**
	 * @return the number of objects stored in the map.
	 */
	public int size() {
		return size;
	}
	
	/**
	 * @return the size of the hash table inside the map.
	 * This method is declared for debugging. 
	 */
	public int capacity() {
		return capacity;
	}
	
	/**
	 * A simple list structure to store a registered object and its ID.
	 */
	private static class Entry {
		private WeakReference<Object> reference;
		private int hashcode;
		private long objectId;
		private Entry next;
		
		public Entry(Object o, long id, Entry e, int hashcode) {
			this.reference = new WeakReference<Object>(o);
			this.objectId = id;
			this.next = e;
			this.hashcode = hashcode;
		}
	}
	
}
