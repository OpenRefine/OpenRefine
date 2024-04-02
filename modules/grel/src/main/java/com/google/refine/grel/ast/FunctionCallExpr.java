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

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.Function;

/**
 * An abstract syntax tree node encapsulating a function call. The function's arguments are all evaluated down to values
 * before the function is applied. If any argument is an error, the function is not applied, and the error is the result
 * of the expression.
 */
public class FunctionCallExpr implements Evaluable {

    final protected Evaluable[] _args;
    final protected Function _function;

    public FunctionCallExpr(Evaluable[] args, Function f) {
        _args = args;
        _function = f;
    }

    @Override
    public Object evaluate(Properties bindings) {
        Object[] args = new Object[_args.length];
        for (int i = 0; i < _args.length; i++) {
            Object v = _args[i].evaluate(bindings);
            if (ExpressionUtils.isError(v)) {
                return v; // bubble up the error
            }
            args[i] = v;
        }
        try {
            return _function.call(bindings, args);
        } catch (Exception e) {
            return new EvalError(e);
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        for (Evaluable ev : _args) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(ev.toString());
        }

        return _function.getClass().getSimpleName() + "(" + sb.toString() + ")";
    }
}
