package com.metaweb.gridworks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.HasFieldsListImpl;
import com.metaweb.gridworks.expr.WrappedRow;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class InterProjectModel {
    static public class ProjectJoin {
        final public long   fromProjectID;
        final public String fromProjectColumnName;
        final public long   toProjectID;
        final public String toProjectColumnName;
        
        final public Map<Object, List<Integer>> valueToRowIndices = 
            new HashMap<Object, List<Integer>>();
        
        ProjectJoin(
            long   fromProjectID,
            String fromProjectColumnName,
            long   toProjectID,
            String toProjectColumnName
        ) {
            this.fromProjectID = fromProjectID;
            this.fromProjectColumnName = fromProjectColumnName;
            this.toProjectID = toProjectID;
            this.toProjectColumnName = toProjectColumnName;
        }
        
        public HasFieldsListImpl getRows(Object value) {
            if (ExpressionUtils.isNonBlankData(value) && valueToRowIndices.containsKey(value)) {
                Project toProject = ProjectManager.singleton.getProject(toProjectID);
                if (toProject != null) {
                    HasFieldsListImpl rows = new HasFieldsListImpl();
                    for (Integer r : valueToRowIndices.get(value)) {
                        Row row = toProject.rows.get(r);
                        rows.add(new WrappedRow(toProject, r, row));
                    }
                    
                    return rows;
                }
            }
            return null;
        }
    }
    
    protected Map<String, ProjectJoin> _joins = new HashMap<String, ProjectJoin>();
    
    public ProjectJoin getJoin(String fromProject, String fromColumn, String toProject, String toColumn) {
        String key = fromProject + ";" + fromColumn + ";" + toProject + ";" + toColumn;
        if (!_joins.containsKey(key)) {
            ProjectJoin join = new ProjectJoin(
                ProjectManager.singleton.getProjectID(fromProject), 
                fromColumn, 
                ProjectManager.singleton.getProjectID(toProject), 
                toColumn
            );
            
            computeJoin(join);
            
            _joins.put(key, join);
        }
        
        return _joins.get(key);
    }
    
    public void flushJoinsInvolvingProject(long projectID) {
        for (Entry<String, ProjectJoin> entry : _joins.entrySet()) {
            ProjectJoin join = entry.getValue();
            if (join.fromProjectID == projectID || join.toProjectID == projectID) {
                _joins.remove(entry.getKey());
            }
        }
    }

    public void flushJoinsInvolvingProjectColumn(long projectID, String columnName) {
        for (Entry<String, ProjectJoin> entry : _joins.entrySet()) {
            ProjectJoin join = entry.getValue();
            if (join.fromProjectID == projectID && join.fromProjectColumnName.equals(columnName) || 
                join.toProjectID == projectID && join.toProjectColumnName.equals(columnName)) {
                _joins.remove(entry.getKey());
            }
        }
    }

    protected void computeJoin(ProjectJoin join) {
        if (join.fromProjectID < 0 || join.toProjectID < 0) {
            return;
        }
        
        Project fromProject = ProjectManager.singleton.getProject(join.fromProjectID);
        Project toProject = ProjectManager.singleton.getProject(join.toProjectID);
        if (fromProject == null || toProject == null) {
            return;
        }
        
        Column fromColumn = fromProject.columnModel.getColumnByName(join.fromProjectColumnName);
        Column toColumn = toProject.columnModel.getColumnByName(join.toProjectColumnName);
        if (fromColumn == null || toColumn == null) {
            return;
        }
        
        for (Row fromRow : fromProject.rows) {
            Object value = fromRow.getCellValue(fromColumn.getCellIndex());
            if (ExpressionUtils.isNonBlankData(value) && !join.valueToRowIndices.containsKey(value)) {
                join.valueToRowIndices.put(value, new ArrayList<Integer>());
            }
        }
        
        int count = toProject.rows.size();
        for (int r = 0; r < count; r++) {
            Row toRow = toProject.rows.get(r);
            
            Object value = toRow.getCellValue(toColumn.getCellIndex());
            if (ExpressionUtils.isNonBlankData(value) && join.valueToRowIndices.containsKey(value)) {
                join.valueToRowIndices.get(value).add(r);
            }
        }
    }
}
