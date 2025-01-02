
package com.google.refine.grel.ast;

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
