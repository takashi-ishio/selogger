package selogger.test;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import selogger.logging.SequentialFileName;

public class SequentialFileNameTest {

	@Test
	public void testGetNextFile() {
		SequentialFileName seq = new SequentialFileName(new File("."), "ABC", ".txt", 5);
		Assert.assertEquals(new File(".", "ABC00001.txt"), seq.getNextFile());
		Assert.assertEquals(new File(".", "ABC00002.txt"), seq.getNextFile());
		Assert.assertEquals(new File(".", "ABC00003.txt"), seq.getNextFile());
		
		SequentialFileName seq2 = new SequentialFileName(new File("."), "ABC", ".txt.gz", 0);
		Assert.assertEquals(new File(".", "ABC1.txt.gz"), seq2.getNextFile());
		Assert.assertEquals(new File(".", "ABC2.txt.gz"), seq2.getNextFile());
		Assert.assertEquals(new File(".", "ABC3.txt.gz"), seq2.getNextFile());
	}
}
