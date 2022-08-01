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

package com.google.refine.grel.controls;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.Control;
import com.google.refine.grel.ControlDescription;
import com.google.refine.grel.ControlEvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.ast.VariableExpr;

public class ForRange implements Control {

    @Override
    public String checkArguments(Evaluable[] args) {
        if (args.length != 5) {
            return ControlEvalError.expects_five_args(ControlFunctionRegistry.getControlName(this));
        } else if (!(args[3] instanceof VariableExpr)) {
            // variable name";
            return ControlEvalError.expects_third_arg_element_var_name(ControlFunctionRegistry.getControlName(this));
        }
        return null;
    }

    @Override
    public Object call(Properties bindings, Evaluable[] args) {
        Object fromO = args[0].evaluate(bindings);
        Object toO = args[1].evaluate(bindings);
        Object stepO = args[2].evaluate(bindings);

        if (ExpressionUtils.isError(fromO)) {
            return fromO;
        } else if (ExpressionUtils.isError(toO)) {
            return toO;
        } else if (ExpressionUtils.isError(stepO)) {
            return stepO;
        } else if (!(fromO instanceof Number) || !(toO instanceof Number) || !(stepO instanceof Number)) {
            return ControlEvalError.for_range();
        }

        String indexName = ((VariableExpr) args[3]).getName();
        Object oldIndexValue = bindings.get(indexName);

        try {
            List<Object> results = new ArrayList<Object>();

            if (isIntegral((Number) fromO) && isIntegral((Number) stepO)) {
                long from = ((Number) fromO).longValue();
                long step = ((Number) stepO).longValue();
                double to = ((Number) toO).doubleValue();

                while (from < to) {
                    bindings.put(indexName, from);

                    Object r = args[4].evaluate(bindings);

                    results.add(r);

                    from += step;
                }
            } else {
                double from = ((Number) fromO).longValue();
                double step = ((Number) stepO).longValue();
                double to = ((Number) toO).doubleValue();

                while (from < to) {
                    bindings.put(indexName, from);

                    Object r = args[4].evaluate(bindings);

                    results.add(r);

                    from += step;
                }
            }
            return results.toArray();
        } finally {
            /*
             * Restore the old values bound to the variables, if any.
             */
            if (oldIndexValue != null) {
                bindings.put(indexName, oldIndexValue);
            } else {
                bindings.remove(indexName);
            }
        }
    }

    static private boolean isIntegral(Number o) {
        if (o instanceof Integer || o instanceof Long) {
            return true;
        } else {
            return (o.doubleValue() - o.longValue()) == 0;
        }
    }

    @Override
    public String getDescription() {
        // \"to\". At each iteration, evaluates expression e, and pushes the result onto the result array.";
        return ControlDescription.for_range_desc();
    }

    @Override
    public String getParams() {
        return "number from, number to, number step, variable v, expression e";
    }

    @Override
    public String getReturns() {
        return "array";
    }
}
