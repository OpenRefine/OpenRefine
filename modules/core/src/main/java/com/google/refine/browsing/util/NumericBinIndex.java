/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.browsing.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * A utility class for computing the base bins that form the base histograms of numeric range facets. It evaluates an
 * expression on all the rows of a project to get numeric values, determines how many bins to distribute those values
 * in, and bins the rows accordingly.
 * 
 * This class processes all rows rather than just the filtered rows because it needs to compute the base bins of a
 * numeric range facet, which remain unchanged as the user interacts with the facet.
 */
abstract public class NumericBinIndex {

    protected int _totalValueCount;
    protected int _numbericValueCount;
    protected double _min;
    protected double _max;
    protected double _step;
    protected int[] _bins;

    protected int _numericRowCount;
    protected int _nonNumericRowCount;
    protected int _blankRowCount;
    protected int _errorRowCount;

    protected boolean _hasError = false;
    protected boolean _hasNonNumeric = false;
    protected boolean _hasNumeric = false;
    protected boolean _hasBlank = false;

    abstract protected void iterate(Project project, RowEvaluable rowEvaluable, List<Double> allValues);

    public NumericBinIndex(Project project, RowEvaluable rowEvaluable) {
        _min = Double.POSITIVE_INFINITY;
        _max = Double.NEGATIVE_INFINITY;

        // TODO: An array of doubles would be more memmory efficient - double[] allValues
        List<Double> allValues = new ArrayList<Double>();

        iterate(project, rowEvaluable, allValues);

        _numbericValueCount = allValues.size();

        if (_min >= _max) {
            _step = 1;
            _min = Math.min(_min, _max);
            _max = _min + _step;
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
            int bin = Math.max((int) Math.floor((d - _min) / _step), 0);
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

    protected void processRow(
            Project project,
            RowEvaluable rowEvaluable,
            List<Double> allValues,
            int rowIndex,
            Row row,
            Properties bindings) {
        Object value = rowEvaluable.eval(project, rowIndex, row, bindings);

        if (ExpressionUtils.isError(value)) {
            _hasError = true;
        } else if (ExpressionUtils.isNonBlankData(value)) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    _totalValueCount++;

                    if (ExpressionUtils.isError(v)) {
                        _hasError = true;
                    } else if (ExpressionUtils.isNonBlankData(v)) {
                        if (v instanceof Number) {
                            if (processValue(((Number) v).doubleValue(), allValues)) {
                                _hasNumeric = true;
                            } else {
                                _hasError = true;
                            }
                        } else {
                            _hasNonNumeric = true;
                        }
                    } else {
                        _hasBlank = true;
                    }
                }
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    _totalValueCount++;

                    if (ExpressionUtils.isError(v)) {
                        _hasError = true;
                    } else if (ExpressionUtils.isNonBlankData(v)) {
                        if (v instanceof Number) {
                            if (processValue(((Number) v).doubleValue(), allValues)) {
                                _hasNumeric = true;
                            } else {
                                _hasError = true;
                            }
                        } else {
                            _hasNonNumeric = true;
                        }
                    } else {
                        _hasBlank = true;
                    }
                }
            } else {
                _totalValueCount++;

                if (value instanceof Number) {
                    if (processValue(((Number) value).doubleValue(), allValues)) {
                        _hasNumeric = true;
                    } else {
                        _hasError = true;
                    }
                } else {
                    _hasNonNumeric = true;
                }
            }
        } else {
            _hasBlank = true;
        }
    }

    protected void preprocessing() {
        _hasBlank = false;
        _hasError = false;
        _hasNonNumeric = false;
        _hasNumeric = false;
    }

    protected void postprocessing() {
        if (_hasError) {
            _errorRowCount++;
        }
        if (_hasBlank) {
            _blankRowCount++;
        }
        if (_hasNumeric) {
            _numericRowCount++;
        }
        if (_hasNonNumeric) {
            _nonNumericRowCount++;
        }
    }

    protected boolean processValue(double v, List<Double> allValues) {
        if (!Double.isInfinite(v) && !Double.isNaN(v)) {
            _min = Math.min(_min, v);
            _max = Math.max(_max, v);
            allValues.add(v);
            return true;
        } else {
            return false;
        }
    }

}
