
package org.openrefine.operations;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.FacetConfig;
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
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.changes.RowChangeDataSerializer;
import org.openrefine.model.changes.RowInRecordChangeDataJoiner;
import org.openrefine.model.changes.RowListChangeDataSerializer;
import org.openrefine.operations.exceptions.DuplicateColumnException;
import org.openrefine.operations.exceptions.MissingColumnException;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.overlay.OverlayModel;

/**
 * Operations which act by simply by mapping rows, preserving their order. The row mapper may access the containing
 * record if the operation is run in records mode. The operation provides two {@link RowInRecordMapper}s: a positive
 * one, for the rows which are in the current view (selected by the facets), and a negative one, for the other rows. The
 * latter one is the identity by default, but operations which create new columns generally need to add empty values to
 * make sure the number of cells in the rows match. <br>
 * The operation is able to define:
 * <ul>
 * <li>The columns it reads to derive its new values, as {@link #getColumnDependencies()}. If those dependencies are
 * defined, then the {@link RowInRecordMapper}s provided by the operation will be fed with reduced rows, containing only
 * the declared dependencies, in the order in which they are defined. If the dependencies are left unspecified, the row
 * mappers will be fed with the original rows.</li>
 * <li>The columns it inserts or modifies as {@link #getColumnInsertions()}. If this is provided, then the row mappers
 * are expected to return rows containing only such columns, in the order they are mentioned in the list of column
 * insertions. Those will be then completed by the unmodified columns to form the full resulting rows. If
 * {@link #getColumnInsertions()} is null, then the operation's own row mappers are expected to return the full rows
 * already.</li>
 * <li>In the case when {@link #getColumnInsertions()} is not null, it is also possible to specify which of the original
 * columns should be deleted from the resulting rows. This setting is otherwise ignored.</li>
 * </ul>
 * 
 * This class can also be subclassed by operations which ignore the engine, by initializing the engine config with
 * {@link EngineConfig#ALL_ROWS}.
 */
abstract public class RowMapOperation extends EngineDependentOperation {

    protected String _changeDataId = "eval";

