package com.metaweb.gridworks.model.changes;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class RowStarChange implements Change {
    private static final long serialVersionUID = 7343472491567342093L;
    
    final int rowIndex;
    final boolean newStarred;
    boolean oldStarred;
    
    public RowStarChange(int rowIndex, boolean newStarred) {
        this.rowIndex = rowIndex;
        this.newStarred = newStarred;
    }

    public void apply(Project project) {
        Row row = project.rows.get(rowIndex);
        
        oldStarred = row.starred;
        row.starred = newStarred;
    }

    public void revert(Project project) {
        Row row = project.rows.get(rowIndex);
        
        row.starred = oldStarred;
    }
}