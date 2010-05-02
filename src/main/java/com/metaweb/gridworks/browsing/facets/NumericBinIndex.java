package com.metaweb.gridworks.browsing.facets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

/**
 * A utility class for computing the base bins that form the base histograms of 
 * numeric range facets. It evaluates an expression on all the rows of a project to
 * get numeric values, determines how many bins to distribute those values in, and 
 * bins the rows accordingly.
 * 
 * This class processes all rows rather than just the filtered rows because it
 * needs to compute the base bins of a numeric range facet, which remain unchanged 
 * as the user interacts with the facet.
 */
public class NumericBinIndex {
    
    private int _totalValueCount;
    private int _numbericValueCount;
    private double _min;
    private double _max;
    private double _step;
    private int[]  _bins;
    
    private int _numericRowCount;
    private int _nonNumericRowCount;
    private int _blankRowCount;
    private int _errorRowCount;
    
    public NumericBinIndex(Project project, String columnName, int cellIndex, Evaluable eval) {
        Properties bindings = ExpressionUtils.createBindings(project);
        
        _min = Double.POSITIVE_INFINITY;
        _max = Double.NEGATIVE_INFINITY;
        
        List<Double> allValues = new ArrayList<Double>();
        for (int i = 0; i < project.rows.size(); i++) {
            Row row = project.rows.get(i);
            Cell cell = row.getCell(cellIndex);

            ExpressionUtils.bind(bindings, row, i, columnName, cell);
            
            Object value = eval.evaluate(bindings);
            
            boolean rowHasError = false;
            boolean rowHasNonNumeric = false;
            boolean rowHasNumeric = false;
            boolean rowHasBlank = false;
            
            if (ExpressionUtils.isError(value)) {
                rowHasError = true;
            } else if (ExpressionUtils.isNonBlankData(value)) {
                if (value.getClass().isArray()) {
                    Object[] a = (Object[]) value;
                    for (Object v : a) {
                        _totalValueCount++;
                        
                        if (ExpressionUtils.isError(v)) {
                            rowHasError = true;
                        } else if (ExpressionUtils.isNonBlankData(v)) {
                            if (v instanceof Number) {
                                rowHasNumeric = true;
                                processValue(((Number) v).doubleValue(), allValues);
                            } else {
                                rowHasNonNumeric = true;
                            }
                        } else {
                            rowHasBlank = true;
                        }
                    }
                } else if (value instanceof Collection<?>) {
                    for (Object v : ExpressionUtils.toObjectCollection(value)) {
                        _totalValueCount++;
                        
                        if (ExpressionUtils.isError(v)) {
                            rowHasError = true;
                        } else if (ExpressionUtils.isNonBlankData(v)) {
                            if (v instanceof Number) {
                                rowHasNumeric = true;
                                processValue(((Number) v).doubleValue(), allValues);
                            } else {
                                rowHasNonNumeric = true;
                            }
                        } else {
                            rowHasBlank = true;
                        }
                    }
                } else {
                    _totalValueCount++;
                    
                    if (value instanceof Number) {
                        rowHasNumeric = true;
                        processValue(((Number) value).doubleValue(), allValues);
                    } else {
                        rowHasNonNumeric = true;
                    }
                }
            } else {
                rowHasBlank = true;
            }
            
            if (rowHasError) {
                _errorRowCount++;
            }
            if (rowHasBlank) {
                _blankRowCount++;
            }
            if (rowHasNumeric) {
                _numericRowCount++;
            }
            if (rowHasNonNumeric) {
                _nonNumericRowCount++;
            }
        }
        
        _numbericValueCount = allValues.size();
        
        if (_min >= _max) {
            _step = 1;
            _min = Math.min(_min, _max);
            _max = _step;
            _bins = new int[1];
            
            return;
        }
        
        double diff = _max - _min;
        
        _step = 1;
        if (diff > 10) {
            while (_step * 100 < diff) {
                _step *= 10;
            }
        } else {
            while (_step * 100 > diff) {
                _step /= 10;
            }
        }
        
        double originalMax = _max;
        _min = (Math.floor(_min / _step) * _step);
        _max = (Math.ceil(_max / _step) * _step);
        
        double binCount = (_max - _min) / _step;
        if (binCount > 100) {
            _step *= 2;
            binCount = (binCount + 1) / 2;
        }
        
        if (_max <= originalMax) {
        	_max += _step;
        	binCount++;
        }
        
        _bins = new int[(int) Math.round(binCount)];
        for (double d : allValues) {
            int bin = Math.max((int) Math.floor((d - _min) / _step),0);
            _bins[bin]++;
        }
    }
    
    public boolean isNumeric() {
        return _numbericValueCount > _totalValueCount / 2;
    }

    public double getMin() {
        return _min;
    }

    public double getMax() {
        return _max;
    }

    public double getStep() {
        return _step;
    }

    public int[] getBins() {
        return _bins;
    }
    
    public int getNumericRowCount() {
        return _numericRowCount;
    }

    public int getNonNumericRowCount() {
        return _nonNumericRowCount;
    }

    public int getBlankRowCount() {
        return _blankRowCount;
    }

    public int getErrorRowCount() {
        return _errorRowCount;
    }

    protected void processValue(double v, List<Double> allValues) {
        if (!Double.isInfinite(v) && !Double.isNaN(v)) {
            _min = Math.min(_min, v);
            _max = Math.max(_max, v);
            allValues.add(v);
        }
    }

}
