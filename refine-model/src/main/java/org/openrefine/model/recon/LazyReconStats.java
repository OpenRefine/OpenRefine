package org.openrefine.model.recon;

import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.ModelException;
import org.openrefine.model.Row;

/**
 * Recon stats that are only computed when needed (lazily).
 * 
 * @author Antonin Delpeuch
 *
 */
public class LazyReconStats implements ReconStats {

    private static final long serialVersionUID = 5308688632569364002L;
    
    private ReconStats _values = null;
    private final transient GridState _grid;
    private final String _columnName;
    
    public LazyReconStats(GridState grid, String columnName) {
        _grid = grid;
        _columnName = columnName;
    }

    @Override
    public long getNonBlanks() {
        ensureComputed();
        return _values.getNonBlanks();
    }

    @Override
    public long getNewTopics() {
        ensureComputed();
        return _values.getNewTopics();
    }

    @Override
    public long getMatchedTopics() {
        ensureComputed();
        return _values.getMatchedTopics();
    }
    
    /**
     * Updates the column model with a lazily-computed ReconStats object.
     * This can be used to update recon statistics without incurring an aggregation
     * as long as the recon stats are not read.
     * 
     * @param gridState
     * @param columnName
     * @return
     */
    public static GridState updateReconStats(GridState gridState, String columnName) {
        ReconStats reconStats = new LazyReconStats(gridState, columnName);
        ColumnModel columnModel = gridState.getColumnModel();
        int columnIndex = columnModel.getColumnIndexByName(columnName);
        ColumnMetadata newColumnMetadata = columnModel.getColumnByName(columnName).withReconStats(reconStats);
        ColumnModel newColumnModel = columnModel;
        try {
            newColumnModel = columnModel.replaceColumn(columnIndex, newColumnMetadata);
        } catch (ModelException e) {
            // unreachable: we did not change the column name
        }
        return gridState.withColumnModel(newColumnModel);
    }
    
    /**
     * Updates the column model with a lazily-computed ReconStats object.
     * This can be used to update recon statistics without incurring an aggregation
     * as long as the recon stats are not read.
     * 
     * @param gridState
     * @param columnIndex
     * @return
     */
    public static GridState updateReconStats(GridState gridState, int columnIndex) {
        String columnName = gridState.getColumnModel().getColumns().get(columnIndex).getName();
        return updateReconStats(gridState, columnName);
    }
    
    private void ensureComputed() {
        if (_values == null) {
            _values = ReconStatsImpl.create(_grid, _columnName);
        }
    }
    
    @Override
    public String toString() {
        if (_values == null) {
            return "[LazyReconStats]";
        } else {
            return _values.toString();
        }
    }
    
    @Override
    public boolean equals(Object other) {
        ensureComputed();
        return _values.equals(other);
    }
    
    @Override
    public int hashCode() {
        ensureComputed();
        return _values.hashCode();
    }

    @Override
    public ReconStats withRow(Row row, int columnIndex) {
        ensureComputed();
        return _values.withRow(row, columnIndex);
    }

    @Override
    public ReconStats sum(ReconStats other) {
        ensureComputed();
        return _values.sum(other);
    }

}
