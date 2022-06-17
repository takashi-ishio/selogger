package selogger.logging.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import selogger.logging.util.JsonBuffer;
import selogger.weaver.DataInfo;
import selogger.weaver.IDataInfoListener;

public abstract class AbstractEventLogger implements IDataInfoListener {

	private ArrayList<DataInfo> dataids;
	private String formatName;
	
	public AbstractEventLogger(String formatName) {
		this.formatName = formatName;
		dataids = new ArrayList<>(65536);
	}
	
	@Override
	public void onCreated(List<DataInfo> events) {
		dataids.addAll(events);
	}
	
	/**
	 * Write the trace data into a json file
	 * @param trace specifies a file 
	 */
	protected void saveJson(File trace) {
		try (PrintWriter w = new PrintWriter(new FileOutputStream(trace))) {
			w.write("{ \"format\":\"" + formatName + "\", \"events\": [\n");
			
			boolean isFirst = true;
			for (int i=0; i<dataids.size(); i++) {
				if (!isRecorded(i)) continue;
				if (isFirst) { 
					isFirst = false;
				} else {
					w.write(",\n");
				}
				
				JsonBuffer buf = new JsonBuffer();
				buf.writeStartObject();
				DataInfo d = dataids.get(i);
				buf.writeStringField("cname", d.getMethodInfo().getClassName());
				buf.writeStringField("mname", d.getMethodInfo().getMethodName());
				buf.writeStringField("mdesc", d.getMethodInfo().getMethodDesc());
				buf.writeStringField("mhash", d.getMethodInfo().getShortMethodHash());
				buf.writeNumberField("line", d.getLine());
				buf.writeNumberField("inst", d.getInstructionIndex());
				buf.writeStringField("event", d.getEventType().name());
				if (d.getAttributes() != null) {
					buf.writeObjectFieldStart("attr");
					d.getAttributes().foreach(buf);
					buf.writeEndObject();
				}
				buf.writeStringField("vtype", d.getValueDesc().toString());
				writeAttributes(buf, d);
				buf.writeEndObject();
				w.write(buf.toString());
			}
			w.write("\n]}");
		} catch (Throwable e) {
		}
	}
	
	protected ArrayList<DataInfo> getDataids() {
		return dataids;
	}
	
	protected abstract boolean isRecorded(int dataid);
	
	protected abstract void writeAttributes(JsonBuffer json, DataInfo dataid); 
	
}
