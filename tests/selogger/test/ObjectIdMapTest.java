package selogger.test;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import selogger.logging.util.ObjectIdMap;

/**
 * Test class for ObjectIdMap
 */
public class ObjectIdMapTest {

	@Test
	public void testSmallObjectIdMap() {
		ObjectIdMap map = new ObjectIdMap(100);
		Assert.assertEquals(128, map.capacity());

		// Create 20 strings
		ArrayList<String> strings = new ArrayList<String>();
		for (int i=0; i<20; ++i) {
			strings.add(Integer.toString(i));
		}
		// Check their IDs
		for (int i=0; i<20; ++i) {
			map.getId(strings.get(i));
			Assert.assertEquals(i+1, map.size());
		}
		
		// Check IDs again (they should not change)
		for (int i=0; i<20; ++i) {
			Assert.assertEquals(i+1, map.getId(strings.get(i)));
		}

		// Add a new object
		Assert.assertEquals(21, map.getId(Integer.toString(0)));

		// Check IDs again (they should not change)
		for (int i=0; i<20; ++i) {
			Assert.assertEquals(i+1, map.getId(strings.get(i)));
		}
	}
	

	@Test
	public void testObjectIdMap() {
		ObjectIdMap map = new ObjectIdMap(100);
		Assert.assertEquals(128, map.capacity());
		Assert.assertEquals(0, map.size());
		
		ArrayList<String> strings = new ArrayList<String>();
		for (int i=0; i<10000; ++i) {
			strings.add(Integer.toString(i));
		}
		for (int i=0; i<10000; ++i) {
			map.getId(strings.get(i));
			
			// Check that the map size correctly increases
			if (63 <= i && i < 127) {
				Assert.assertEquals(256, map.capacity());
			} else if (127 <= i && i < 255) { 
				Assert.assertEquals(512, map.capacity());
			}

		}
		
		// Object Id must be correct
		Assert.assertEquals(11L, map.getId(strings.get(10)));
		Assert.assertEquals(1L, map.getId(strings.get(0)));
		Assert.assertEquals(10000, map.getId(strings.get(9999)));
		
		// Inetntionally execute gc() and then check the behavior of IDs
		for (int i=100; i<9900; ++i) {
			strings.remove(Integer.toString(i));
		}
		System.gc();
		for (int i=10000; i<20000; ++i) {
			String s = Integer.toString(i);
			strings.add(s);
			map.getId(s);
		}
		Assert.assertEquals(10200, map.size());
		for (int i=0; i<strings.size(); ++i) {
			String s = strings.get(i);
			int obj = Integer.parseInt(s);
			Assert.assertEquals(obj+1, map.getId(s));
		}

	}
	
	@Test
	public void testNull() {
		ObjectIdMap map = new ObjectIdMap(10);
		Assert.assertEquals(1, map.getId(1));
		Assert.assertEquals(2, map.getId(2));
		Assert.assertEquals(0, map.getId(null));
		Assert.assertEquals(3, map.getId(3));
		Assert.assertEquals(0, map.getId(null));
	}
	
	
}
