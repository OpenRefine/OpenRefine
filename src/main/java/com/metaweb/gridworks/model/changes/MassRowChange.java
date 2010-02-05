package com.metaweb.gridworks.model.changes;

import java.util.ArrayList;
import java.util.List;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class MassRowChange implements Change {
    private static final long serialVersionUID = 5640738656190790569L;

    final protected List<Row> _newRows;
    protected List<Row>       _oldRows;
    
    public MassRowChange(List<Row> newRows) {
        _newRows = newRows;
    }
    
    @Override
    public void apply(Project project) {
        synchronized (project) {
            _oldRows = new ArrayList<Row>(project.rows);
            project.rows.clear();
            project.rows.addAll(_newRows);
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            project.rows.clear();
            project.rows.addAll(_oldRows);
        }
    }

}
