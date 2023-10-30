
package org.openrefine.operations;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.expr.WrappedCell;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnInsertion;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Record;
import org.openrefine.model.RecordMapper;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.overlay.OverlayModel;

/**
 * Base class for an operation which evaluates an expression on rows or records, and then uses it to derive the new
 * grid.
 * 
 * Depending on whether the expression is local or not, the operation will compute it lazily (with a {@link RowMapper}
 * or {@link RecordMapper}) or will evaluate it eagerly and store the evaluation results in a {@link ChangeData} object,
 * so that expensive evaluations (or expressions with side effects) are not evaluated multiple times.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class ExpressionBasedOperation extends RowMapOperation {

    protected final String _expression;
    protected final String _baseColumnName;
    protected final OnError _onError;
    protected final int _repeatCount;
    protected String _changeDataId = "eval";
    protected boolean _forceEagerEvaluation = false;

    // initialized lazily
    protected Evaluable _eval = null;
    protected List<String> _orderedDependencies = null;

    protected ExpressionBasedOperation(EngineConfig engineConfig, String expression, String baseColumnName, OnError onError,
            int repeatCount) {
        super(engineConfig);
        _expression = expression;
        _baseColumnName = baseColumnName;
        _onError = onError;
        _repeatCount = repeatCount;
    }

    // TODO to be handled by RowMapOperation instead
    protected abstract boolean preservesRecordStructure(ColumnModel columnModel);

    /**
     * Returns the new column model after the operation has run.
     */
    protected ColumnModel getNewColumnModel(ColumnModel columnModel, Map<String, OverlayModel> overlayModels, ChangeContext context,
            Evaluable eval) throws OperationException {
        return columnModel;
    }

    /**
     * Returns the new overlay models after this change is applied.
     */
    protected Map<String, OverlayModel> getNewOverlayModels(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            ChangeContext context, Evaluable evaluable)
            throws OperationException {
        return overlayModels;
    }

    protected Evaluable getEvaluable() throws OperationException {
        if (_eval == null) {
            try {
                _eval = MetaParser.parse(_expression);
            } catch (ParsingException e) {
                throw new OperationException("parsing", e.getMessage(), e);
            }
        }
        return _eval;
    }

    @Override
    public List<String> getColumnDependencies() {
        if (_orderedDependencies == null) {
            try {
                Set<String> columnDependencies = getEvaluable().getColumnDependencies(_baseColumnName);
                _orderedDependencies = columnDependencies == null ? null
                        : columnDependencies
                                .stream()
                                .collect(Collectors.toList());
            } catch (OperationException e) {
                // to make sure that we can re-serialize an operation even if its expression could not be parsed
                _orderedDependencies = null;
            }
        }
        return _orderedDependencies;
    }

    @Override
    protected ColumnModel getNewColumnModel(ColumnModel columnModel, Map<String, OverlayModel> overlayModels, ChangeContext context)
            throws OperationException {
        Validate.notNull(_eval);
        return getNewColumnModel(columnModel, overlayModels, context, _eval);
    }

    @Override
    protected RowInRecordMapper getPositiveRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            long estimatedRowCount, ChangeContext context) throws OperationException {
        Validate.notNull(_eval);
        boolean preservesRecordStructure = preservesRecordStructure(columnModel);
        List<ColumnInsertion> insertions = getColumnInsertions();
        boolean replaceMode = insertions != null && insertions.size() >= 1 && insertions.get(0).isReplace();
        return positiveRowMapper(_baseColumnName, _onError, _repeatCount, _eval,
                columnModel, overlayModels, context.getProjectId(), preservesRecordStructure, replaceMode);
    }

    @Override
    protected final RowInRecordMapper getNegativeRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            long estimatedRowCount, ChangeContext context) throws OperationException {
        boolean preservesRecordStructure = preservesRecordStructure(columnModel);
        return new NegativeRowMapper(preservesRecordStructure);
    }

    @Override
    protected final Map<String, OverlayModel> getNewOverlayModels(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            ChangeContext context) throws OperationException {
        Validate.notNull(_eval);
        return getNewOverlayModels(columnModel, overlayModels, context, _eval);
    }

    protected static class NegativeRowMapper extends RowInRecordMapper {

        private static final long serialVersionUID = -2790780327244338473L;
        boolean preservesRecordStructure;

        public NegativeRowMapper(boolean preservesRecordStructure) {
            this.preservesRecordStructure = preservesRecordStructure;
        }

        @Override
        public Row call(Record record, long rowId, Row row) {
            return new Row(Collections.singletonList(null), row.flagged, row.starred);
        }

        @Override
        public boolean preservesRecordStructure() {
            return preservesRecordStructure;
        }
    }

    public static RowInRecordMapper positiveRowMapper(
            String baseColumnName,
            OnError onError,
            int repeatCount,
            Evaluable eval,
            ColumnModel columnModel,
            Map<String, OverlayModel> overlayModels,
            long projectId,
            boolean preservesRecordStructure,
            boolean replaceMode) {
        int columnIndex = columnModel.getColumnIndexByName(baseColumnName);
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 276517207226047737L;

            @Override
            public Row call(Record translatedRecord, long rowId, Row translatedRow) {
                return new Row(Collections.singletonList(makeCell(translatedRecord, rowId, translatedRow)), translatedRow.flagged,
                        translatedRow.starred);
            }

            public Cell makeCell(Record translatedRecord, long rowId, Row translatedRow) {
                Cell cell = columnIndex == -1 ? null : translatedRow.getCell(columnIndex);
                Cell newCell = null;

                Properties bindings = new Properties();
                ExpressionUtils.bind(bindings, columnModel, translatedRow, rowId, translatedRecord,
                        baseColumnName, cell, overlayModels, projectId);
                // this should only happen when we are actually called by a row mapper and
                // not within the context of change data production
                if (ExpressionUtils.dependsOnPendingValues(eval, baseColumnName, columnModel, translatedRow, translatedRecord)) {
                    if (replaceMode) {
                        return new Cell(cell == null ? null : cell.value, null, true);
                    } else {
                        return Cell.PENDING_NULL;
                    }
                }

                Object o = eval.evaluate(bindings);
                if (o != null) {
                    if (o instanceof Cell) {
                        newCell = (Cell) o;
                    } else if (o instanceof WrappedCell) {
                        newCell = ((WrappedCell) o).cell;
                    } else {
                        Serializable v = ExpressionUtils.wrapStorable(o);
                        if (ExpressionUtils.isError(v)) {
                            if (onError == OnError.SetToBlank) {
                                return Cell.NULL;
                            } else if (onError == OnError.KeepOriginal) {
                                v = cell != null ? cell.value : null;
                            }
                        }

                        if (v != null) {
                            newCell = new Cell(v, null);
                        }

                        for (int i = 0; i < repeatCount; i++) {
                            ExpressionUtils.bind(bindings, null, translatedRow, rowId, translatedRecord, baseColumnName, newCell,
                                    overlayModels, projectId);

                            v = ExpressionUtils.wrapStorable(eval.evaluate(bindings));
                            if (ExpressionUtils.isError(v)) {
                                break;
                            } else if (ExpressionUtils.sameValue(newCell.value, v)) {
                                break;
                            }

                            newCell = new Cell(v, newCell.recon);
                        }
                    }
                }
                if (newCell == null) {
                    newCell = Cell.NULL;
                }
                return newCell;
            }

            @Override
            public boolean persistResults() {
                return !eval.isLocal();
            }

            @Override
            public boolean preservesRecordStructure() {
                return preservesRecordStructure;
            }

        };
    }

}
