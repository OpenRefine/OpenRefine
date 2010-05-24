package com.metaweb.gridworks.browsing.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.metaweb.gridworks.browsing.DecoratedValue;
import com.metaweb.gridworks.browsing.RecordVisitor;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.browsing.facets.NominalFacetChoice;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;
import com.metaweb.gridworks.model.Row;

/**
 * Visit matched rows or records and group them into facet choices based on the values computed
 * from a given expression.
 */
public class ExpressionNominalValueGrouper implements RowVisitor, RecordVisitor {
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
    
    /*
     * Scratch pad variables
     */
    protected boolean hasBlank;
    protected boolean hasError;
    
    public ExpressionNominalValueGrouper(Evaluable evaluable, String columnName, int cellIndex) {
        _evaluable = evaluable;
        _columnName = columnName;
        _cellIndex = cellIndex;
    }
    
    @Override
    public void start(Project project) {
    	// nothing to do
    }
    
    @Override
    public void end(Project project) {
    	// nothing to do
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
    	Object value = evalRow(project, rowIndex, row, bindings);
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
    
    protected Object evalRow(Project project, int rowIndex, Row row, Properties bindings) {
	    Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);
	
	    ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
	    
	    return _evaluable.evaluate(bindings);
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
    
    public RowEvaluable getChoiceCountRowEvaluable() {
    	return new RowEvaluable() {
			@Override
			public Object eval(Project project, int rowIndex, Row row, Properties bindings) {
		    	Object value = evalRow(project, rowIndex, row, bindings);
		        if (value != null) {
			        if (value.getClass().isArray()) {
		                Object[] a = (Object[]) value;
		                for (int i = 0; i < a.length; i++) {
		                	a[i] = getValueCount(a[i]);
		                }
		                return a;
		            } else if (value instanceof Collection<?>) {
		            	List<Object> list = ExpressionUtils.toObjectList(value);
		            	int count = list.size();
		                for (int i = 0; i < count; i++) {
		                	list.set(i, getValueCount(list.get(i)));
		                }
		                return list;
		            }
	            }
			        
		        return getValueCount(value);
			}
    	
    		protected Integer getValueCount(Object value) {
    			if (value == null) {
    				return blankCount;
    			} else if (ExpressionUtils.isError(value)) {
    				return errorCount;
    			} else {
    				return choices.get(value).count;
    			}
    		}
    	};
    }
}
