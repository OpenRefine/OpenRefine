
package org.openrefine.operations;

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
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.overlay.OverlayModel;

/**
 * Simplifies the architecture for immediate operations which simply act by mapping rows. This allows the mapper to be
 * defined in a method of the operation.
 * 
 * This class can also be subclassed by operations which ignore the engine, by initializing the engine config with
 * {@link EngineConfig#ALL_ROWS}.
 */
abstract public class RowMapOperation extends EngineDependentOperation {

    protected RowMapOperation(EngineConfig engineConfig) {
        super(engineConfig);
    }

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws OperationException {
        Engine engine = getEngine(projectState, context.getProjectId());
        RowInRecordMapper positiveMapper = getPositiveRowMapper(projectState.getColumnModel(), projectState.getOverlayModels(), context);
        RowInRecordMapper negativeMapper = getNegativeRowMapper(projectState.getColumnModel(), projectState.getOverlayModels(), context);
        ColumnModel newColumnModel = getNewColumnModel(projectState.getColumnModel(), projectState.getOverlayModels(), context);
        Map<String, OverlayModel> newOverlayModels = getNewOverlayModels(projectState.getColumnModel(), projectState.getOverlayModels(),
                context);
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
                mappedState.withOverlayModels(newOverlayModels),
                recordsPreserved ? GridPreservation.PRESERVES_RECORDS : GridPreservation.PRESERVES_ROWS);
    }

    /**
     * Returns the column model after the change is applied to the given grid. By default, returns the same column
     * model.
     * 
     * @return the column model of the new grid
     */
    protected ColumnModel getNewColumnModel(ColumnModel columnModel, Map<String, OverlayModel> overlayModels, ChangeContext context)
            throws OperationException {
        return columnModel;
    }

    /**
     * Returns the row mapper applied to the rows matched by the filter.
     */
    protected RowInRecordMapper getPositiveRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            ChangeContext context) throws OperationException {
        return RowInRecordMapper.IDENTITY;
    }

    /**
     * Returns the row mapper applied to the rows not matched by the filter.
     */
    protected RowInRecordMapper getNegativeRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            ChangeContext context) throws OperationException {
        return RowInRecordMapper.IDENTITY;
    }

    /**
     * Returns the new overlay models after this change is applied.
     */
    protected Map<String, OverlayModel> getNewOverlayModels(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            ChangeContext context) throws OperationException {
        return overlayModels;
    }

}