    protected RowMapOperation(EngineConfig engineConfig) {
        super(engineConfig);
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
     * columns, before the inserted columns ({@link #getColumnInsertions()}) are added. This setting is only used if
     * {@link #getColumnInsertions()} is not null.
     */
    @JsonProperty(value = "columnDeletions", access = JsonProperty.Access.READ_ONLY)
    @JsonInclude(Include.NON_EMPTY)
    public Set<String> getColumnDeletions() {
        return Collections.emptySet();
    }

    /**
     * Returns the column model after the change is applied to the given grid. This only used if
     * {@link #getColumnInsertions()} is not null, in which case the new column model is directly derived from the
     * supplied insertions.
     */
    protected ColumnModel getNewColumnModel(ColumnModel columnModel, Map<String, OverlayModel> overlayModels, ChangeContext context)
            throws OperationException {
        return columnModel;
    }

    /**
     * Returns the row mapper applied to the rows matched by the filter.
     * 
     * @param estimatedRowCount
     *            TODO
     */
    protected RowInRecordMapper getPositiveRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            long estimatedRowCount, ChangeContext context) throws OperationException {
        return RowInRecordMapper.IDENTITY;
    }

    /**
     * Returns the row mapper applied to the rows not matched by the filter.
     * 
     * @param estimatedRowCount
     *            TODO
     */
    protected RowInRecordMapper getNegativeRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            long estimatedRowCount, ChangeContext context) throws OperationException {
        return RowInRecordMapper.IDENTITY;
    }

    /**
     * Returns the new overlay models after this change is applied.
     */
    protected Map<String, OverlayModel> getNewOverlayModels(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            ChangeContext context) throws OperationException {
        return overlayModels;
    }

    @JsonIgnore
    protected List<FacetConfig> getCreatedFacets() {
        return Collections.emptyList();
    }

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws OperationException {
        Engine engine = getEngine(projectState, context.getProjectId());

        ColumnModel columnModel = projectState.getColumnModel();
        Map<String, OverlayModel> overlayModels = projectState.getOverlayModels();
        long rowCount = projectState.rowCount();

        ColumnModel newColumnModel;
        RowInRecordMapper positiveMapper;
        RowInRecordMapper negativeMapper;
        boolean preservesRecords;

        List<String> dependencies = getColumnDependencies();
        List<ColumnId> dependencyIds = null;
        List<ColumnId> dependencyIdsWithEngine = null;
        if (dependencies != null) {
            // Isolate mappers so that they only read the dependencies that they declare
            dependencyIds = new ArrayList<>(dependencies.size());
            List<ColumnMetadata> inputColumns = new ArrayList<>();
            for (String columnName : dependencies) {
                ColumnMetadata metadata = columnModel.getColumnByName(columnName);
                if (metadata == null) {
                    throw new MissingColumnException(columnName);
                }
                dependencyIds.add(metadata.getColumnId());
                inputColumns.add(metadata);
            }
            Set<String> engineColumnDependencies = _engineConfig.getColumnDependencies();
            if (engineColumnDependencies != null) {
                dependencyIdsWithEngine = new ArrayList<>(dependencyIds);
                for (String columnName : engineColumnDependencies) {
                    ColumnMetadata metadata = columnModel.getColumnByName(columnName);
                    if (metadata == null) {
                        throw new MissingColumnException(columnName);
                    }
                    dependencyIdsWithEngine.add(metadata.getColumnId());
                }
            }

            ColumnModel inputColumnModel = new ColumnModel(inputColumns,
                    columnModel.getKeyColumnIndex(),
                    columnModel.hasRecords());
            ColumnMapper columnMapper = new ColumnMapper(dependencyIds, columnModel);

            newColumnModel = getNewColumnModel(inputColumnModel, overlayModels, context);
            positiveMapper = columnMapper.translateRowInRecordMapper(
                    getPositiveRowMapper(inputColumnModel, overlayModels, rowCount, context));
            negativeMapper = columnMapper.translateRowInRecordMapper(
                    getNegativeRowMapper(inputColumnModel, overlayModels, rowCount, context));
        } else {
            // expose the full column model to the row mapper
            positiveMapper = getPositiveRowMapper(columnModel, overlayModels, rowCount, context);
            negativeMapper = getNegativeRowMapper(columnModel, overlayModels, rowCount, context);
            newColumnModel = getNewColumnModel(columnModel, overlayModels, context);
        }

        boolean persistResults = positiveMapper.persistResults();
        RowInRecordMapper changeDataProducer = null;
        if (persistResults) {
            changeDataProducer = positiveMapper;
        }
        RowInRecordChangeDataJoiner joiner = defaultJoiner(negativeMapper);

        // Complete mappers so that they also add the columns that they do not touch
        List<ColumnInsertion> insertions = getColumnInsertions();
        if (insertions != null) {
            List<ColumnMetadata> columns = columnModel.getColumns();
            // Build new column model while keeping track of the indices of the columns to be written.
            // Each value is either:
            // - a non-negative integer, interpreted as a column index from the original grid
            // - a negative integer, interpreted as a "-(i + 1)" where i is a column index from the mapper
            List<Integer> indicesMap = new ArrayList<>(IntStream.range(0, columns.size()).boxed().collect(Collectors.toList()));
            List<Integer> negativeIndicesMap = new ArrayList<>(IntStream.range(0, columns.size()).boxed().collect(Collectors.toList()));
            List<String> newColumnNames = new ArrayList<>(columnModel.getColumnNames());
            List<ColumnMetadata> newColumnMetadata = new ArrayList<>(columnModel.getColumns());

            // process deletions first
            for (String deletedColumn : getColumnDeletions()) {
                int index = newColumnNames.indexOf(deletedColumn);
                if (index != -1) {
                    indicesMap.remove(index);
                    negativeIndicesMap.remove(index);
                    newColumnNames.remove(index);
                    newColumnMetadata.remove(index);
                } else {
                    throw new MissingColumnException(deletedColumn);
                }
            }

            // insert columns declared by the operation
            int mapperColumnIndex = 0;
            for (ColumnInsertion insertion : insertions) {
                int insertionIndex = insertion.getInsertAt() != null ? (newColumnNames.indexOf(insertion.getInsertAt()) + 1)
                        : 0;

                ColumnMetadata columnMetadata;
                int sourceColumnIndex;
                if (insertion.getCopiedFrom() == null) {
                    sourceColumnIndex = -(mapperColumnIndex + 1);
                    mapperColumnIndex++;
                    columnMetadata = new ColumnMetadata(insertion.getName())
                            .withLastModified(context.getHistoryEntryId());
                } else {
                    sourceColumnIndex = columnModel.getRequiredColumnIndex(insertion.getCopiedFrom());
                    columnMetadata = columnModel.getColumnByIndex(sourceColumnIndex)
                            .withName(insertion.getName());
                    if (!engine.isNeutral()) {
                        columnMetadata = columnMetadata.withLastModified(context.getHistoryEntryId());
                    }
                }

                if (insertion.isReplace()) {
                    int replacingIndex = insertionIndex - 1;
                    if (replacingIndex == -1) {
                        throw new MissingColumnException(insertion.getInsertAt());
                    }
                    int existingName = newColumnNames.indexOf(insertion.getName());
                    if (existingName != -1 && existingName != replacingIndex) {
                        throw new DuplicateColumnException(insertion.getName());
                    }
                    ColumnMetadata replacedColumn = newColumnMetadata.get(replacingIndex);
                    if (columnMetadata.getReconConfig() == null && replacedColumn.getReconConfig() != null) {
                        columnMetadata = columnMetadata.withReconConfig(replacedColumn.getReconConfig());
                    }
                    if ((columnMetadata.getReconConfig() == null && insertion.getReconConfig() != null)
                            || insertion.getOverrideReconConfig()) {
                        columnMetadata = columnMetadata.withReconConfig(insertion.getReconConfig());
                    }
                    indicesMap.set(replacingIndex, sourceColumnIndex);
                    // no need to change negativeIndicesMap, since we will not touch the column for rows outside of the
                    // view
                    newColumnNames.set(replacingIndex, insertion.getName());
                    newColumnMetadata.set(replacingIndex, columnMetadata);
                } else {
                    int existingName = newColumnNames.indexOf(insertion.getName());
                    if (existingName != -1) {
                        throw new DuplicateColumnException(insertion.getName());
                    }
                    indicesMap.add(insertionIndex, sourceColumnIndex);
                    negativeIndicesMap.add(insertionIndex, sourceColumnIndex);
                    newColumnNames.add(insertionIndex, insertion.getName());
                    newColumnMetadata.add(insertionIndex, columnMetadata);
                }
            }

            if (!persistResults) {
                positiveMapper = mapperWithInsertions(positiveMapper, indicesMap);
                negativeMapper = mapperWithInsertions(negativeMapper, negativeIndicesMap);
            } else {
                joiner = joinerWithInsertions(indicesMap, negativeIndicesMap, columnModel, engine);
            }
            // build new column model
            newColumnModel = new ColumnModel(newColumnMetadata, columnModel.getKeyColumnIndex(), columnModel.hasRecords());
            int origKeyIndex = columnModel.getKeyColumnIndex();
            preservesRecords = 0 <= origKeyIndex && origKeyIndex < indicesMap.size()
                    && indicesMap.get(origKeyIndex) == newColumnModel.getKeyColumnIndex();
        } else {
            preservesRecords = positiveMapper.preservesRecordStructure() && negativeMapper.preservesRecordStructure();

            // mark all columns as being modified
            newColumnModel = new ColumnModel(
                    newColumnModel.getColumns().stream()
                            .map(c -> c.withLastModified(context.getHistoryEntryId()))
                            .collect(Collectors.toList()),
                    newColumnModel.getKeyColumnIndex(), newColumnModel.hasRecords());
        }

        Map<String, OverlayModel> newOverlayModels = getNewOverlayModels(columnModel, projectState.getOverlayModels(),
                context);
        Grid mappedState;
        RowInRecordMapper producer = changeDataProducer;
        if (Mode.RowBased.equals(engine.getMode())) {
            RowFilter rowFilter = engine.combinedRowFilters();
            if (persistResults) {
                ChangeData<Row> changeData;
                try {
                    changeData = context.getChangeData("eval", new RowChangeDataSerializer(), (grid, partialChangeData) -> {
                        Engine localEngine = new Engine(grid, _engineConfig, context.getProjectId());
                        RowFilter filter = localEngine.combinedRowFilters();
                        return grid.mapRows(filter, producer, partialChangeData);
                    }, dependencyIdsWithEngine, Engine.Mode.RowBased);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                mappedState = projectState.join(changeData, joiner, newColumnModel);
            } else {
                mappedState = projectState.mapRows(RowMapper.conditionalMapper(rowFilter, positiveMapper, negativeMapper), newColumnModel);
            }
        } else {
            RecordFilter recordFilter = engine.combinedRecordFilters();
            if (persistResults) {
                ChangeData<List<Row>> changeData;
                try {
                    changeData = context.getChangeData("eval", new RowListChangeDataSerializer(), (grid, partialChangeData) -> {
                        Engine localEngine = new Engine(grid, _engineConfig, context.getProjectId());
                        RecordFilter filter = localEngine.combinedRecordFilters();
                        return grid.mapRecords(filter, producer, partialChangeData);
                    }, dependencyIdsWithEngine, Engine.Mode.RecordBased);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                mappedState = projectState.join(changeData, joiner, newColumnModel);
            } else {
                mappedState = projectState.mapRecords(
                        RecordMapper.conditionalMapper(recordFilter, positiveMapper, negativeMapper),
                        newColumnModel);
            }
        }
        return new ChangeResult(
                mappedState.withOverlayModels(newOverlayModels),
                preservesRecords ? GridPreservation.PRESERVES_RECORDS : GridPreservation.PRESERVES_ROWS,
                getCreatedFacets());
    }

    protected static RowInRecordChangeDataJoiner joinerWithInsertions(List<Integer> positiveIndexMap, List<Integer> negativeIndexMap,
            ColumnModel columnModel, Engine engine) {
        RowFilter rowFilter = Mode.RowBased.equals(engine.getMode()) ? engine.combinedRowFilters() : null;
        RecordFilter recordFilter = Mode.RecordBased.equals(engine.getMode()) ? engine.combinedRecordFilters() : null;
        return new RowInRecordChangeDataJoiner() {

            private static final long serialVersionUID = -1227446910427143739L;

            @Override
            public List<Row> call(Record record, IndexedData<List<Row>> indexedData) {
                if (recordFilter != null && recordFilter.filterRecord(record)) {
                    return super.call(record, indexedData);
                } else {
                    return record.getRows().stream()
                            .map(this::negativeRowMap)
                            .collect(Collectors.toList());
                }
            }

            @Override
            public Row call(Record record, Row row, IndexedData<Row> indexedData) {
                if (indexedData != null && (rowFilter == null || rowFilter.filterRow(indexedData.getId(), row))) {
                    Row mapped = indexedData.getData();
                    List<Cell> newCells = positiveIndexMap.stream()
                            .map(i -> i >= 0 ? row.getCell(i) : (mapped == null ? Cell.PENDING_NULL : mapped.getCell(-(i + 1))))
                            .collect(Collectors.toList());
                    return new Row(newCells, row.flagged, row.starred);
                } else {
                    return negativeRowMap(row);
                }
            }

            private Row negativeRowMap(Row row) {
                List<Cell> newCells = negativeIndexMap.stream()
                        .map(i -> i >= 0 ? row.getCell(i) : null)
                        .collect(Collectors.toList());
                return new Row(newCells, row.flagged, row.starred);
            }

            @Override
            public boolean preservesRecordStructure() {
                int keyIndex = columnModel.getKeyColumnIndex();
                return positiveIndexMap.size() <= keyIndex || positiveIndexMap.get(keyIndex) == keyIndex;
            }

        };
    }

    protected static RowInRecordChangeDataJoiner defaultJoiner(RowInRecordMapper negativeMapper) {
        return new RowInRecordChangeDataJoiner() {

            private static final long serialVersionUID = -7468515718356773265L;

            @Override
            public Row call(Record record, Row row, IndexedData<Row> indexedData) {
                if (indexedData.getData() != null) {
                    return indexedData.getData();
                } else {
                    return negativeMapper.call(record, indexedData.getId(), row);
                }
            }

            @Override
            public boolean preservesRecordStructure() {
                return false;
            }

        };
    }

    protected static RowInRecordMapper mapperWithInsertions(RowInRecordMapper mapper, List<Integer> indexMap) {
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
