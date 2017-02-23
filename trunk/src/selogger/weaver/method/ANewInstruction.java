package selogger.weaver.method;

public class ANewInstruction {

	private int dataId;
	private String typeName;

	public ANewInstruction(int dataId, String typeName) {
		this.dataId = dataId;
		this.typeName = typeName;
	}
	
	public int getDataId() {
		return dataId;
	}
	
	public String getTypeName() {
		return typeName;
	}

}
