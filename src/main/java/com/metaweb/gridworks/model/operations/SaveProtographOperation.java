package com.metaweb.gridworks.model.operations;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.process.QuickHistoryEntryProcess;
import com.metaweb.gridworks.protograph.Protograph;

public class SaveProtographOperation implements AbstractOperation {
    private static final long serialVersionUID = 3134524625206033285L;
    
    final protected Protograph _protograph;

	public SaveProtographOperation(
		Protograph protograph
	) {
		_protograph = protograph;
	}

	public Process createProcess(Project project, Properties options)
			throws Exception {
		
        String description = "Save schema-alignment protograph";
        
		Change change = new ProtographChange(_protograph);
		HistoryEntry historyEntry = new HistoryEntry(
			project, description, this, change);

		return new QuickHistoryEntryProcess(project, historyEntry);
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub

	}
	

	static public class ProtographChange implements Change {
		private static final long serialVersionUID = -564820111174473901L;
		
		final protected Protograph 	_newProtograph;
	    protected Protograph		_oldProtograph;
	    
	    public ProtographChange(Protograph protograph) {
	        _newProtograph = protograph;
	    }
	    
	    public void apply(Project project) {
	        synchronized (project) {
	        	_oldProtograph = project.protograph;
	        	project.protograph = _newProtograph;
	        }
	    }

	    public void revert(Project project) {
	        synchronized (project) {
	        	project.protograph = _oldProtograph;
	        }
	    }
	} 
}
