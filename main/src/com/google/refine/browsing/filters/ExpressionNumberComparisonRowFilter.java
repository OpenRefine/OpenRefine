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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.util.RowEvaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.util.JsonValueConverter;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * Judge if a row matches by evaluating a given expression on the row, based on a particular column, and checking the
 * result. It's a match if the result satisfies some numeric comparisons, or if the result is non-numeric or blank or
 * error and we want non-numeric or blank or error values.
 */
abstract public class ExpressionNumberComparisonRowFilter implements RowFilter {

    final protected RowEvaluable _rowEvaluable;
    final protected boolean _selectNumeric;
    final protected boolean _selectNonNumeric;
    final protected boolean _selectBlank;
    final protected boolean _selectError;

    public ExpressionNumberComparisonRowFilter(
            RowEvaluable rowEvaluable,
            boolean selectNumeric,
            boolean selectNonNumeric,
            boolean selectBlank,
            boolean selectError) {
        _rowEvaluable = rowEvaluable;
        _selectNumeric = selectNumeric;
        _selectNonNumeric = selectNonNumeric;
        _selectBlank = selectBlank;
        _selectError = selectError;
    }

    @Override
    public boolean filterRow(Project project, int rowIndex, Row row) {
        Properties bindings = ExpressionUtils.createBindings(project);

        Object value = _rowEvaluable.eval(project, rowIndex, row, bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    if (checkValue(v)) {
                        return true;
                    }
                }
                return false;
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    if (checkValue(v)) {
                        return true;
                    }
                }
                return false;
            } else if (value instanceof ArrayNode) {
                ArrayNode a = (ArrayNode) value;
                int l = a.size();

                for (int i = 0; i < l; i++) {
                    if (checkValue(JsonValueConverter.convert(a.get(i)))) {
                        return true;
                    }
                }
                return false;
            } // else, fall through
        }

        return checkValue(value);
    }

    protected boolean checkValue(Object v) {
        if (ExpressionUtils.isError(v)) {
            return _selectError;
        } else if (ExpressionUtils.isNonBlankData(v)) {
            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();
                if (Double.isInfinite(d) || Double.isNaN(d)) {
                    return _selectError;
                } else {
                    return _selectNumeric && checkValue(d);
                }
            } else {
                return _selectNonNumeric;
            }
        } else {
            return _selectBlank;
        }
    }

    abstract protected boolean checkValue(double d);
}
