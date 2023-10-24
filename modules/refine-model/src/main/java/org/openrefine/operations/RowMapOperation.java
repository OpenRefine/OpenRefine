
package org.openrefine.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnId;
import org.openrefine.model.ColumnInsertion;
import org.openrefine.model.ColumnMapper;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Record;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.RecordMapper;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.exceptions.MissingColumnException;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.overlay.OverlayModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

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

        ColumnModel columnModel = projectState.getColumnModel();
        Map<String, OverlayModel> overlayModels = projectState.getOverlayModels();

        ColumnModel newColumnModel;
        RowInRecordMapper positiveMapper;
        RowInRecordMapper negativeMapper;

        // Isolate mappers so that they only read the dependencies that they declare
        List<String> dependencies = getColumnDependencies();
        List<ColumnId> dependencyIds = null;
        if (dependencies != null) {
            dependencyIds = new ArrayList<>(dependencies.size());
            for (String columnName : dependencies) {
                ColumnMetadata metadata = columnModel.getColumnByName(columnName);
                if (metadata == null) {
                    throw new MissingColumnException(columnName);
                }
                dependencyIds.add(metadata.getColumnId());
            }
            ColumnModel inputColumnModel = new ColumnModel(dependencyIds.stream()
                    .map(dId -> new ColumnMetadata(dId.getColumnName()))
                    .collect(Collectors.toList()),
                    columnModel.getKeyColumnIndex(),
                    columnModel.hasRecords());
            ColumnMapper columnMapper = new ColumnMapper(dependencyIds, columnModel);

            newColumnModel = getNewColumnModel(inputColumnModel, overlayModels, context);
            positiveMapper = columnMapper.translateRowInRecordMapper(
                    getPositiveRowMapper(inputColumnModel, overlayModels, context));
            negativeMapper = columnMapper.translateRowInRecordMapper(
                    getNegativeRowMapper(inputColumnModel, overlayModels, context));
        } else {
            // expose the full column model to the row mapper
            positiveMapper = getPositiveRowMapper(columnModel, overlayModels, context);
            negativeMapper = getNegativeRowMapper(columnModel, overlayModels, context);
            newColumnModel = getNewColumnModel(columnModel, overlayModels, context);
        }

        // Complete mappers so that they also add the columns that they do not touch
        List<ColumnInsertion> insertions = getColumnInsertions();
        if (insertions != null) {
            List<ColumnMetadata> columns = columnModel.getColumns();
            // Build new column model while keeping track of the indices of the columns to be written.
            // Each value is either:
            // - a non-negative integer, interpreted as a column index from the original grid
            // - a negative integer, interpreted as a "-(i + 1)" where i is a column index from the mapper
            List<Integer> indicesMap = new ArrayList<>(IntStream.range(0, columns.size()).boxed().collect(Collectors.toList()));
            List<String> newColumnNames = new ArrayList<>(columnModel.getColumnNames());

            // process deletions first
            for (String deletedColumn : getColumnDeletions()) {
                int index = newColumnNames.indexOf(deletedColumn);
                if (index != -1) {
                    indicesMap.remove(index);
                    newColumnNames.remove(index);
                }
            }

            // insert columns declared by the operation
            int mapperColumnIndex = 0;
            for (ColumnInsertion insertion : insertions) {
                int insertionIndex = insertion.getInsertAt() != null ? (newColumnNames.indexOf(insertion.getInsertAt()) + 1)
                        : newColumnNames.size();

                if (insertion.isReplace()) {
                    int replacingIndex = insertionIndex - 1;
                    if (replacingIndex == -1) {
                        throw new MissingColumnException(insertion.getInsertAt());
                    }
                    indicesMap.set(replacingIndex, -(mapperColumnIndex + 1));
                    newColumnNames.set(replacingIndex, insertion.getName()); // TODO detect conflicts
                } else {
                    indicesMap.add(insertionIndex, -(mapperColumnIndex + 1));
                    newColumnNames.add(insertionIndex, insertion.getName()); // TODO detect conflicts
                }
                mapperColumnIndex++;
            }

            // derive new column model and mappers
            ColumnModel columnModelFromMapper = newColumnModel;
            List<ColumnMetadata> fullColumns = indicesMap.stream().map(i -> {
                if (i >= 0) {
                    return columnModel.getColumnByIndex(i);
                } else {
                    return columnModelFromMapper.getColumnByIndex(-(i + 1))
                            .withLastModified(context.getHistoryEntryId());
                }
            }).collect(Collectors.toList());
            ColumnModel fullColumnModel = new ColumnModel(fullColumns, columnModel.getKeyColumnIndex(),
                    columnModel.hasRecords() && positiveMapper.preservesRecordStructure());
            newColumnModel = fullColumnModel;

            positiveMapper = mapperWithInsertions(positiveMapper, indicesMap);
            negativeMapper = mapperWithInsertions(negativeMapper, indicesMap);
        }

        Map<String, OverlayModel> newOverlayModels = getNewOverlayModels(columnModel, projectState.getOverlayModels(),
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
     * The list of columns that are read by the operation, in the order in which they should be supplied to the
     * row/record mapper. If this returns null, then all columns are passed in their original order. <br>
     * This does not need to include the columns relied on by the facets.
     */
    @JsonProperty(value = "columnDependencies", access = JsonProperty.Access.READ_ONLY)
    @JsonInclude(Include.NON_NULL)
    public List<String> getColumnDependencies() {
        return null;
    }

    /**
     * The list of columns that this operation touches.
     * <ul>
     * <li>if null, then we expect that the rows/records returned by the row/record mappers are complete, meaning that
     * all columns are potentially touched by the operation</li>
     * <li>if not null, then the final row/records returned by the operation are constructed by inserting the new values
     * returned by the row/record mappers in the locations designated by this list of column insertions.</li>
     * </ul>
     */
    @JsonProperty(value = "columnInsertions", access = JsonProperty.Access.READ_ONLY)
    @JsonInclude(Include.NON_NULL)
    public List<ColumnInsertion> getColumnInsertions() {
        return null;
    }

    /**
     * The list of columns that this operation deletes. Any column name added here will be removed from the returned
     * columns, before the inserted columns ({@link #getColumnInsertions()}) are added.
     */
    @JsonProperty(value = "columnDeletions", access = JsonProperty.Access.READ_ONLY)
    @JsonInclude(Include.NON_EMPTY)
    public Set<String> getColumnDeletions() {
        return Collections.emptySet();
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

    private static RowInRecordMapper mapperWithInsertions(RowInRecordMapper mapper, List<Integer> indexMap) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 1807833316147737273L;

            @Override
            public boolean preservesRecordStructure() {
                return mapper.preservesRecordStructure();
            }

            @Override
            public Row call(Record record, long rowId, Row row) {
                Row mapped = mapper.call(record, rowId, row);
                List<Cell> newCells = indexMap.stream()
                        .map(i -> i >= 0 ? row.getCell(i) : mapped.getCell(-(i + 1)))
                        .collect(Collectors.toList());
                return new Row(newCells, row.flagged, row.starred);

            }
        };
    }

}
