package selogger.logging.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import selogger.logging.IErrorLogger;
import selogger.logging.util.JsonBuffer;
import selogger.weaver.DataInfo;
import selogger.weaver.IDataInfoListener;

public abstract class AbstractEventLogger implements IDataInfoListener {

	private ArrayList<DataInfo> dataids;
	private String formatName;
	private IErrorLogger logger;
	
	public AbstractEventLogger(String formatName, IErrorLogger logger) {
		this.formatName = formatName;
		this.logger = logger;
		dataids = new ArrayList<>(65536);
	}
	
	@Override
	public void onCreated(List<DataInfo> events) {
		dataids.addAll(events);
	}
	
	/**
	 * Write the buffer contents into a text file
	 * @param filename
	 */
	protected void saveText(File trace) {
		try (PrintWriter w = new PrintWriter(new FileWriter(trace))) {
			w.write("cname,mname,mdesc,mhash,line,inst,attr,event,vtype," + getColumnNames() + "\n");
			for (int i=0; i<dataids.size(); i++) {
				if (isRecorded(i)) {
					DataInfo d = getDataids().get(i);
					StringBuilder builder = new StringBuilder(512);
					builder.append(d.getMethodInfo().getClassName());
					builder.append(",");
					builder.append(d.getMethodInfo().getMethodName());
					builder.append(",");
					builder.append(d.getMethodInfo().getMethodDesc());
					builder.append(",");
					builder.append(d.getMethodInfo().getShortMethodHash());
					builder.append(",");
					builder.append(d.getLine());
					builder.append(",");
					builder.append(d.getInstructionIndex());
					builder.append(",");
					builder.append("\"" + d.getAttributes() + "\"");
					builder.append(",");
					builder.append(d.getEventType().name());
					builder.append(",");
					builder.append(d.getValueDesc().toString());
					builder.append(",");
					writeAttributes(builder, d);
					builder.append("\n");
					w.write(builder.toString());
				}
			}
		} catch (Throwable e) {
			if (logger != null) logger.log(e);
		}
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
			if (logger != null) logger.log(e);
		}
	}
	
	protected ArrayList<DataInfo> getDataids() {
		return dataids;
	}
	
	/**
	 * @param dataid specifies an event
	 * @return A subclass should return true if the event is recorded 
	 */
	protected abstract boolean isRecorded(int dataid);
	
	/**
	 * A subclass overrides this method to write additional attributes for JSON
	 * @param json buffer to record the output
	 * @param d specifies the event to be written
	 */
	protected abstract void writeAttributes(JsonBuffer json, DataInfo d); 
	
	/**
	 * @return A subclass should return a header line for additional attributes
	 */
	protected abstract String getColumnNames();
	
	/**
	 * A subclass overrides this method to write additional attributes for CSV
	 * @param builder is a buffer to record the output
	 * @param d specifies the event to be written
	 */
	protected abstract void writeAttributes(StringBuilder builder, DataInfo d);
	
}
