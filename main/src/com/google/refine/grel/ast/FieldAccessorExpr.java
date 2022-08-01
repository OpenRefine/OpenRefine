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

package com.google.refine.grel.ast;

import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.HasFields;
import com.google.refine.expr.util.JsonValueConverter;

/**
 * An abstract syntax tree node encapsulating a field accessor, e.g., "cell.value" is accessing the field named "value"
 * on the variable called "cell".
 */
public class FieldAccessorExpr implements Evaluable {

    final protected Evaluable _inner;
    final protected String _fieldName;

    public FieldAccessorExpr(Evaluable inner, String fieldName) {
        _inner = inner;
        _fieldName = fieldName;
    }

    @Override
    public Object evaluate(Properties bindings) {
        Object o = _inner.evaluate(bindings);
        if (ExpressionUtils.isError(o)) {
            return o; // bubble the error up
        } else if (o == null) {
            return null;
        } else if (o instanceof HasFields) {
            return ((HasFields) o).getField(_fieldName, bindings);
        } else if (o instanceof ObjectNode) {
            JsonNode value = ((ObjectNode) o).get(_fieldName);
            return JsonValueConverter.convert(value);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return _inner.toString() + "." + _fieldName;
    }
}
