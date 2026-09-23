/*

Copyright 2026, OpenRefine contributors
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

import java.util.Collection;
import java.util.Properties;

import com.fasterxml.jackson.databind.node.ArrayNode;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.util.JsonValueConverter;
import com.google.refine.grel.Control;
import com.google.refine.grel.ControlDescription;
import com.google.refine.grel.ControlEvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.ast.VariableExpr;

public class Reduce implements Control {

    @Override
    public Object call(Properties bindings, Evaluable[] args) {
        // args[0] - list of values
        // args[1] - variable name in reduce expression
        // args[2] - variable name for accumulator
        // args[3] - initial value for reducing
        // args[4] - the reduce expression
        Object inputList = args[0].evaluate(bindings);
        if (ExpressionUtils.isError(inputList)) {
            return inputList;
        } else if (!ExpressionUtils.isArrayOrCollection(inputList) && !(inputList instanceof ArrayNode)) {
            return new EvalError(ControlEvalError.expects_first_arg_array(ControlFunctionRegistry.getControlName(this)));
        }

        String valueVariable = ((VariableExpr) args[1]).getName();
        String accumulatorVariable = ((VariableExpr) args[2]).getName();
        Object initialValue = args[3].evaluate(bindings);
        if (ExpressionUtils.isError(initialValue)) {
            return initialValue;
        }
        bindings.put(accumulatorVariable, initialValue);
        Evaluable expr = args[4];

        if (inputList.getClass().isArray()) {
            Object[] values = (Object[]) inputList;

            for (Object v : values) {
                if (v != null) {
                    bindings.put(valueVariable, v);
                } else {
                    bindings.remove(valueVariable);
                }

                Object result = expr.evaluate(bindings);
                if (ExpressionUtils.isError(result)) {
                    return result;
                }
                bindings.put(accumulatorVariable, result);
            }
        } else if (inputList instanceof ArrayNode) {
            ArrayNode a = (ArrayNode) inputList;
            int l = a.size();

            for (int i = 0; i < l; i++) {
                Object v = JsonValueConverter.convert(a.get(i));

                if (v != null) {
                    bindings.put(valueVariable, v);
                } else {
                    bindings.remove(valueVariable);
                }

                Object result = expr.evaluate(bindings);
                if (ExpressionUtils.isError(result)) {
                    return result;
                }
                bindings.put(accumulatorVariable, result);
            }
        } else {
            Collection<Object> collection = ExpressionUtils.toObjectCollection(inputList);

            for (Object v : collection) {
                if (v != null) {
                    bindings.put(valueVariable, v);
                } else {
                    bindings.remove(valueVariable);
                }

                Object result = expr.evaluate(bindings);
                if (ExpressionUtils.isError(result)) {
                    return result;
                }
                bindings.put(accumulatorVariable, result);
            }
        }

        return bindings.get(accumulatorVariable);
    }

    @Override
    public String checkArguments(Evaluable[] args) {
        if (args.length != 5) {
            return ControlEvalError.expects_five_args(ControlFunctionRegistry.getControlName(this));
        } else if (!(args[1] instanceof VariableExpr)) {
            return ControlEvalError.expects_second_arg_index_var_name(ControlFunctionRegistry.getControlName(this));
        } else if (!(args[2] instanceof VariableExpr)) {
            return ControlEvalError.expects_third_arg_element_var_name(ControlFunctionRegistry.getControlName(this));
        }
        return null;
    }

    @Override
    public String getDescription() {
        return ControlDescription.reduce_desc();
    }

    @Override
    public String getParams() {
        return "expression e, variable v, variable acc, expression initial, expression func";
    }

    @Override
    public String getReturns() {
        return "object";
    }

}
