
package org.openrefine.operations;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.RowMapChange;

/**
 * Base class for an operation which evaluates an expression on rows or records, and then uses it to derive the new
 * grid.
 * 
 * Depending on whether the expresion is local or not, the operation will return an immediate change or create a
 * long-running process which evaluates the expression and stores the results in a {@link ChangeData} object, so that
 * expensive evaluations (or expressions with side effects) are not evaluated multiple times.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class ExpressionBasedOperation extends EngineDependentOperation {

    protected final String _expression;
    protected final String _baseColumnName;
    protected final OnError _onError;

    protected ExpressionBasedOperation(EngineConfig engineConfig, String expression, String baseColumnName, OnError onError) {
        super(engineConfig);
        _expression = expression;
        _baseColumnName = baseColumnName;
        _onError = onError;
    }

    /**
     * Returns the mapper to apply to all matching rows.
     * 
     * @param state
     * @return
     * @throws DoesNotApplyException
     */
    protected abstract RowInRecordMapper getPositiveRowMapper(Grid state, ChangeContext context, Evaluable eval)
            throws DoesNotApplyException;

    protected RowInRecordMapper getNegativeRowMapper(Grid state, ChangeContext context, Evaluable eval) throws DoesNotApplyException {
        return RowInRecordMapper.IDENTITY;
    }

    protected ColumnModel getNewColumnModel(Grid state, ChangeContext context, Evaluable eval) throws DoesNotApplyException {
        return state.getColumnModel();
    }

    protected int columnIndex(ColumnModel model, String columnName, Evaluable eval) throws DoesNotApplyException {
        return RowMapChange.columnIndex(model, columnName);
    }

    protected Grid postTransform(Grid state, ChangeContext context, Evaluable eval) {
        return state;
    }

    @Override
    public Change createChange() {
        Evaluable eval;
        try {
            eval = MetaParser.parse(_expression);
        } catch (ParsingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        // if the evaluator is a pure function which only reads data from the current row/record
        if (eval.isLocal()) {
            return createChange(eval);
        } else {
            return getChangeForNonLocalExpression("eval", eval);
        }
    }

    public Change createChange(Evaluable eval) {
        return new RowMapChange(getEngineConfig()) {

            @Override
            public RowInRecordMapper getPositiveRowMapper(Grid state, ChangeContext context) throws DoesNotApplyException {
                return ExpressionBasedOperation.this.getPositiveRowMapper(state, context, eval);
            }

            @Override
            public RowInRecordMapper getNegativeRowMapper(Grid state, ChangeContext context) throws DoesNotApplyException {
                return ExpressionBasedOperation.this.getNegativeRowMapper(state, context, eval);
            }

            @Override
            public ColumnModel getNewColumnModel(Grid state, ChangeContext context) throws DoesNotApplyException {
                return ExpressionBasedOperation.this.getNewColumnModel(state, context, eval);
            }

            @Override
            public Grid postTransform(Grid state, ChangeContext context) {
                return ExpressionBasedOperation.this.postTransform(state, context, eval);
            }

            @Override
            public boolean isImmediate() {
                return true;
            }

        };
    }

    protected abstract Change getChangeForNonLocalExpression(String changeDataId, Evaluable evaluable);

}
