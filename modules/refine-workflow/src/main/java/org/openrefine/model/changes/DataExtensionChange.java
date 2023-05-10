
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.*;
import org.openrefine.model.Record;
import org.openrefine.model.recon.DataExtensionReconConfig;
import org.openrefine.model.recon.ReconType;
import org.openrefine.model.recon.ReconciledDataExtensionJob;
import org.openrefine.model.recon.ReconciledDataExtensionJob.DataExtension;
import org.openrefine.model.recon.ReconciledDataExtensionJob.RecordDataExtension;
import org.openrefine.util.ParsingUtilities;

public class DataExtensionChange extends EngineDependentChange {

    @JsonProperty("baseColumnName")
    protected final String _baseColumnName;
    @JsonProperty("endpoint")
    protected final String _endpoint;
    @JsonProperty("identifierSpace")
    protected final String _identifierSpace;
    @JsonProperty("schemaSpace")
    private final String _schemaSpace;
    @JsonProperty("columnInsertIndex")
    private final int _columnInsertIndex;
    @JsonProperty("columnNames")
    private final List<String> _columnNames;
    @JsonProperty("columnTypes")
    private final List<ReconType> _columnTypes;
    @JsonProperty("extension")
    private final ReconciledDataExtensionJob.DataExtensionConfig _extension;

    @JsonCreator
    public DataExtensionChange(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("baseColumnName") String baseColumnName,
            @JsonProperty("endpoint") String endpoint,
            @JsonProperty("identifierSpace") String identifierSpace,
            @JsonProperty("schemaSpace") String schemaSpace,
            @JsonProperty("columnInsertIndex") int columnInsertIndex,
            @JsonProperty("columnNames") List<String> columnNames,
            @JsonProperty("columnTypes") List<ReconType> columnTypes,
            @JsonProperty("extension") ReconciledDataExtensionJob.DataExtensionConfig extension) {
        super(engineConfig);
        _baseColumnName = baseColumnName;
        _endpoint = endpoint;
        _identifierSpace = identifierSpace;
        _schemaSpace = schemaSpace;
        _columnInsertIndex = columnInsertIndex;
        _columnNames = columnNames;
        _columnTypes = columnTypes;
        _extension = extension;
    }

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws DoesNotApplyException {
        /**
         * This operation does not always respect the rows mode, because when fetching multiple values for the same row,
         * the extra values are spread in the record of the given row. Therefore, the fetching is done in records mode
         * at all times, but in rows mode we also pass down the row filter to the fetcher so that it can filter out rows
         * that should not be fetched inside a given record.
         */

        Engine engine = new Engine(projectState, _engineConfig, 1234L);
        RowFilter rowFilter = RowFilter.ANY_ROW;
        if (Engine.Mode.RowBased.equals(engine.getMode())) {
            rowFilter = engine.combinedRowFilters();
        }
        int baseColumnId = projectState.getColumnModel().getColumnIndexByName(_baseColumnName);
        if (baseColumnId == -1) {
            throw new ColumnNotFoundException(_baseColumnName);
        }
        ReconciledDataExtensionJob job = new ReconciledDataExtensionJob(_extension, _endpoint, _identifierSpace, _schemaSpace);
        DataExtensionProducer producer = new DataExtensionProducer(job, baseColumnId, rowFilter);
        Function<Optional<ChangeData<RecordDataExtension>>, ChangeData<RecordDataExtension>> changeDataCompletion = incompleteChangeData -> projectState
                .mapRecords(engine.combinedRecordFilters(), producer, incompleteChangeData);

        ChangeData<RecordDataExtension> changeData;
        try {
            changeData = context.getChangeData("extend", new DataExtensionSerializer(), changeDataCompletion);
        } catch (IOException e) {
            throw new DoesNotApplyException(String.format("Unable to retrieve change data for data extension"));
        }

        ColumnModel newColumnModel = projectState.getColumnModel().withHasRecords(true);
        for (int i = 0; i != _columnNames.size(); i++) {
            newColumnModel = newColumnModel.insertUnduplicatedColumn(
                    _columnInsertIndex + i,
                    new ColumnMetadata(_columnNames.get(i), _columnNames.get(i), new DataExtensionReconConfig(
                            _endpoint,
                            _identifierSpace,
                            _schemaSpace,
                            _columnTypes.get(i))));
        }
        RecordChangeDataJoiner<RecordDataExtension> joiner = new DataExtensionJoiner(baseColumnId, _columnInsertIndex, _columnNames.size());
        Grid state = projectState.join(changeData, joiner, newColumnModel);

        return new ChangeResult(state, GridPreservation.NO_ROW_PRESERVATION);
    }

    @Override
    public boolean isImmediate() {
        return false;
    }

    public static class DataExtensionSerializer implements ChangeDataSerializer<RecordDataExtension> {

        private static final long serialVersionUID = -8334190917198142840L;

