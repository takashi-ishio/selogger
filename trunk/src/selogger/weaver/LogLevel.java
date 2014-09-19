package selogger.weaver;

public enum LogLevel {
	Normal, IgnoreArrayInitializer, OnlyEntryExit, Failed;
	
	@Override
	public String toString() {
		switch (this) {
		case Normal:
			return "N";
		case IgnoreArrayInitializer:
			return "IgnoreArray";
		case OnlyEntryExit:
			return "OnlyEntryExit";
		case Failed:
			return "F";
		default:
			return "";
		}
	}
}
