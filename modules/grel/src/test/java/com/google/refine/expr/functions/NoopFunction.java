
package com.google.refine.expr.functions;

import java.util.Properties;

import com.google.refine.grel.Function;

/**
 * A noop function for use in testing the default implementations of Function's default methods. This function does
 * nothing and returns null.
 */
public class NoopFunction implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        return null;
    }

    @Override
    public String getDescription() {
        return "A no-op function that does nothing and returns null.";
    }

    @Override
    public String getReturns() {
        return "null";
    }
}
