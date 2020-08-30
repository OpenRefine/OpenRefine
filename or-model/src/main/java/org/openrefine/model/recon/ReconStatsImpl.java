package org.openrefine.model.recon;

import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.recon.Recon.Judgment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Statically stored recon statistics
 * 
 * @author Antonin Delpeuch
 *
 */
public class ReconStatsImpl implements ReconStats {
    
    private static final long serialVersionUID = -6321424927189309528L;
    
    private final long    nonBlanks;
    private final long    newTopics;
    private final long    matchedTopics;
    
    /**
     * Creates a summary of reconciliation statistics.
     * 
     * @param nonBlanks
     *     the number of non blank cells in the column
     * @param newTopics
     *     the number of cells matched to a new topic in the column
     * @param matchedTopics
     *     the number of cells matched to an existing topic in the column
     */
    @JsonCreator
    public ReconStatsImpl(
            @JsonProperty("nonBlanks")
            long nonBlanks,
            @JsonProperty("newTopics")
            long newTopics,
            @JsonProperty("matchedTopics")
            long matchedTopics) {
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
     * Creates reconciliation statistics from a column of
     * cells.
     * 
     * @param state the state of the grid
     * @param columnName the column for which we should gather reconciliation statistics
     * @return the statistics of cell reconciliation in the column
     */
    static public ReconStats create(GridState state, String columnName) {
        Aggregator aggregator = new Aggregator(state.getColumnModel().getColumnIndexByName(columnName)); 
        return state.aggregateRows(aggregator, ZERO);
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
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReconStats)) {
            return false;
        }
        ReconStats rs = (ReconStats)other;
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
