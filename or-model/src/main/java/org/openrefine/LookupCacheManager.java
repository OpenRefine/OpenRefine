package org.openrefine;

import org.apache.commons.lang3.NotImplementedException;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.HasFieldsListImpl;
import org.openrefine.expr.WrappedRow;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.util.LookupException;

import java.util.*;

/**
 * Manage the cache of project's lookups.
 *
 * @author Lu Liu
 */
public class LookupCacheManager {

    protected final Map<String, ProjectLookup> _lookups = new HashMap<>();

    /**
     * Computes the ProjectLookup based on combination key,
     * returns the cached one from the HashMap if already computed.
     *
     * @param targetProject the project to look up
     * @param targetColumn  the column of the target project to look up
     * @return a {@link ProjectLookup} instance of the lookup result
     */
    public ProjectLookup getLookup(long targetProject, String targetColumn) throws LookupException {
        String key = targetProject + ";" + targetColumn;
        if (!_lookups.containsKey(key)) {
            ProjectLookup lookup = new ProjectLookup(targetProject, targetColumn);
            computeLookup(lookup);

            synchronized (_lookups) {
                _lookups.put(key, lookup);
            }
        }

        return _lookups.get(key);
    }

    public void flushLookupsInvolvingProject(long projectID) {
        synchronized (_lookups) {
            for (Iterator<Map.Entry<String, ProjectLookup>> it = _lookups.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, ProjectLookup> entry = it.next();
                ProjectLookup lookup = entry.getValue();
                if (lookup.targetProjectID == projectID) {
                    it.remove();
                }
            }
        }
    }

    public void flushLookupsInvolvingProjectColumn(long projectID, String columnName) {
        synchronized (_lookups) {
            for (Iterator<Map.Entry<String, ProjectLookup>> it = _lookups.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, ProjectLookup> entry = it.next();
                ProjectLookup lookup = entry.getValue();
                if (lookup.targetProjectID == projectID && lookup.targetColumnName.equals(columnName)) {
                    it.remove();
                }
            }
        }
    }

    protected void computeLookup(ProjectLookup lookup) throws LookupException {
        // TODO to be migrated
        throw new NotImplementedException("not migrated to the new architecture yet");
        /*
        if (lookup.targetProjectID < 0) {
            return;
        }

        Project targetProject = ProjectManager.singleton.getProject(lookup.targetProjectID);
        ProjectMetadata targetProjectMetadata = ProjectManager.singleton.getProjectMetadata(lookup.targetProjectID);
        if (targetProject == null) {
            return;
        }

        // if this is a lookup on the index column
        if (lookup.targetColumnName.equals(Cross.INDEX_COLUMN_NAME)) {
            for (int r = 0; r < targetProject.rows.size(); r++) {
                lookup.valueToRowIndices.put(String.valueOf(r) , Collections.singletonList(r));
            }
            return; // return directly
        }

        Column targetColumn = targetProject.columnModel.getColumnByName(lookup.targetColumnName);
        if (targetColumn == null) {
            throw new LookupException("Unable to find column " + lookup.targetColumnName + " in project " + targetProjectMetadata.getName());
        }

        // We can't use for-each here, because we'll need the row index when creating WrappedRow
        int count = targetProject.rows.size();
        for (int r = 0; r < count; r++) {
            Row targetRow = targetProject.rows.get(r);
            Object value = targetRow.getCellValue(targetColumn.getCellIndex());
            if (ExpressionUtils.isNonBlankData(value)) {
                String valueStr = value.toString();
                lookup.valueToRowIndices.putIfAbsent(valueStr, new ArrayList<>());
                lookup.valueToRowIndices.get(valueStr).add(r);
            }
        }
        */
    }

    static public class ProjectLookup {

        final public long targetProjectID;
        final public String targetColumnName;

        final public Map<Object, List<Integer>> valueToRowIndices = new HashMap<>();

        ProjectLookup(long targetProjectID, String targetColumnName) {
            this.targetProjectID = targetProjectID;
            this.targetColumnName = targetColumnName;
        }

        public HasFieldsListImpl getRows(Object value) {
            if (!ExpressionUtils.isNonBlankData(value)) return null;
            String valueStr = value.toString();
            if (valueToRowIndices.containsKey(valueStr)) {
                Project targetProject = ProjectManager.singleton.getProject(targetProjectID);
                
                GridState grid = targetProject.getCurrentGridState();
                ColumnModel columnModel = grid.getColumnModel();
                if (targetProject != null) {
                    HasFieldsListImpl rows = new HasFieldsListImpl();
                    for (Integer r : valueToRowIndices.get(valueStr)) {
                        Row row = grid.getRow(r);
                        rows.add(new WrappedRow(columnModel, r, row));
                    }

                    return rows;
                }
            }
            return null;
        }
    }
}
