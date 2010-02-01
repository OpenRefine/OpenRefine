package com.metaweb.gridlock.process;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridlock.Jsonizable;

public class ProcessManager implements Jsonizable {
	protected List<Process> _processes = new LinkedList<Process>();
	
	public ProcessManager() {
		
	}
	

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("processes"); writer.array();
		for (Process p : _processes) {
			p.write(writer, options);
		}
		writer.endArray();
		
		writer.endObject();
	}

	public boolean queueProcess(Process process) {
		if (process.isImmediate() && _processes.size() == 0) {
			process.performImmediate();
			return true;
		} else {
			_processes.add(process);
			
			update();
			
			return false;
		}
	}
	
	public void onDoneProcess(Process p) {
		_processes.remove(p);
		update();
	}
	
	protected void update() {
		while (_processes.size() > 0) {
			Process p = _processes.get(0);
			if (p.isImmediate()) {
				p.performImmediate();
				_processes.remove(0);
			} else if (p.isDone()) {
				_processes.remove(0);
			} else {
				if (!p.isRunning()) {
					p.startPerforming(this);
				}
				break;
			}
		}
	}
}
