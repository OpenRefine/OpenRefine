
package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Record;
import org.openrefine.model.RecordMapper;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.RowScanMapper;

/**
 * Transforms a table without a record structure to blanking out values which are identical to those on the previous
 * row, creating a record structure.
 * 
 * @author Antonin Delpeuch
 *
 */
public class BlankDownChange extends EngineDependentChange {

    protected final String _columnName;

    @JsonCreator
    public BlankDownChange(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName) {
        super(engineConfig);
        _columnName = columnName;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @Override
    public GridState apply(GridState state) throws DoesNotApplyException {
        ColumnModel model = state.getColumnModel();
        int index = model.getColumnIndexByName(_columnName);
        if (index == -1) {
            throw new DoesNotApplyException(
                    String.format("Column '%s' does not exist", _columnName));
        }
        Engine engine = getEngine(state);
        if (Mode.RecordBased.equals(_engineConfig.getMode())) {
            // Simple map of records
            return state.mapRecords(
                    RecordMapper.conditionalMapper(engine.combinedRecordFilters(), recordMapper(index), RecordMapper.IDENTITY),
                    model);

        } else {
            // We need to remember the cell from the previous row, so we use a scan map
            return state.mapRows(RowScanMapper.conditionalMapper(engine.combinedRowFilters(), rowScanMapper(index), RowMapper.IDENTITY),
                    model);
        }
    }

    protected static RecordMapper recordMapper(int columnIndex) {
        return new RecordMapper() {

            private static final long serialVersionUID = -5754924505312738966L;

            @Override
            public List<Row> call(Record record) {
                Cell lastCell = null;
                List<Row> result = new LinkedList<>();
                for (Row row : record.getRows()) {
                    Serializable cellValue = row.getCellValue(columnIndex);
                    if (lastCell != null
                            && ExpressionUtils.isNonBlankData(cellValue)
                            && cellValue.equals(lastCell.getValue())) {
                        result.add(row.withCell(columnIndex, null));
                    } else {
                        result.add(row);
                    }
                    lastCell = row.getCell(columnIndex);
                }
                return result;
            }

        };
    }

    protected static RowScanMapper<Cell> rowScanMapper(int columnIndex) {
        return new RowScanMapper<Cell>() {

            private static final long serialVersionUID = 2808768242505893380L;

            @Override
            public Cell feed(long rowId, Row row) {
                return row.getCell(columnIndex);
            }

            @Override
            public Cell combine(Cell left, Cell right) {
                if (right != null && right.value == null) {
                    // Cell.NULL is used as sentinel, for rows that are skipped by facets.
                    // null cells are simply represented by right == null
                    return left;
                } else {
                    return right;
                }
            }

            @Override
            public Cell unit() {
                return Cell.NULL;
            }

            @Override
            public Row map(Cell lastCell, long rowId, Row row) {
                Serializable cellValue = row.getCellValue(columnIndex);
                if (ExpressionUtils.isNonBlankData(cellValue)
                        && lastCell != null
                        && cellValue.equals(lastCell.getValue())) {
                    return row.withCell(columnIndex, null);
                } else {
                    return row;
                }
            }

        };
    }

    @Override
    public boolean isImmediate() {
        return true;
    }

}
