
package com.google.refine.grel.ast;

import java.util.Map;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.functions.arrays.ArgsToArray;
import com.google.refine.grel.Function;

/**
 * Node for an expression which introduces an array explicitly, with square brackets.
 */

public class ArrayExpr extends FunctionCallExpr {

    private final static Function argsToArray = new ArgsToArray();

    /**
     * @param elements
     *            the elements of the array
     */
    public ArrayExpr(Evaluable[] elements) {
        super(elements, argsToArray, "argsToArray", false);
    }

    @Override
    public Evaluable renameColumnDependencies(Map<String, String> substitutions) {
        Evaluable[] translatedArgs = new Evaluable[_args.length];
        for (int i = 0; i != _args.length; i++) {
            translatedArgs[i] = _args[i].renameColumnDependencies(substitutions);
        }
        return new ArrayExpr(translatedArgs);
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

        return "[" + sb.toString() + "]";
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof ArrayExpr;
    }

}
