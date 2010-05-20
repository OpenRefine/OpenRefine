package com.metaweb.gridworks.browsing.facets;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.metaweb.gridworks.browsing.DecoratedValue;
import com.metaweb.gridworks.browsing.RecordVisitor;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;
import com.metaweb.gridworks.model.Row;

/**
 * Visit matched rows and group them into facet choices based on the values computed
 * from a given expression.
 */
public class ExpressionNominalRowGrouper implements RowVisitor, RecordVisitor {
	static public class IndexedNominalFacetChoice extends NominalFacetChoice {
		int _latestIndex;
		
		public IndexedNominalFacetChoice(DecoratedValue decoratedValue, int latestIndex) {
			super(decoratedValue);
			_latestIndex = latestIndex;
		}
	}
	
    /*
     * Configuration
     */
    final protected Evaluable   _evaluable;
    final protected String      _columnName;
    final protected int         _cellIndex;
    
    /*
     * Computed results
     */
    final public Map<Object, IndexedNominalFacetChoice> choices = new HashMap<Object, IndexedNominalFacetChoice>();
    public int blankCount = 0;
    public int errorCount = 0;
    protected boolean hasBlank;
    protected boolean hasError;
    
    public ExpressionNominalRowGrouper(Evaluable evaluable, String columnName, int cellIndex) {
        _evaluable = evaluable;
        _columnName = columnName;
        _cellIndex = cellIndex;
    }
    
    public boolean visit(Project project, int rowIndex, Row row) {
    	hasError = false;
    	hasBlank = false;
    	
        Properties bindings = ExpressionUtils.createBindings(project);
        
        visitRow(project, rowIndex, row, bindings, rowIndex);
        
        if (hasError) {
        	errorCount++;
        }
        if (hasBlank) {
        	blankCount++;
        }
        
        return false;
    }
    
    @Override
    public boolean visit(Project project, Record record) {
    	hasError = false;
    	hasBlank = false;
    	
        Properties bindings = ExpressionUtils.createBindings(project);
        
        for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
        	Row row = project.rows.get(r);
            visitRow(project, r, row, bindings, record.recordIndex);
        }
        
        if (hasError) {
        	errorCount++;
        }
        if (hasBlank) {
        	blankCount++;
        }
        
        return false;
    }
    
    protected void visitRow(Project project, int rowIndex, Row row, Properties bindings, int index) {
        Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);

        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
        
        Object value = _evaluable.evaluate(bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    processValue(v, rowIndex);
                }
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    processValue(v, rowIndex);
                }
            } else {
            	processValue(value, rowIndex);
            }
        } else {
        	processValue(value, rowIndex);
        }
    }
    
    protected void processValue(Object value, int index) {
        if (ExpressionUtils.isError(value)) {
            hasError = true;
        } else if (ExpressionUtils.isNonBlankData(value)) {
            String valueString = value.toString();
            IndexedNominalFacetChoice facetChoice = choices.get(valueString);
            
            if (facetChoice != null) {
            	if (facetChoice._latestIndex < index) {
            		facetChoice._latestIndex = index;
            		facetChoice.count++;
            	}
            } else {
            	String label = value.toString();
            	DecoratedValue dValue = new DecoratedValue(value, label);
            	IndexedNominalFacetChoice choice = 
            		new IndexedNominalFacetChoice(dValue, index);
            	
                choice.count = 1;
                choices.put(valueString, choice);
            }
        } else {
        	hasBlank = true;
        }
    }
}
