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
            return ControlEvalError.expects_second_arg_var_name(ControlFunctionRegistry.getControlName(this));
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
        return "expression e, variable v, expression initial, expression func";
    }

    @Override
    public String getReturns() {
        return "object";
    }

}
