package org.openrefine.browsing.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openrefine.browsing.filters.ExpressionEqualRowFilter;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.RowFilter;
import org.openrefine.util.StringUtils;

public class StringValuesFacetAggregator extends ExpressionValueFacetAggregator<StringValuesFacetState> {
    private static final long serialVersionUID = 1L;

    protected final ColumnModel  _columnModel;
    protected final int          _cellIndex;
    protected final Set<String>  _selected;
    protected final boolean      _selectBlanks;
    protected final boolean      _selectErrors;
    
    /**
     * Constructor.
     * 
     * @param columnModel
     *      the list of columns of the table where this facet is being applied
     * @param cellIndex
     *      the index of the base column where the evaluable is run
     * @param evaluable
     *      the evaluable which generates the values held by this facet
     * @param selected
     *      the list of string values which are selected
     * @param selectBlanks
     *      whether blanks should be selected
     * @param selectErrors
     *      whether errors should be selected
     * @param invert
     *      whether the selection should be inverted
     */
    public StringValuesFacetAggregator(
            ColumnModel columnModel, int cellIndex, RowEvaluable evaluable,
            Set<String> selected, boolean selectBlanks, boolean selectErrors, boolean invert) {
        super(invert, evaluable);
        _columnModel = columnModel;
        _cellIndex = cellIndex;
        _selected = selected;
        _selectBlanks = selectBlanks;
        _selectErrors = selectErrors;
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
    public RowFilter getRowFilter() {
        return _eval == null || 
                (_selected.size() == 0 && !_selectBlanks && !_selectErrors) ? 
                    null :
                    new ExpressionEqualRowFilter(
                        _eval, 
                        _columnModel.getColumns().get(_cellIndex).getName(),
                        _cellIndex, 
                        _selected, 
                        _selectBlanks, 
                        _selectErrors,
                        _invert);
    }

    @Override
    protected StringValuesFacetState withValue(StringValuesFacetState state, Object value, boolean inView) {
        if (!inView) {
            return state;
        }
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

}
