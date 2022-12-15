
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.recon.DataExtensionReconConfig;
import org.openrefine.model.recon.ReconType;
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

    @JsonCreator
    public DataExtensionChange(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("baseColumnName") String baseColumnName,
            @JsonProperty("endpoint") String endpoint,
            @JsonProperty("identifierSpace") String identifierSpace,
            @JsonProperty("schemaSpace") String schemaSpace,
            @JsonProperty("columnInsertIndex") int columnInsertIndex,
            @JsonProperty("columnNames") List<String> columnNames,
            @JsonProperty("columnTypes") List<ReconType> columnTypes) {
        super(engineConfig);
        _baseColumnName = baseColumnName;
        _endpoint = endpoint;
        _identifierSpace = identifierSpace;
        _schemaSpace = schemaSpace;
        _columnInsertIndex = columnInsertIndex;
        _columnNames = columnNames;
        _columnTypes = columnTypes;
    }

    @Override
    public GridState apply(GridState projectState, ChangeContext context) throws DoesNotApplyException {
        ChangeData<RecordDataExtension> changeData;
        try {
            changeData = context.getChangeData("extend", new DataExtensionSerializer());
        } catch (IOException e) {
            throw new DoesNotApplyException(String.format("Unable to retrieve change data for data extension"));
        }
        int baseColumnId = projectState.getColumnModel().getColumnIndexByName(_baseColumnName);
        if (baseColumnId == -1) {
            throw new ColumnNotFoundException(_baseColumnName);
        }
        ColumnModel newColumnModel = projectState.getColumnModel();
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
        GridState state = projectState.join(changeData, joiner, newColumnModel);

        return state;
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
        public List<Row> call(Record record, RecordDataExtension changeData) {
            List<Row> newRows = new ArrayList<>();
            List<Row> oldRows = record.getRows();
            // the changeData object can be null, for instance on rows excluded by facets
            Map<Long, DataExtension> extensions = changeData != null ? changeData.getExtensions() : Collections.emptyMap();

            for (int i = 0; i != oldRows.size(); i++) {
                Row row = oldRows.get(i);
                long rowId = record.getStartRowId() + i;
                DataExtension extension = extensions.get(rowId);
                if (extension == null || extension.data.isEmpty()) {
                    newRows.add(row.insertCells(columnInsertId, Collections.nCopies(nbInsertedColumns, null)));
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

    }

}
