package com.google.refine.grel.controls;

import java.util.Collection;
import java.util.Properties;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.util.JsonValueConverter;
import com.google.refine.grel.Control;
import com.google.refine.grel.ast.VariableExpr;

public class All implements Control {
    
    @Override
    public String checkArguments(Evaluable[] args) {
        if (args.length != 1 && args.length != 3) {
            return "all expects either 1 argument (an array of booleans) or 3 arguments (array, variable name, expression)";
        }
        if (args.length == 3 && !(args[1] instanceof VariableExpr)) {
            return "all expects second argument to be a variable name";
        }
        return null;
    }

    @Override
    public Object call(Properties bindings, Evaluable[] args) {
        Object array = args[0].evaluate(bindings);
        if (array == null) {
            return true; // Empty or null evaluates to true in "all" logic
        }

        String varName = null;
        if (args.length == 3) {
            varName = ((VariableExpr) args[1]).getName();
        }

        if (array instanceof Collection<?>) {
            for (Object item : (Collection<?>) array) {
                if (!evaluateItem(item, varName, bindings, args)) {
                    return false;
                }
            }
            return true;
        } else if (array.getClass().isArray()) {
            for (Object item : (Object[]) array) {
                if (!evaluateItem(item, varName, bindings, args)) {
                    return false;
                }
            }
            return true;
        } else if (array instanceof ArrayNode) {
            ArrayNode a = (ArrayNode) array;
            for (int i = 0; i < a.size(); i++) {
                Object item = JsonValueConverter.convert(a.get(i));
                if (!evaluateItem(item, varName, bindings, args)) {
                    return false;
                }
            }
            return true;
        }
        
        return new EvalError("First argument must be an array");
    }

    private boolean evaluateItem(Object item, String varName, Properties bindings, Evaluable[] args) {
        if (args.length == 1) {
            return ExpressionUtils.isTrue(item);
        } else {
            Object oldValue = bindings.get(varName);
            if (item != null) {
                bindings.put(varName, item);
            } else {
                bindings.remove(varName);
            }
            
            Object result = args[2].evaluate(bindings);
            
            if (oldValue != null) {
                bindings.put(varName, oldValue);
            } else {
                bindings.remove(varName);
            }
            
            return ExpressionUtils.isTrue(result);
        }
    }

    @Override
    public String getDescription() {
        return "Evaluates to true if all elements in the array are true (or evaluate to true based on the expression)";
    }

    @Override
    public String getParams() {
        return "array a, (optional) variable v, (optional) expression e";
    }

    @Override
    public String getReturns() {
        return "boolean";
    }
}