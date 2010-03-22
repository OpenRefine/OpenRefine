package com.metaweb.gridworks.expr;

import java.util.Properties;

/**
 * Interface for evaluable expressions in any arbitrary language.
 */
public interface Evaluable {
    /**
     * Evaluate this expression in the given environment (bindings).
     * 
     * @param bindings
     * @return
     */
    public Object evaluate(Properties bindings);
}
