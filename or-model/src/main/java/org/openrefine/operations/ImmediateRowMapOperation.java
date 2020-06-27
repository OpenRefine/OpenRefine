
package org.openrefine.operations;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.RowMapChange;

/**
 * Simplifies the architecture for immediate operations which simply act by mapping rows. This allows the mapper to be
 * defined in a method of the operation, hiding the construction of the {@link Change} away.
 * 
 * @author Antonin Delpeuch
 *
 */
abstract public class ImmediateRowMapOperation extends EngineDependentOperation {

    protected ImmediateRowMapOperation(EngineConfig engineConfig) {
        super(engineConfig);
    }

    /**
     * Returns the mapper to apply to all matching rows.
     * 
     * @param state
     * @return
     * @throws DoesNotApplyException
     */
    protected abstract RowMapper getPositiveRowMapper(GridState state, ChangeContext context) throws DoesNotApplyException;

    protected RowMapper getNegativeRowMapper(GridState state, ChangeContext context) throws DoesNotApplyException {
        return RowMapper.IDENTITY;
    }

    protected ColumnModel getNewColumnModel(GridState state, ChangeContext context) throws DoesNotApplyException {
        return state.getColumnModel();
    }

    protected int columnIndex(ColumnModel model, String columnName) throws DoesNotApplyException {
        return RowMapChange.columnIndex(model, columnName);
    }

    protected GridState postTransform(GridState state, ChangeContext context) {
        return state;
    }

    @Override
    public Change createChange() {
        return new RowMapChange(getEngineConfig()) {

            @Override
            public RowMapper getPositiveRowMapper(GridState state, ChangeContext context) throws DoesNotApplyException {
                return ImmediateRowMapOperation.this.getPositiveRowMapper(state, context);
            }

            @Override
            public RowMapper getNegativeRowMapper(GridState state, ChangeContext context) throws DoesNotApplyException {
                return ImmediateRowMapOperation.this.getNegativeRowMapper(state, context);
            }

            @Override
            public ColumnModel getNewColumnModel(GridState state, ChangeContext context) throws DoesNotApplyException {
                return ImmediateRowMapOperation.this.getNewColumnModel(state, context);
            }

            @Override
            public GridState postTransform(GridState state, ChangeContext context) {
                return ImmediateRowMapOperation.this.postTransform(state, context);
            }

            @Override
            public boolean isImmediate() {
                return true;
            }
        };
    }
}
