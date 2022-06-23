package selogger.weaver;

import org.junit.Assert;
import org.junit.Test;

import selogger.logging.io.BinaryStreamLogger;
import selogger.logging.io.DiscardLogger;
import selogger.logging.io.EventFrequencyLogger;
import selogger.logging.io.FilterLogger;
import selogger.logging.io.LatestEventLogger;
import selogger.logging.io.TextStreamLogger;

public class RuntimeWeaverTest {

	@Test
	public void testConstructor() {
		RuntimeWeaver w = new RuntimeWeaver("format=omni");
		Assert.assertTrue(w.logger instanceof TextStreamLogger);

		w = new RuntimeWeaver("format=omnitext");
		Assert.assertTrue(w.logger instanceof TextStreamLogger);

		w = new RuntimeWeaver("format=omnibinary");
		Assert.assertTrue(w.logger instanceof BinaryStreamLogger);

		w = new RuntimeWeaver("format=nearomni");
		Assert.assertTrue(w.logger instanceof LatestEventLogger);

		w = new RuntimeWeaver("format=nearomni,logstart=X,logend=Y");
		Assert.assertTrue(w.logger instanceof FilterLogger);

		w = new RuntimeWeaver("format=nearomni,logstart=X###METHOD_ENTRY,logend=Y###METHOD_EXIT");
		Assert.assertTrue(w.logger instanceof FilterLogger);

		// logstart and logend must be specified at the same time
		w = new RuntimeWeaver("format=nearomni,logstart=X###METHOD_ENTRY");
		Assert.assertFalse(w.logger instanceof FilterLogger);

		w = new RuntimeWeaver("format=nearomni,logend=X###METHOD_ENTRY");
		Assert.assertFalse(w.logger instanceof FilterLogger);

		w = new RuntimeWeaver("format=freq");
		Assert.assertTrue(w.logger instanceof EventFrequencyLogger);
		
		w = new RuntimeWeaver("format=discard");
		Assert.assertTrue(w.logger instanceof DiscardLogger);

	}
}
