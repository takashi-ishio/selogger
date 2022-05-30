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
}
