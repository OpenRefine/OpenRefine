
package org.openrefine;

import java.io.Serializable;
import java.util.*;

import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.HasFieldsListImpl;
import org.openrefine.expr.WrappedRow;
import org.openrefine.history.History;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.util.LookupException;

/**
 * Manage the cache of project's lookups.
 *
 * @author Lu Liu
 */
public class LookupCacheManager {

    public static final String INDEX_COLUMN_NAME = "_OpenRefine_Index_Column_Name_";

    protected final Map<String, ProjectLookup> _lookups = new HashMap<>();

    /**
     * Computes the ProjectLookup based on combination key, returns the cached one from the HashMap if already computed.
     *
     * @param targetProject
     *            the project to look up
     * @param targetColumn
     *            the column of the target project to look up
     */
    public ProjectLookup getLookup(long targetProject, String targetColumn) throws LookupException {
        String key = targetProject + ";" + targetColumn;

        Project project = ProjectManager.singleton.getProject(targetProject);
        if (project == null) {
            throw new LookupException(String.format("Project %d could not be found", targetProject));
        }

        // Retrieve the id of the last entry, used for cache invalidation
        History history = project.getHistory();
        List<HistoryEntry> entries = history.getLastPastEntries(1);
        long changeId = 0;
        if (!entries.isEmpty()) {
            changeId = entries.get(0).getId();
        }

        ProjectLookup lookup = _lookups.get(key);

        if (lookup == null || lookup.getChangeId() != changeId) {
            lookup = new ProjectLookup(project.getCurrentGrid(), targetColumn, changeId, project.getMetadata().getName());

            synchronized (_lookups) {
                _lookups.put(key, lookup);
            }
        }

        return lookup;
    }

    static public class ProjectLookup implements Serializable {

        private static final long serialVersionUID = -7316491331964997894L;
        private final Grid grid;
        private final long changeId;
        final public String targetColumnName;

        final public Map<Object, List<Long>> valueToRowIndices = new HashMap<>();

        ProjectLookup(Grid grid, String columnName, long changeId, String projectName) throws LookupException {
            this.grid = grid;
            this.targetColumnName = columnName;
            this.changeId = changeId;

            // Populate the index
            // if this is a lookup on the index column
            if (INDEX_COLUMN_NAME.equals(targetColumnName)) {
                for (long r = 0; r < grid.rowCount(); r++) {
                    valueToRowIndices.put(String.valueOf(r), Collections.singletonList(r));
                }
                return; // return directly
            }

            ColumnModel columnModel = grid.getColumnModel();
            int targetColumnIndex = columnModel.getColumnIndexByName(columnName);
            if (targetColumnIndex == -1) {
                throw new LookupException("Unable to find column " + targetColumnName + " in project " + projectName);
            }

            // We can't use for-each here, because we'll need the row index when creating WrappedRow
            for (IndexedRow indexedRow : grid.iterateRows(RowFilter.ANY_ROW)) {
                Row targetRow = indexedRow.getRow();
                Object value = targetRow.getCellValue(targetColumnIndex);
                if (ExpressionUtils.isNonBlankData(value)) {
                    String valueStr = value.toString();
                    valueToRowIndices.putIfAbsent(valueStr, new ArrayList<>());
                    valueToRowIndices.get(valueStr).add(indexedRow.getIndex());
                }
            }
        }

        public long getChangeId() {
            return changeId;
        }

        public HasFieldsListImpl getRows(Object value) {
            if (!ExpressionUtils.isNonBlankData(value))
                return null;
            String valueStr = value.toString();
            ColumnModel columnModel = grid.getColumnModel();
            HasFieldsListImpl rows = new HasFieldsListImpl();
            List<Long> rowIds = valueToRowIndices.get(valueStr);
            if (rowIds == null) {
                return null;
            }
            List<IndexedRow> matchedRows = grid.getRows(rowIds);
            for (IndexedRow indexedRow : matchedRows) {
                rows.add(new WrappedRow(columnModel, indexedRow.getIndex(), indexedRow.getRow()));
            }

            return rows;
        }
    }
}
