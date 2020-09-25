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

package org.openrefine.browsing.filters;

import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import org.openrefine.browsing.util.RowEvaluable;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.util.JsonValueConverter;
import org.openrefine.model.Cell;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.util.StringUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Judge if a row matches by evaluating a given expression on the row, based on a particular
 * column, and checking the result. It's a match if the result is any one of a given list of 
 * values, or if the result is blank or error and we want blank or error values. 
 */
public class ExpressionEqualRowFilter implements RowFilter {
    private static final long serialVersionUID = 1L;

    final protected RowEvaluable       _evaluable; // the expression to evaluate
    
    final protected String          _columnName;
    final protected int             _cellIndex; // the expression is based on this column;
                                                // -1 if based on no column in particular,
                                                // for expression such as "row.starred".
    
    final protected Set<String>     _matches;
    final protected boolean         _selectBlank;
    final protected boolean         _selectError;
    final protected boolean         _invert;
    
    public ExpressionEqualRowFilter(
        RowEvaluable evaluable,
        String columnName,
        int cellIndex, 
        Set<String> matches, 
        boolean selectBlank, 
        boolean selectError,
        boolean invert
    ) {
        _evaluable = evaluable;
        _columnName = columnName;
        _cellIndex = cellIndex;
        _matches = matches;
        _selectBlank = selectBlank;
        _selectError = selectError;
        _invert = invert;
    }

    @Override
    public boolean filterRow(long rowIndex, Row row) {
        return _invert ^
                internalFilterRow(rowIndex, row);
    }
    
    public boolean internalFilterRow(long rowIndex, Row row) {
        Properties bindings = ExpressionUtils.createBindings();
        
        Object value = _evaluable.eval(rowIndex, row, bindings);
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
    
    protected boolean testValue(Object v) {
        if (ExpressionUtils.isError(v)) {
            return _selectError;
        } else if (ExpressionUtils.isNonBlankData(v)) {
            return _matches.contains(StringUtils.toString(v));
        } else {
            return _selectBlank;
        }
    }
}
