
package org.openrefine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrefine.browsing.util.ExpressionBasedRowEvaluable;
import org.openrefine.browsing.util.RowEvaluable;
import org.openrefine.browsing.util.StringValuesFacetAggregator;
import org.openrefine.browsing.util.StringValuesFacetState;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.history.History;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.util.FacetCountException;

/**
 * Holds a cache of facet counts, to be used by the facetCount GREL function.
 * 
 * @author Antonin Delpeuch
 *
 */
public class FacetCountCacheManager {

    protected final Map<String, FacetCount> _cache = new HashMap<>();

    public StringValuesFacetState getFacetState(long targetProjectId, String expression, String columnName) throws FacetCountException {
        String key = targetProjectId + ";" + columnName + "_" + expression;

        Project project = ProjectManager.singleton.getProject(targetProjectId);
        if (project == null) {
            throw new FacetCountException(String.format("Project %d could not be found", targetProjectId));
        }

        // Retrieve the id of the last entry, used for cache invalidation
        History history = project.getHistory();
        List<HistoryEntry> entries = history.getLastPastEntries(1);
        long changeId = 0;
        if (!entries.isEmpty()) {
            changeId = entries.get(0).getId();
        }

        FacetCount facetCount = _cache.get(key);

        if (facetCount == null || facetCount.getChangeId() != changeId) {
            facetCount = computeFacetCount(project.getCurrentGridState(), columnName, expression, changeId);

            synchronized (_cache) {
                _cache.put(key, facetCount);
            }
        }

        return facetCount.getFacetState();
    }

    protected FacetCount computeFacetCount(GridState grid, String columnName, String expression, long changeId) throws FacetCountException {
        ColumnModel columnModel = grid.getColumnModel();
        int cellIndex = columnModel.getColumnIndexByName(columnName);
        if (cellIndex == -1) {
            throw new FacetCountException(String.format("The column '%s' could not be found", columnName));
        }

        RowEvaluable evaluable;
        try {
            evaluable = new ExpressionBasedRowEvaluable(columnName, cellIndex, MetaParser.parse(expression), columnModel);
        } catch (ParsingException e) {
            throw new FacetCountException(String.format("The expression '%s' is invalid: %s", expression, e.getMessage()));
        }
        StringValuesFacetAggregator aggregator = new StringValuesFacetAggregator(
                columnModel, cellIndex, evaluable, Collections.emptySet(), false, false, false);
        StringValuesFacetState initialState = new StringValuesFacetState();
        StringValuesFacetState result = grid.aggregateRows(aggregator, initialState);
        return new FacetCount(result, changeId);
    }

    public static class FacetCount {

        private final StringValuesFacetState _facetState;
        private final long _changeId;

        public FacetCount(StringValuesFacetState facetState, long changeId) {
            _facetState = facetState;
            _changeId = changeId;
        }

        public long getChangeId() {
            return _changeId;
        }

        public StringValuesFacetState getFacetState() {
            return _facetState;
        }
    }

}
