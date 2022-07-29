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
 * result. It's a match if the result satisfies some string comparisons.
 */
abstract public class ExpressionStringComparisonRowFilter implements RowFilter {

    final protected Evaluable _evaluable;
    final protected Boolean _invert;
    final protected String _columnName;
    final protected int _cellIndex;

    public ExpressionStringComparisonRowFilter(Evaluable evaluable, Boolean invert, String columnName, int cellIndex) {
        _evaluable = evaluable;
        _invert = invert;
        _columnName = columnName;
        _cellIndex = cellIndex;
    }

    @Override
    public boolean filterRow(Project project, int rowIndex, Row row) {
        Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);

        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
        Boolean invert = _invert;
        Object value = _evaluable.evaluate(bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    if (checkValue(v instanceof String ? ((String) v) : v.toString())) {
                        return !invert;
                    }
                }
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    if (checkValue(v.toString())) {
                        return !invert;
                    }
                }
                return invert;
            } else if (value instanceof ArrayNode) {
                ArrayNode a = (ArrayNode) value;
                int l = a.size();

                for (int i = 0; i < l; i++) {
                    if (checkValue(JsonValueConverter.convert(a.get(i)).toString())) {
                        return !invert;
                    }
                }
                return invert;
            } else {
                if (checkValue(value instanceof String ? ((String) value) : value.toString())) {
                    return !invert;
                }
            }
        }
        return invert;
    }

    abstract protected boolean checkValue(String s);
}
