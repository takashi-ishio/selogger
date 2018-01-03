package selogger.test;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import selogger.logging.FileNameGenerator;

public class SequentialFileNameTest {

	@Test
	public void testGetNextFile() {
		FileNameGenerator seq = new FileNameGenerator(new File("."), "ABC", ".txt");
		Assert.assertEquals(new File(".", "ABC00001.txt"), seq.getNextFile());
		Assert.assertEquals(new File(".", "ABC00002.txt"), seq.getNextFile());
		Assert.assertEquals(new File(".", "ABC00003.txt"), seq.getNextFile());
		
		FileNameGenerator seq2 = new FileNameGenerator(new File("."), "ABC", ".txt.gz");
		Assert.assertEquals(new File(".", "ABC00001.txt.gz"), seq2.getNextFile());
		Assert.assertEquals(new File(".", "ABC00002.txt.gz"), seq2.getNextFile());
		Assert.assertEquals(new File(".", "ABC00003.txt.gz"), seq2.getNextFile());
	}
}
