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

package com.google.refine.browsing.filters;

import java.util.Collection;
import java.util.Properties;

import com.google.refine.browsing.RowFilter;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * Judge if a row matches by evaluating two given expressions on the row, based on two different columns and checking
 * the results. It's a match if the result satisfies some numeric comparisons.
 */
abstract public class DualExpressionsNumberComparisonRowFilter implements RowFilter {

    final protected Evaluable _x_evaluable;
    final protected String _x_columnName;
    final protected int _x_cellIndex;
    final protected Evaluable _y_evaluable;
    final protected String _y_columnName;
    final protected int _y_cellIndex;

    public DualExpressionsNumberComparisonRowFilter(
            Evaluable x_evaluable,
            String x_columnName,
            int x_cellIndex,
            Evaluable y_evaluable,
            String y_columnName,
            int y_cellIndex) {
        _x_evaluable = x_evaluable;
        _x_columnName = x_columnName;
        _x_cellIndex = x_cellIndex;
        _y_evaluable = y_evaluable;
        _y_columnName = y_columnName;
        _y_cellIndex = y_cellIndex;
    }

    @Override
    public boolean filterRow(Project project, int rowIndex, Row row) {
        Cell x_cell = _x_cellIndex < 0 ? null : row.getCell(_x_cellIndex);
        Properties x_bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(x_bindings, row, rowIndex, _x_columnName, x_cell);
        Object x_value = _x_evaluable.evaluate(x_bindings);

        Cell y_cell = _y_cellIndex < 0 ? null : row.getCell(_y_cellIndex);
        Properties y_bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(y_bindings, row, rowIndex, _y_columnName, y_cell);
        Object y_value = _y_evaluable.evaluate(y_bindings);

        if (x_value != null && y_value != null) {
            if (x_value.getClass().isArray() || y_value.getClass().isArray()) {
                return false;
            } else if (x_value instanceof Collection<?> || y_value instanceof Collection<?>) {
                return false;
            } // else, fall through
        }

        return checkValue(x_value, y_value);
    }

    protected boolean checkValue(Object vx, Object vy) {
        if (ExpressionUtils.isError(vx) || ExpressionUtils.isError(vy)) {
            return false;
        } else if (ExpressionUtils.isNonBlankData(vx) && ExpressionUtils.isNonBlankData(vy)) {
            if (vx instanceof Number && vy instanceof Number) {
                double dx = ((Number) vx).doubleValue();
                double dy = ((Number) vy).doubleValue();
                return (!Double.isInfinite(dx) &&
                        !Double.isNaN(dx) &&
                        !Double.isInfinite(dy) &&
                        !Double.isNaN(dy) &&
                        checkValues(dx, dy));
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    abstract protected boolean checkValues(double dx, double dy);
}