        @Override
        public String serialize(RecordDataExtension changeDataItem) {
            try {
                return ParsingUtilities.saveWriter.writeValueAsString(changeDataItem);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Cell serialization failed", e);
            }
        }

        @Override
        public RecordDataExtension deserialize(String serialized) throws IOException {
            return ParsingUtilities.mapper.readValue(serialized, RecordDataExtension.class);
        }

    }

    protected static class DataExtensionJoiner implements RecordChangeDataJoiner<RecordDataExtension> {

        private static final long serialVersionUID = 8991393046204795332L;
        private final int baseColumnId;
        private final int columnInsertId;
        private final int nbInsertedColumns;

        protected DataExtensionJoiner(int baseColumnId, int columnInsertId, int nbInsertedColumns) {
            this.baseColumnId = baseColumnId;
            this.columnInsertId = columnInsertId;
            this.nbInsertedColumns = nbInsertedColumns;
        }

        @Override
        public List<Row> call(Record record, IndexedData<RecordDataExtension> indexedData) {
            RecordDataExtension changeData = indexedData.getData();
            List<Row> newRows = new ArrayList<>();
            List<Row> oldRows = record.getRows();
            // the changeData object can be null, for instance on rows excluded by facets
            Map<Long, DataExtension> extensions = changeData != null ? changeData.getExtensions() : Collections.emptyMap();

            for (int i = 0; i != oldRows.size(); i++) {
                Row row = oldRows.get(i);
                long rowId = record.getStartRowId() + i;
                DataExtension extension = extensions.get(rowId);
                if (extension == null || extension.data.isEmpty()) {
                    newRows.add(row.insertCells(columnInsertId,
                            Collections.nCopies(nbInsertedColumns, indexedData.isPending() ? Cell.PENDING_NULL : null)));
                    continue;
                }

                int origRow = i;
                for (List<Cell> extensionRow : extension.data) {
                    Row newRow;
                    if (origRow == i || (origRow < oldRows.size() && oldRows.get(origRow).isCellBlank(baseColumnId))) {
                        newRow = oldRows.get(origRow);
                        origRow++;
                    } else {
                        newRow = new Row(Collections.nCopies(row.getCells().size(), null));
                    }
                    List<Cell> insertedCells = extensionRow;
                    if (insertedCells.size() != nbInsertedColumns) {
                        insertedCells = new ArrayList<>(extensionRow);
                        insertedCells.addAll(Collections.nCopies(nbInsertedColumns - insertedCells.size(), null));
                    }
                    newRows.add(newRow.insertCells(columnInsertId, extensionRow));
                }
            }
            return newRows;
        }

        @Override
        public boolean preservesRecordStructure() {
            return false;
        }

    }

    public static class DataExtensionProducer implements RecordChangeDataProducer<RecordDataExtension> {

        private static final long serialVersionUID = -7946297987163653933L;
        private final ReconciledDataExtensionJob _job;
        private final int _cellIndex;
        private final RowFilter _rowFilter;

        public DataExtensionProducer(ReconciledDataExtensionJob job, int cellIndex, RowFilter rowFilter) {
            _job = job;
            _cellIndex = cellIndex;
            _rowFilter = rowFilter;
        }

        @Override
        public RecordDataExtension call(Record record) {
            return callRecordBatch(Collections.singletonList(record)).get(0);
        }

        @Override
        public List<RecordDataExtension> callRecordBatch(List<Record> records) {

            Set<String> ids = new HashSet<>();

            for (Record record : records) {
                for (IndexedRow indexedRow : record.getIndexedRows()) {
                    Row row = indexedRow.getRow();
                    if (!_rowFilter.filterRow(indexedRow.getIndex(), row)) {
                        continue;
                    }
                    Cell cell = row.getCell(_cellIndex);
                    if (cell != null && cell.recon != null && cell.recon.match != null) {
                        ids.add(cell.recon.match.id);
                    }
                }
            }

            Map<String, DataExtension> extensions;
            try {
                extensions = _job.extend(ids);
            } catch (Exception e) {
                e.printStackTrace();
                extensions = Collections.emptyMap();
            }

            List<RecordDataExtension> results = new ArrayList<>();
            for (Record record : records) {
                Map<Long, DataExtension> recordExtensions = new HashMap<>();
                for (IndexedRow indexedRow : record.getIndexedRows()) {
                    if (!_rowFilter.filterRow(indexedRow.getIndex(), indexedRow.getRow())) {
                        continue;
                    }
                    Cell cell = indexedRow.getRow().getCell(_cellIndex);
                    if (cell != null && cell.recon != null && cell.recon.match != null) {
                        recordExtensions.put(indexedRow.getIndex(), extensions.get(cell.recon.match.id));
                    }
                }
                results.add(new RecordDataExtension(recordExtensions));
            }
            return results;
        }

        @Override
        public int getBatchSize() {
            return _job.getBatchSize();
        }

    }

}
