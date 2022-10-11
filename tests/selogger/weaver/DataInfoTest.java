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
	
	/**
	 * 
	 */
	@Test
	public void testParse() {
		DataInfo entry = DataInfo.parse("20,0,0,11,14,CATCH,Ljava/lang/Object;,location=exceptional-exit,type=Ljava/lang/Throwable;,start=0,end=14,handler=14");
		Assert.assertEquals(5, entry.getAttributes().getAttributeCount());
		
		DataInfo entry2 = DataInfo.parse("21,0,0,11,14,METHOD_EXCEPTIONAL_EXIT,Ljava/lang/Object;,location=exceptional-exit-rethrow");
		Assert.assertEquals(1, entry2.getAttributes().getAttributeCount());

		DataInfo entry3 = DataInfo.parse("22,0,1,17,-1,RESERVED,V,null");
		Assert.assertEquals(0, entry3.getAttributes().getAttributeCount());
		
	}
}
