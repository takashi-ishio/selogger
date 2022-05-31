package selogger.weaver;

import org.junit.Assert;
import org.junit.Test;

import selogger.EventType;

public class DataInfoPatternTest {

	@Test
	public void testClassNames() {
		DataInfoPattern pattern = new DataInfoPattern(".+");
		Assert.assertTrue(pattern.isTarget("selogger/my/Class", "testX", "()V", EventType.METHOD_ENTRY));
	}
	
	@Test
	public void testEventType() {
		DataInfoPattern pattern = new DataInfoPattern(".+#.+#.+#METHOD_ENTRY");
		Assert.assertTrue(pattern.isTarget("selogger/my/Class", "testX", "()V", EventType.METHOD_ENTRY));

		DataInfoPattern pattern2 = new DataInfoPattern(".+#.+#.+#METHOD_NORMAL_EXIT;METHOD_EXCEPTIONAL_EXIT");
		Assert.assertFalse(pattern2.isTarget("selogger/my/Class", "testX", "()V", EventType.METHOD_ENTRY));
		Assert.assertTrue(pattern2.isTarget("selogger/my/Class", "testX", "()V", EventType.METHOD_NORMAL_EXIT));
		Assert.assertTrue(pattern2.isTarget("selogger/my/Class", "testX", "()V", EventType.METHOD_EXCEPTIONAL_EXIT));

		DataInfoPattern pattern3 = new DataInfoPattern(".+#.+#.+#METHOD_EXIT");
		Assert.assertFalse(pattern3.isTarget("selogger/my/Class", "testX", "()V", EventType.METHOD_ENTRY));
		Assert.assertTrue(pattern3.isTarget("selogger/my/Class", "testX", "()V", EventType.METHOD_NORMAL_EXIT));
		Assert.assertTrue(pattern3.isTarget("selogger/my/Class", "testX", "()V", EventType.METHOD_EXCEPTIONAL_EXIT));

	}

	@Test
	public void testEmptyPattern() {
		DataInfoPattern pattern = new DataInfoPattern(".+###METHOD_ENTRY");
		Assert.assertTrue(pattern.isTarget("selogger/my/Class", "testX", "()V", EventType.METHOD_ENTRY));
	}

}
