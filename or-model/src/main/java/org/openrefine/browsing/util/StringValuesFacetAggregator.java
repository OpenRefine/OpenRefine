
package org.openrefine.browsing.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.openrefine.browsing.RecordFilter;
import org.openrefine.browsing.RowFilter;
import org.openrefine.browsing.facets.FacetAggregator;
import org.openrefine.browsing.filters.AllRowsRecordFilter;
import org.openrefine.browsing.filters.AnyRowRecordFilter;
import org.openrefine.browsing.filters.ExpressionEqualRowFilter;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Row;
import org.openrefine.util.StringUtils;

public class StringValuesFacetAggregator implements FacetAggregator<StringValuesFacetState> {

    private static final long serialVersionUID = 1L;

    protected final ColumnModel _columnModel;
    protected final int _cellIndex;
    protected final Evaluable _evaluable;
    protected final Set<String> _selected;
    protected final boolean _selectBlanks;
    protected final boolean _selectErrors;
    protected final boolean _invert;

    /**
     * Constructor.
     * 
     * @param columnModel
     *            the list of columns of the table where this facet is being applied
     * @param cellIndex
     *            the index of the base column where the evaluable is run
     * @param evaluable
     *            the evaluable which generates the values held by this facet
     * @param selected
     *            the list of string values which are selected
     * @param selectBlanks
     *            whether blanks should be selected
     * @param selectErrors
     *            whether errors should be selected
     * @param invert
     *            whether the selection should be inverted
     */
    public StringValuesFacetAggregator(
            ColumnModel columnModel, int cellIndex, Evaluable evaluable,
            Set<String> selected, boolean selectBlanks, boolean selectErrors, boolean invert) {
        _columnModel = columnModel;
        _cellIndex = cellIndex;
        _evaluable = evaluable;
        _selected = selected;
        _selectBlanks = selectBlanks;
        _selectErrors = selectErrors;
        _invert = invert;
    }

    @Override
    public StringValuesFacetState sum(StringValuesFacetState first, StringValuesFacetState second) {
        Map<String, Long> newCounts = new HashMap<>(first.getCounts());
        second.getCounts().entrySet().forEach(e -> {
            if (newCounts.containsKey(e.getKey())) {
                newCounts.put(e.getKey(), e.getValue() + newCounts.get(e.getKey()));
            } else {
                newCounts.put(e.getKey(), e.getValue());
            }
        });
        return new StringValuesFacetState(
                newCounts,
                first.getErrorCount() + second.getErrorCount(),
                first.getBlankCount() + second.getBlankCount());
    }

    @Override
    public StringValuesFacetState withRow(StringValuesFacetState state, long rowId, Row row) {
        // Evaluate the expression on that row
        Object value = evaluateOnRow(rowId, row);
        if (ExpressionUtils.isError(value)) {
            return new StringValuesFacetState(
                    state.getCounts(), state.getErrorCount() + 1, state.getBlankCount());
        } else if (ExpressionUtils.isNonBlankData(value)) {
            String valueStr = StringUtils.toString(value);
            Map<String, Long> newCounts = new HashMap<>(state.getCounts());
            if (newCounts.containsKey(valueStr)) {
                newCounts.put(valueStr, newCounts.get(valueStr) + 1);
            } else {
                newCounts.put(valueStr, 1L);
            }
            return new StringValuesFacetState(
                    newCounts, state.getErrorCount(), state.getBlankCount());
        } else {
            return new StringValuesFacetState(
                    state.getCounts(), state.getErrorCount(), state.getBlankCount() + 1);
        }
    }

    protected Object evaluateOnRow(long rowId, Row row) {
        Properties bindings = ExpressionUtils.createBindings();
        ExpressionUtils.bind(bindings, _columnModel, row, rowId, null, row.getCell(_cellIndex));
        return _evaluable.evaluate(bindings);
    }

    @Override
    public RowFilter getRowFilter() {
        return _evaluable == null ||
                (_selected.size() == 0 && !_selectBlanks && !_selectErrors) ? null
                        : new ExpressionEqualRowFilter(
                                _evaluable,
                                _columnModel.getColumns().get(_cellIndex).getName(),
                                _cellIndex,
                                _selected,
                                _selectBlanks,
                                _selectErrors,
                                _invert);
    }

    @Override
    public RecordFilter getRecordFilter() {
        RowFilter rowFilter = getRowFilter();
        if (rowFilter == null) {
            return null;
        }
        return _invert ? new AllRowsRecordFilter(rowFilter) : new AnyRowRecordFilter(rowFilter);
    }

}
