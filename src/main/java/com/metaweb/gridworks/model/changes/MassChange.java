package com.metaweb.gridworks.model.changes;

 import java.util.List;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Project;

public class MassChange implements Change {
    private static final long serialVersionUID = -3926239320561407450L;
    
    final protected List<? extends Change> _changes;
	final protected boolean                _updateRowContextDependencies;
	
	public MassChange(List<? extends Change> changes, boolean updateRowContextDependencies) {
		_changes = changes;
		_updateRowContextDependencies = updateRowContextDependencies;
	}
	
	public void apply(Project project) {
		synchronized (project) {
			for (Change change : _changes) {
			    change.apply(project);
			}
			
			if (_updateRowContextDependencies) {
			    project.recomputeRowContextDependencies();
			}
		}
	}

	public void revert(Project project) {
		synchronized (project) {
            for (Change change : _changes) {
                change.revert(project);
            }
			
			if (_updateRowContextDependencies) {
			    project.recomputeRowContextDependencies();
			}
		}
	}
}
