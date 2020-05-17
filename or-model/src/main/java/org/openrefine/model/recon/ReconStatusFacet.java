
package org.openrefine.model.recon;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.openrefine.browsing.RecordFilter;
import org.openrefine.browsing.RowFilter;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetAggregator;
import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.browsing.facets.FacetResult;
import org.openrefine.browsing.facets.FacetState;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Recon.Judgment;
import org.openrefine.model.ReconStats;
import org.openrefine.model.Row;

/**
 * Facet provided to compute reconciliation statistics via the {@link GridState} API - this facet is not actually
 * exposed to end users at the moment (a text facet is used instead).
 */
public class ReconStatusFacet implements Facet {

    public static class Config implements FacetConfig {

        public String columnName;

        public Config(String columnName) {
            this.columnName = columnName;
        }

        @Override
        public Facet apply(ColumnModel columnModel) {
            return new ReconStatusFacet(this, columnModel.getColumnIndexByName(columnName));
        }

        @Override
        public Set<String> getColumnDependencies() {
            return Collections.singleton(columnName);
        }

        @Override
        public FacetConfig renameColumnDependencies(Map<String, String> substitutions) {
            return new Config(substitutions.getOrDefault(columnName, columnName));
        }

        @Override
        public boolean isNeutral() {
            return true;
        }

        @Override
        public String getJsonType() {
            return null; // not exposed to the user
        }
    }

    private static class ReconStatsAggregator implements FacetAggregator<ReconStats> {

        private static final long serialVersionUID = -7078589836137133764L;
        int _cellIndex;

        protected ReconStatsAggregator(int cellIndex) {
            _cellIndex = cellIndex;
        }

        @Override
        public ReconStats sum(ReconStats first, ReconStats second) {
            return new ReconStats(
                    first.nonBlanks + second.nonBlanks,
                    first.newTopics + second.newTopics,
                    first.matchedTopics + second.matchedTopics);
        }

        @Override
        public ReconStats withRow(ReconStats stats, long rowId, Row row) {
            int nonBlanks = 0;
            int newTopics = 0;
            int matchedTopics = 0;
            Cell cell = row.getCell(_cellIndex);
            if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                nonBlanks++;

                if (cell.recon != null) {
                    if (cell.recon.judgment == Judgment.New) {
                        newTopics++;
                    } else if (cell.recon.judgment == Judgment.Matched) {
                        matchedTopics++;
                    }
                }
            }
            return new ReconStats(
                    stats.nonBlanks + nonBlanks,
                    stats.newTopics + newTopics,
                    stats.matchedTopics + matchedTopics);
        }

        @Override
        public RowFilter getRowFilter() {
            return RowFilter.ANY_ROW;
        }

        @Override
        public RecordFilter getRecordFilter() {
            return RecordFilter.ANY_RECORD;
        }

    }

    Config _config;
    int _columnIndex;

    public ReconStatusFacet(Config config, int columnIndex) {
        _config = config;
        _columnIndex = columnIndex;
    }

    @Override
    public FacetConfig getConfig() {
        return _config;
    }

    @Override
    public FacetState getInitialFacetState() {
        return new ReconStats(0, 0, 0);
    }

    @Override
    public FacetAggregator<?> getAggregator() {
        return new ReconStatsAggregator(_columnIndex);
    }

    @Override
    public FacetResult getFacetResult(FacetState state) {
        return (ReconStats) state;
    }

}
