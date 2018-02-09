package selogger.weaver;

import org.junit.Assert;
import org.junit.Test;

import selogger.EventType;
import selogger.weaver.method.Descriptor;

public class DataInfoTest {

	@Test
	public void testAttributes() {
		DataInfo entry = new DataInfo(0, 1, 2, 3, 4, EventType.CALL, Descriptor.Object, "Desc=(IJ)Ljava/lang/Object;,Type=1,Empty=,Property=2");
		Assert.assertEquals("(IJ)Ljava/lang/Object;", entry.getAttribute("Desc", ""));
		Assert.assertEquals("1", entry.getAttribute("Type", ""));
		Assert.assertEquals("2", entry.getAttribute("Property", ""));
		Assert.assertEquals("", entry.getAttribute("Empty", "1"));
		Assert.assertEquals("", entry.getAttribute("NotExist", ""));
	}
	
	@Test
	public void testActualAttributes() {
		DataInfo entry = new DataInfo(0, 1, 2, 3, 4, EventType.CALL, Descriptor.Object, "Name=addDesc,Desc=(Ljava/lang/String;Ljava/lang/String;)V");
		Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)V", entry.getAttribute("Desc", ""));

		DataInfo entry2 = new DataInfo(0, 1, 2, 3, 4, EventType.CALL, Descriptor.Object, "Name=addDesc,Desc=(Ljava/lang/String;Ljava/lang/String;)V,Desc2=");
		Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)V", entry2.getAttribute("Desc", ""));
	}
}
