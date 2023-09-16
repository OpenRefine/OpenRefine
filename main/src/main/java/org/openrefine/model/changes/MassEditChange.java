
package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.util.StringUtils;

public class MassEditChange extends RowMapChange {

    protected final Evaluable _evaluable;
    protected final String _columnName;
    protected final Map<String, Serializable> _fromTo;
    protected final Serializable _fromBlankTo;
    protected final Serializable _fromErrorTo;

    public MassEditChange(
            EngineConfig engineConfig,
            Evaluable evaluable,
            String columnName,
            Map<String, Serializable> fromTo,
            Serializable fromBlankTo,
            Serializable fromErrorTo) {
        super(engineConfig);
        _evaluable = evaluable;
        _columnName = columnName;
        _fromTo = fromTo;
        _fromBlankTo = fromBlankTo;
        _fromErrorTo = fromErrorTo;
    }

    @Override
    public RowMapper getPositiveRowMapper(GridState state) throws DoesNotApplyException {
        int columnIdx = columnIndex(state.getColumnModel(), _columnName);
        return mapper(columnIdx, _evaluable, _columnName, _fromTo, _fromBlankTo, _fromErrorTo);
    }

    private static RowMapper mapper(int columnIdx, Evaluable evaluable, String columnName,
            Map<String, Serializable> fromTo, Serializable fromBlankTo, Serializable fromErrorTo) {
        return new RowMapper() {

            private static final long serialVersionUID = 6383816657756293719L;

            @Override
            public Row call(long rowIndex, Row row) {
                Cell cell = row.getCell(columnIdx);
                Cell newCell = cell;

                Properties bindings = ExpressionUtils.createBindings();
                ExpressionUtils.bind(bindings, null, row, rowIndex, columnName, cell);

                Object v = evaluable.evaluate(bindings);
                if (ExpressionUtils.isError(v)) {
                    if (fromErrorTo != null) {
                        newCell = new Cell(fromErrorTo, (cell != null) ? cell.recon : null);
                    }
                } else if (ExpressionUtils.isNonBlankData(v)) {
                    String from = StringUtils.toString(v);
                    Serializable to = fromTo.get(from);
                    if (to != null) {
                        newCell = new Cell(to, (cell != null) ? cell.recon : null);
                    }
                } else {
                    if (fromBlankTo != null) {
                        newCell = new Cell(fromBlankTo, (cell != null) ? cell.recon : null);
                    }
                }
                return row.withCell(columnIdx, newCell);
            }

        };
    }

    @Override
    public boolean isImmediate() {
        return true;
    }

}
