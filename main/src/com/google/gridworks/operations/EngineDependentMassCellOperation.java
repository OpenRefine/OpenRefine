package com.google.gridworks.operations;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.google.gridworks.browsing.Engine;
import com.google.gridworks.browsing.FilteredRows;
import com.google.gridworks.browsing.RowVisitor;
import com.google.gridworks.history.Change;
import com.google.gridworks.history.HistoryEntry;
import com.google.gridworks.model.Column;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.changes.CellChange;
import com.google.gridworks.model.changes.MassCellChange;

abstract public class EngineDependentMassCellOperation extends EngineDependentOperation {
    final protected String    _columnName;
    final protected boolean _updateRowContextDependencies;
    
    protected EngineDependentMassCellOperation(
            JSONObject engineConfig, String columnName, boolean updateRowContextDependencies) {
        super(engineConfig);
        _columnName = columnName;
        _updateRowContextDependencies = updateRowContextDependencies;
    }

    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = createEngine(project);
        
        Column column = project.columnModel.getColumnByName(_columnName);
        if (column == null) {
            throw new Exception("No column named " + _columnName);
        }
        
        List<CellChange> cellChanges = new ArrayList<CellChange>(project.rows.size());
        
        FilteredRows filteredRows = engine.getAllFilteredRows();
        try {
            filteredRows.accept(project, createRowVisitor(project, cellChanges, historyEntryID));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        String description = createDescription(column, cellChanges);
        
        return new HistoryEntry(
            historyEntryID, project, description, this, createChange(project, column, cellChanges));
    }
    
    protected Change createChange(Project project, Column column, List<CellChange> cellChanges) {
        return new MassCellChange(
            cellChanges, column.getName(), _updateRowContextDependencies);
    }
    
    abstract protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception;
    abstract protected String createDescription(Column column, List<CellChange> cellChanges);
}
