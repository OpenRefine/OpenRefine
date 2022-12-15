
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.Engine;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.ModelException;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.recon.ReconConfig;

/**
 * Adds a new column based on data fetched from an external process. If no column name is supplied, then the change will
 * replace the column at the given index instead (merging with existing contents in rows not covered by the change
 * data).
 * 
 * New recon config and stats can be supplied for the column changed or created. If a recon config and no recon stats
 * are provided, the change computes the new recon stats on the fly.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ColumnChangeByChangeData implements Change {

    private final String _changeDataId;
    private final int _columnIndex;
    private final String _columnName;
    private final Engine.Mode _engineMode;
    private final ReconConfig _reconConfig;

    @JsonCreator
    public ColumnChangeByChangeData(
            @JsonProperty("changeDataId") String changeDataId,
            @JsonProperty("columnIndex") int columnIndex,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("mode") Engine.Mode mode,
            @JsonProperty("reconConfig") ReconConfig reconConfig) {
        _changeDataId = changeDataId;
        _columnIndex = columnIndex;
        _columnName = columnName;
        _engineMode = mode;
        _reconConfig = reconConfig;
    }

    @JsonProperty("changeDataId")
    public String getChangeDataId() {
        return _changeDataId;
    }

    @JsonProperty("columnIndex")
    public int getColumnIndex() {
        return _columnIndex;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("mode")
    public Engine.Mode getMode() {
        return _engineMode;
    }

    @JsonProperty("reconConfig")
    public ReconConfig getReconConfig() {
        return _reconConfig;
    }

    @Override
    public GridState apply(GridState projectState, ChangeContext context) throws DoesNotApplyException {
        ColumnModel columnModel = projectState.getColumnModel();
        if (_columnName != null) {
            ColumnMetadata column = new ColumnMetadata(_columnName)
                    .withReconConfig(_reconConfig);
            try {
                columnModel = projectState.getColumnModel().insertColumn(_columnIndex, column);
            } catch (ModelException e) {
                throw new Change.DoesNotApplyException(
                        String.format("A column with name '{}' cannot be added as the name conflicts with an existing column",
                                _columnName));
            }
        } else if (_reconConfig != null) {
            columnModel = columnModel
                    .withReconConfig(_columnIndex, _reconConfig);
        }

        Joiner joiner = new Joiner(_columnIndex, _columnName != null);

        GridState joined;
        if (Engine.Mode.RowBased.equals(_engineMode)) {
            ChangeData<Cell> changeData = null;
            try {
                changeData = context.getChangeData(_changeDataId, new CellChangeDataSerializer());
            } catch (IOException e) {
                throw new DoesNotApplyException(String.format("Unable to retrieve change data '%s'", _changeDataId));
            }
            joined = projectState.join(changeData, joiner, columnModel);
        } else {
            ChangeData<List<Cell>> changeData = null;
            try {
                changeData = context.getChangeData(_changeDataId, new CellListChangeDataSerializer());
            } catch (IOException e) {
                throw new DoesNotApplyException(String.format("Unable to retrieve change data '%s'", _changeDataId));
            }
            joined = projectState.join(changeData, joiner, columnModel);
        }
        return joined;
    }

    @Override
    public boolean isImmediate() {
        return false;
    }

    public static class Joiner implements RowChangeDataJoiner<Cell>, RecordChangeDataJoiner<List<Cell>> {

        private static final long serialVersionUID = 8332780210267820528L;
        private final int _columnIndex;
        private final boolean _add;

        public Joiner(int columnIndex, boolean add) {
            _columnIndex = columnIndex;
            _add = add;
        }

        @Override
        public Row call(long rowId, Row row, Cell cell) {
            if (_add) {
                return row.insertCell(_columnIndex, cell);
            } else {
                if (cell != null) {
                    return row.withCell(_columnIndex, cell);
                } else {
                    return row;
                }
            }
        }

        @Override
        public List<Row> call(Record record, List<Cell> changeData) {
            List<Row> rows = record.getRows();
            List<Row> result = new ArrayList<>(rows.size());
            if (rows.size() != changeData.size()) {
                throw new IllegalArgumentException(
                        String.format("Change data and record do not have the same size at row %d", record.getStartRowId()));
            }
            for (int i = 0; i != rows.size(); i++) {
                result.add(call(record.getStartRowId() + i, rows.get(i), changeData.get(i)));
            }
            return result;
        }

    }

}
