
package org.openrefine.model.changes;

import java.util.LinkedList;
import java.util.List;

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
 * Transforms a table with a record structure to by spreading non-null values in the rows below, in a specific column.
 * 
 * @author Antonin Delpeuch
 *
 */
public class FillDownChange extends EngineDependentChange {

    protected final String _columnName;

    public FillDownChange(
            EngineConfig engineConfig,
            String columnName) {
        super(engineConfig);
        _columnName = columnName;
    }

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
            // simple map of records, since a filled down value cannot spread beyond a record boundary
            return state.mapRecords(
                    RecordMapper.conditionalMapper(engine.combinedRecordFilters(), recordMapper(index), RecordMapper.IDENTITY),
                    model);
        } else {
            // scan map, because we need to remember the last non-null cell
            return state.mapRows(RowScanMapper.conditionalMapper(engine.combinedRowFilters(), rowScanMapper(index), RowMapper.IDENTITY),
                    model);
        }
    }

    protected static RecordMapper recordMapper(int columnIndex) {
        return new RecordMapper() {

            private static final long serialVersionUID = -5754924505312738966L;

            @Override
            public List<Row> call(Record record) {
                Cell lastNonBlankCell = null;
                List<Row> result = new LinkedList<>();
                for (Row row : record.getRows()) {
                    if (row.isCellBlank(columnIndex)) {
                        result.add(row.withCell(columnIndex, lastNonBlankCell));
                    } else {
                        lastNonBlankCell = row.getCell(columnIndex);
                        result.add(row);
                    }
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
                if (right != null && ExpressionUtils.isNonBlankData(right.getValue())) {
                    return right;
                } else {
                    return left;
                }
            }

            @Override
            public Cell unit() {
                return Cell.NULL;
            }

            @Override
            public Row map(Cell lastNonBlankCell, long rowId, Row row) {
                if (!ExpressionUtils.isNonBlankData(row.getCellValue(columnIndex))) {
                    return row.withCell(columnIndex, lastNonBlankCell);
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
