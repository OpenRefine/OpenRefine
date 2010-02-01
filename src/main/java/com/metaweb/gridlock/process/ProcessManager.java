package com.metaweb.gridlock.process;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

public class ProcessManager {
	protected List<Process> _processes = new LinkedList<Process>();
	
	public ProcessManager() {
		
	}
	
	public JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		List<JSONObject> a = new ArrayList<JSONObject>(_processes.size());
		for (Process p : _processes) {
			a.add(p.getJSON(options));
		}
		o.put("processes", a);
		
		return o;
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
