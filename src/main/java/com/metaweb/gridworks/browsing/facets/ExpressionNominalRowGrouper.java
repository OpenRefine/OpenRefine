package com.metaweb.gridworks.browsing.facets;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.metaweb.gridworks.browsing.DecoratedValue;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

/**
 * Visit matched rows and group them into facet choices based on the values computed
 * from a given expression.
 */
public class ExpressionNominalRowGrouper implements RowVisitor {
    /*
     * Configuration
     */
    final protected Evaluable   _evaluable;
    final protected String      _columnName;
    final protected int         _cellIndex;
    
    /*
     * Computed results
     */
    final public Map<Object, NominalFacetChoice> choices = new HashMap<Object, NominalFacetChoice>();
    public int blankCount = 0;
    public int errorCount = 0;
    
    public ExpressionNominalRowGrouper(Evaluable evaluable, String columnName, int cellIndex) {
        _evaluable = evaluable;
        _columnName = columnName;
        _cellIndex = cellIndex;
    }
    
    public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
        Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);

        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
        
        Object value = _evaluable.evaluate(bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    processValue(v);
                }
                return false;
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    processValue(v);
                }
                return false;
            } // else, fall through
        }
        
        processValue(value);
        return false;
    }
    
    protected void processValue(Object value) {
        if (ExpressionUtils.isError(value)) {
            errorCount++;
        } else if (ExpressionUtils.isNonBlankData(value)) {
            String valueString = value.toString();
            String label = value.toString();
            
            DecoratedValue dValue = new DecoratedValue(value, label);
            
            if (choices.containsKey(valueString)) {
                choices.get(valueString).count++;
            } else {
                NominalFacetChoice choice = new NominalFacetChoice(dValue);
                choice.count = 1;
                
                choices.put(valueString, choice);
            }
        } else {
            blankCount++;
        }
    }
}
