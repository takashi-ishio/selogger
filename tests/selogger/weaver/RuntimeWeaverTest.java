package selogger.weaver;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import selogger.logging.io.BinaryStreamLogger;
import selogger.logging.io.DiscardLogger;
import selogger.logging.io.EventFrequencyLogger;
import selogger.logging.io.FilterLogger;
import selogger.logging.io.LatestEventLogger;
import selogger.logging.io.TextStreamLogger;

public class RuntimeWeaverTest {
	
	/**
	 * A temporary folder for execution traces
	 */
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void testConstructor() {
		String outputOption = ",output=" + folder.getRoot().toString();

		RuntimeWeaver w = new RuntimeWeaver("format=omni" + outputOption);
		Assert.assertTrue(w.logger instanceof TextStreamLogger);
		w.close();

		w = new RuntimeWeaver("format=omnitext" + outputOption);
		Assert.assertTrue(w.logger instanceof TextStreamLogger);
		w.close();

		w = new RuntimeWeaver("format=omnibinary" + outputOption);
		Assert.assertTrue(w.logger instanceof BinaryStreamLogger);
		w.close();

		w = new RuntimeWeaver("format=nearomni" + outputOption);
		Assert.assertTrue(w.logger instanceof LatestEventLogger);
		w.close();

		w = new RuntimeWeaver("format=nearomni,logstart=X,logend=Y" + outputOption);
		Assert.assertTrue(w.logger instanceof FilterLogger);
		w.close();

		w = new RuntimeWeaver("format=nearomni,logstart=X###METHOD_ENTRY,logend=Y###METHOD_EXIT" + outputOption);
		Assert.assertTrue(w.logger instanceof FilterLogger);
		w.close();

		// logstart and logend must be specified at the same time
		w = new RuntimeWeaver("format=nearomni,logstart=X###METHOD_ENTRY" + outputOption);
		Assert.assertFalse(w.logger instanceof FilterLogger);
		w.close();

		w = new RuntimeWeaver("format=nearomni,logend=X###METHOD_ENTRY" + outputOption);
		Assert.assertFalse(w.logger instanceof FilterLogger);
		w.close();

		w = new RuntimeWeaver("format=freq" + outputOption);
		Assert.assertTrue(w.logger instanceof EventFrequencyLogger);
		w.close();
		
		w = new RuntimeWeaver("format=discard");
		Assert.assertTrue(w.logger instanceof DiscardLogger);
		w.close();

	}
}
