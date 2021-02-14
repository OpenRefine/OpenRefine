
package org.openrefine.model.recon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.recon.Recon.Judgment;

/**
 * Statically stored recon statistics
 * 
 * @author Antonin Delpeuch
 *
 */
public class ReconStatsImpl implements ReconStats {

    private static final long serialVersionUID = -6321424927189309528L;

    private final long nonBlanks;
    private final long newTopics;
    private final long matchedTopics;

    /**
     * Creates a summary of reconciliation statistics.
     * 
     * @param nonBlanks
     *            the number of non blank cells in the column
     * @param newTopics
     *            the number of cells matched to a new topic in the column
     * @param matchedTopics
     *            the number of cells matched to an existing topic in the column
     */
    @JsonCreator
    public ReconStatsImpl(
            @JsonProperty("nonBlanks") long nonBlanks,
            @JsonProperty("newTopics") long newTopics,
            @JsonProperty("matchedTopics") long matchedTopics) {
        this.nonBlanks = nonBlanks;
        this.newTopics = newTopics;
        this.matchedTopics = matchedTopics;
    }

    @Override
    public long getNonBlanks() {
        return nonBlanks;
    }

    @Override
    public long getNewTopics() {
        return newTopics;
    }

    @Override
    public long getMatchedTopics() {
        return matchedTopics;
    }

    @Override
    public ReconStats withRow(Row row, int columnIndex) {
        int nonBlanks = 0;
        int newTopics = 0;
        int matchedTopics = 0;
        Cell cell = row.getCell(columnIndex);
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
        return new ReconStatsImpl(
                getNonBlanks() + nonBlanks,
                getNewTopics() + newTopics,
                getMatchedTopics() + matchedTopics);
    }

    @Override
    public ReconStats sum(ReconStats other) {
        return new ReconStatsImpl(
                getNonBlanks() + other.getNonBlanks(),
                getNewTopics() + other.getNewTopics(),
                getMatchedTopics() + other.getMatchedTopics());
    }

    /**
     * Creates reconciliation statistics from a column of cells.
     * 
     * @param state
     *            the state of the grid
     * @param columnName
     *            the column for which we should gather reconciliation statistics
     * @return the statistics of cell reconciliation in the column
     */
    static public ReconStats create(GridState state, String columnName) {
        Aggregator aggregator = new Aggregator(state.getColumnModel().getColumnIndexByName(columnName));
        return state.aggregateRows(aggregator, ZERO);
    }

    /**
     * Creates reconciliation statistics for all columns in the grid where a reconciliation config is set, in a single
     * pass over the grid, and adds them to the column model accordingly.
     * 
     * @author Antonin Delpeuch
     */
    static public GridState updateReconStats(GridState state) {

        List<ColumnMetadata> columns = state.getColumnModel()
                .getColumns();
        List<Integer> columnIndices = IntStream.range(0, columns.size())
                .filter(i -> columns.get(i).getReconConfig() != null)
                .boxed()
                .collect(Collectors.toList());
        MultipleAggregator aggregator = new MultipleAggregator(columnIndices);
        List<ReconStats> initialState = columnIndices
                .stream()
                .map(i -> ReconStats.ZERO)
                .collect(Collectors.toList());
        MultiReconStats multiReconStats = state.aggregateRows(aggregator, new MultiReconStats(initialState));

        ColumnModel columnModel = state.getColumnModel();
        for (int i = 0; i != columnIndices.size(); i++) {
            columnModel = columnModel.withReconStats(columnIndices.get(i), multiReconStats._stats.get(i));
        }
        return state.withColumnModel(columnModel);
    }

    protected static class Aggregator implements RowAggregator<ReconStats> {

        private static final long serialVersionUID = -7078589836137133764L;
        int _cellIndex;

        protected Aggregator(int cellIndex) {
            _cellIndex = cellIndex;
        }

        @Override
        public ReconStats sum(ReconStats first, ReconStats second) {
            return first.sum(second);
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
                    if (cell.recon.getJudgment() == Judgment.New) {
                        newTopics++;
                    } else if (cell.recon.getJudgment() == Judgment.Matched) {
                        matchedTopics++;
                    }
                }
            }
            return new ReconStatsImpl(
                    stats.getNonBlanks() + nonBlanks,
                    stats.getNewTopics() + newTopics,
                    stats.getMatchedTopics() + matchedTopics);
        }

    }

    protected static class MultiReconStats implements Serializable {

        private static final long serialVersionUID = 8308171792561019947L;
        protected final List<ReconStats> _stats;

        protected MultiReconStats(List<ReconStats> stats) {
            _stats = stats;
        }
    }

    protected static class MultipleAggregator implements RowAggregator<MultiReconStats> {

        private static final long serialVersionUID = -8282928695144412185L;
        private final List<Aggregator> _aggregators;

        protected MultipleAggregator(List<Integer> columnIndices) {
            _aggregators = columnIndices
                    .stream()
                    .map(i -> new Aggregator(i))
                    .collect(Collectors.toList());
        }

        @Override
        public MultiReconStats sum(MultiReconStats first, MultiReconStats second) {
            List<ReconStats> reconStats = new ArrayList<>(_aggregators.size());
            for (int i = 0; i != _aggregators.size(); i++) {
                reconStats.add(_aggregators.get(i).sum(first._stats.get(i), second._stats.get(i)));
            }
            return new MultiReconStats(reconStats);
        }

        @Override
        public MultiReconStats withRow(MultiReconStats state, long rowId, Row row) {
            List<ReconStats> reconStats = new ArrayList<>(_aggregators.size());
            for (int i = 0; i != _aggregators.size(); i++) {
                reconStats.add(_aggregators.get(i).withRow(state._stats.get(i), rowId, row));
            }
            return new MultiReconStats(reconStats);
        }

    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReconStats)) {
            return false;
        }
        ReconStats rs = (ReconStats) other;
        return (rs.getNonBlanks() == getNonBlanks() &&
                rs.getNewTopics() == getNewTopics() &&
                rs.getMatchedTopics() == getMatchedTopics());
    }

    @Override
    public int hashCode() {
        return (int) (getNonBlanks() + getNewTopics() + getMatchedTopics());
    }

    @Override
    public String toString() {
        return String.format("[ReconStats: non-blanks: %d, new: %d, matched: %d]",
                getNonBlanks(), getNewTopics(), getMatchedTopics());
    }

}
