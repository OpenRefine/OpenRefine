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
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.util.JsonValueConverter;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * Judge if a row matches by evaluating a given expression on the row, based on a particular column, and checking the
 * result. It's a match if the result is any one of a given list of values, or if the result is blank or error and we
 * want blank or error values.
 */
public class ExpressionEqualRowFilter implements RowFilter {

    final protected Evaluable _evaluable; // the expression to evaluate

    final protected String _columnName;
    final protected int _cellIndex; // the expression is based on this column;
                                    // -1 if based on no column in particular,
                                    // for expression such as "row.starred".

    final protected Object[] _matches;
    final protected boolean _selectBlank;
    final protected boolean _selectError;
    final protected boolean _invert;

    public ExpressionEqualRowFilter(
            Evaluable evaluable,
            String columnName,
            int cellIndex,
            Object[] matches,
            boolean selectBlank,
            boolean selectError,
            boolean invert) {
        _evaluable = evaluable;
        _columnName = columnName;
        _cellIndex = cellIndex;
        _matches = matches;
        _selectBlank = selectBlank;
        _selectError = selectError;
        _invert = invert;
    }

    @Override
    public boolean filterRow(Project project, int rowIndex, Row row) {
        return _invert ? internalInvertedFilterRow(project, rowIndex, row) : internalFilterRow(project, rowIndex, row);
    }

    public boolean internalFilterRow(Project project, int rowIndex, Row row) {
        Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);

        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);

        Object value = _evaluable.evaluate(bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    if (testValue(v)) {
                        return true;
                    }
                }
                return false;
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    if (testValue(v)) {
                        return true;
                    }
                }
                return false;
            } else if (value instanceof ArrayNode) {
                ArrayNode a = (ArrayNode) value;
                int l = a.size();

                for (int i = 0; i < l; i++) {
                    if (testValue(JsonValueConverter.convert(a.get(i)))) {
                        return true;
                    }
                }
                return false;
            } // else, fall through
        }

        return testValue(value);
    }

    public boolean internalInvertedFilterRow(Project project, int rowIndex, Row row) {
        Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);

        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);

        Object value = _evaluable.evaluate(bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    if (testValue(v)) {
                        return false;
                    }
                }
                return true;
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    if (testValue(v)) {
                        return false;
                    }
                }
                return true;
            } else if (value instanceof ArrayNode) {
                ArrayNode a = (ArrayNode) value;
                int l = a.size();

                for (int i = 0; i < l; i++) {
                    if (testValue(JsonValueConverter.convert(a.get(i)))) {
                        return false;
                    }
                }
                return true;
            } // else, fall through
        }

        return !testValue(value);
    }

    protected boolean testValue(Object v) {
        if (ExpressionUtils.isError(v)) {
            return _selectError;
        } else if (ExpressionUtils.isNonBlankData(v)) {
            for (Object match : _matches) {
                if (testValue(v, match)) {
                    return true;
                }
            }
            return false;
        } else {
            return _selectBlank;
        }
    }

    protected boolean testValue(Object v, Object match) {
        return (v instanceof Number && match instanceof Number) ? ((Number) match).doubleValue() == ((Number) v).doubleValue()
                : match.equals(v);
    }
}
