package com.metaweb.gridworks.history;

import com.metaweb.gridworks.model.Project;

public class ChangeSequence implements Change {
    private static final long serialVersionUID = 5029993970500006428L;
    
    final protected Change[] _changes;
    
    public ChangeSequence(Change[] changes) {
        _changes = changes;
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {
            for (int i = 0; i < _changes.length; i++) {
                _changes[i].apply(project);
            }
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            for (int i = _changes.length - 1; i >= 0 ; i--) {
                _changes[i].apply(project);
            }
        }
    }

}
