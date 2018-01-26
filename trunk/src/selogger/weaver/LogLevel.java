package selogger.weaver;

public enum LogLevel {
	Normal, IgnoreArrayInitializer, OnlyEntryExit, Failed;
	
	@Override
	public String toString() {
		switch (this) {
		case Normal:
			return "Normal";
		case IgnoreArrayInitializer:
			return "IgnoreArrayInitializer";
		case OnlyEntryExit:
			return "OnlyEntryExit";
		case Failed:
			return "Failed";
		default:
			return "";
		}
	}
}
