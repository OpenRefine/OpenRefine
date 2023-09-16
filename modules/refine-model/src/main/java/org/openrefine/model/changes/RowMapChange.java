
package org.openrefine.model.changes;

import java.util.Map;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.RecordMapper;
import org.openrefine.model.RowFilter;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.RowMapper;
import org.openrefine.overlay.OverlayModel;

/**
 * A change which acts by transforming each row regardless of its context, and only those matched by facets. In records
 * mode, this change is applied on each row of the filtered records.
 * 
 * This class can also be subclassed by changes which ignore the engine, by initializing the engine config with
 * {@link EngineConfig#ALL_ROWS}.
 * 
 *
 */
public abstract class RowMapChange extends EngineDependentChange {

    /**
     * Stores the new column model to be applied on the new grid, and the row mappers used to derive the new grid.
     * <p>
     * The positive mapper is applied to the rows matched by the filter, the negative one is applied to the other rows.
     * <p>
     * Computing both the {@link ColumnModel} and the {@link RowMapper}s can be useful in certain situations where
     * computing both separately would be wasteful.
     * 
     *
     */
    public static class GridMap {

        protected final ColumnModel columnModel;
        protected final RowInRecordMapper positiveMapper;
        protected final RowInRecordMapper negativeMapper;
        protected final Map<String, OverlayModel> overlayModels;

        public GridMap(
                ColumnModel columnModel,
                RowInRecordMapper positiveMapper,
                RowInRecordMapper negativeMapper,
                Map<String, OverlayModel> overlayModels) {
            this.columnModel = columnModel;
            this.positiveMapper = positiveMapper;
            this.negativeMapper = negativeMapper;
            this.overlayModels = overlayModels;
        }
    }

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
     * Returns the function that is applied to each row and row index, the new column model applied to the grid, and the
     * new overlay models. Computing these in one go can be useful in certain situations, for instance when the number
     * of columns added to the column model depends on the data.
     * 
     * If a subclass needs to return only one non-default value out of mapper, column model and overlay models it can
     * instead override the corresponding method.
     * 
     * @param state
     *            the initial column model
     * @throws DoesNotApplyException
     *             if the change does not apply to the given grid
     */
    protected GridMap getGridMap(Grid state, ChangeContext context) throws DoesNotApplyException {
        return new GridMap(
                getNewColumnModel(state, context),
                getPositiveRowMapper(state, context),
                getNegativeRowMapper(state, context),
                getNewOverlayModels(state, context));
    }

    /**
     * Returns the column model after the change is applied to the given grid. By default, returns the same column
     * model.
     * 
     * @param state
     *            the grid to which the change should be applied
     * @return the column model of the new grid
     */
    protected ColumnModel getNewColumnModel(Grid state, ChangeContext context) throws DoesNotApplyException {
        return state.getColumnModel();
    }

    /**
     * Returns the row mapper applied to the rows matched by the filter.
     * 
     * @param state
     *            the initial state of the grid
     */
    protected RowInRecordMapper getPositiveRowMapper(Grid state, ChangeContext context) throws DoesNotApplyException {
        return RowInRecordMapper.IDENTITY;
    }

    /**
     * Returns the row mapper applied to the rows not matched by the filter.
     * 
     * @param state
     *            the initial state of the grid
     */
    protected RowInRecordMapper getNegativeRowMapper(Grid state, ChangeContext context) throws DoesNotApplyException {
        return RowInRecordMapper.IDENTITY;
    }

    /**
     * Returns the new overlay models after this change is applied.
     */
    protected Map<String, OverlayModel> getNewOverlayModels(Grid state, ChangeContext context) throws DoesNotApplyException {
        return state.getOverlayModels();
    }

    /**
     * Method run at the end of the change to update any statistics (such as ReconStats or overlay models) on the
     * resulting state.
     * 
     * @param grid
     *            the grid after the map operation
     * @return the grid with updated column statistics (or any other post transformation)
     */
    protected Grid postTransform(Grid grid, ChangeContext context) throws DoesNotApplyException {
        return grid;
    }

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws DoesNotApplyException {
        Engine engine = getEngine(projectState);
        GridMap gridMap = getGridMap(projectState, context);
        RowInRecordMapper positiveMapper = gridMap.positiveMapper;
        RowInRecordMapper negativeMapper = gridMap.negativeMapper;
        ColumnModel newColumnModel = gridMap.columnModel;
        Map<String, OverlayModel> newOverlayModels = gridMap.overlayModels;
        Grid mappedState;
        if (Mode.RowBased.equals(engine.getMode())) {
            RowFilter rowFilter = engine.combinedRowFilters();
            mappedState = projectState.mapRows(RowMapper.conditionalMapper(rowFilter, positiveMapper, negativeMapper), newColumnModel);
        } else {
            RecordFilter recordFilter = engine.combinedRecordFilters();
            mappedState = projectState.mapRecords(
                    RecordMapper.conditionalMapper(recordFilter, positiveMapper, negativeMapper),
                    newColumnModel);
        }
        boolean recordsPreserved = positiveMapper.preservesRecordStructure() && negativeMapper.preservesRecordStructure();
        return new ChangeResult(
                postTransform(mappedState.withOverlayModels(newOverlayModels), context),
                recordsPreserved ? GridPreservation.PRESERVES_RECORDS : GridPreservation.PRESERVES_ROWS);
    }

    /**
     * Utility method to retrieve a column index and throw an exception if that column does not exist.
     */
    public static int columnIndex(ColumnModel model, String columnName) throws DoesNotApplyException {
        int index = model.getColumnIndexByName(columnName);
        if (index == -1) {
            throw new DoesNotApplyException(
                    String.format("Column '%s' does not exist", columnName));
        }
        return index;
    }
}
