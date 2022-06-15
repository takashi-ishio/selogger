package selogger.weaver;

import org.junit.Assert;
import org.junit.Test;

import selogger.EventType;
import selogger.weaver.method.Descriptor;
import selogger.weaver.method.InstructionAttributes;

public class DataInfoTest {

	/**
	 * Create an object and test getAttribute method
	 */
	@Test
	public void testAttributes() {
		InstructionAttributes attr = InstructionAttributes.of("desc", "(IJ)Ljava/lang/Object;")
				.and("type", 1)
				.and("empty", "")
				.and("property", 2);
		DataInfo entry = new DataInfo(0, 1, 2, 3, 4, EventType.CALL, Descriptor.Object, attr);
		Assert.assertEquals("(IJ)Ljava/lang/Object;", entry.getAttribute("desc", ""));
		Assert.assertEquals("1", entry.getAttribute("type", ""));
		Assert.assertEquals("2", entry.getAttribute("property", ""));
		Assert.assertEquals("", entry.getAttribute("empty", "1"));
		Assert.assertEquals("", entry.getAttribute("NotExist", ""));
	}
	
	/**
	 * Create an object and test getAttribute method.
	 * The test cases come from actual DataInfo.
	 */
	@Test
	public void testActualAttributes() {
		DataInfo entry = new DataInfo(0, 1, 2, 3, 4, EventType.CALL, Descriptor.Object, InstructionAttributes.of("Desc", "(Ljava/lang/String;Ljava/lang/String;)V"));
		Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)V", entry.getAttribute("Desc", ""));
	}
}
