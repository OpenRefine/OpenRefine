
package org.openrefine.model.changes;

import java.util.Map;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.RecordFilter;
import org.openrefine.browsing.RowFilter;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.RecordMapper;
import org.openrefine.model.RowMapper;
import org.openrefine.overlay.OverlayModel;

/**
 * A change which acts by transforming each row regardless of its context, and only those matched by facets. In records
 * mode, this change is applied on each row of the filtered records.
 * 
 * This class can also be subclassed for changes which ignore the engine, by initializing the engine config with
 * {@link EngineConfig.ALL_ROWS}.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class RowMapChange extends EngineDependentChange {

    /**
     * Constructs a change given a row-wise function to apply to all filtered rows.
     * 
     * @param engineConfig
     *            the facets and engine mode to determine the filtered rows
     */
    public RowMapChange(EngineConfig engineConfig) {
        super(engineConfig);
    }

    /**
     * Returns the function that is applied to each row and row index.
     * 
     * @param columnModel
     *            the initial column model
     */
    public abstract RowMapper getRowMapper(ColumnModel columnModel);

    /**
     * Subclasses can override this to change the column model when the change is applied
     * 
     * @param grid
     *            the initial grid state
     * @return the new column model
     */
    public ColumnModel getNewColumnModel(GridState grid) {
        return grid.getColumnModel();
    }

    /**
     * Subclasses can override this to change the overlay models when the change is applied.
     * 
     * @param grid
     *            the initial grid state
     * @return the new column model
     */
    public Map<String, OverlayModel> getNewOverlayModels(GridState grid) {
        return grid.getOverlayModels();
    }

    @Override
    public GridState apply(GridState projectState) {
        Engine engine = getEngine(projectState);
        RowMapper operation = getRowMapper(projectState.getColumnModel());
        ColumnModel newColumnModel = getNewColumnModel(projectState);
        Map<String, OverlayModel> newOverlayModels = getNewOverlayModels(projectState);
        if (Mode.RowBased.equals(engine.getMode())) {
            RowFilter rowFilter = engine.combinedRowFilters();
            return projectState.mapFilteredRows(rowFilter, operation, newColumnModel)
                    .withOverlayModels(newOverlayModels);
        } else {
            RecordFilter recordFilter = engine.combinedRecordFilters();
            RecordMapper recordMapper = RecordMapper.rowWiseRecordMapper(operation);
            return projectState.mapFilteredRecords(recordFilter, recordMapper, newColumnModel)
                    .withOverlayModels(newOverlayModels);
        }
    }
}
