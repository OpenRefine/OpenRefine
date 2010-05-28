package com.metaweb.gridworks.gel.ast;

import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;

/**
 * An abstract syntax tree node encapsulating the retrieval of a variable's content.
 */
public class VariableExpr implements Evaluable {
    final protected String _name;
    
    public VariableExpr(String name) {
        _name = name;
    }
    
    public Object evaluate(Properties bindings) {
        return bindings.get(_name);
    }

    public String toString() {
        return _name;
    }
    
    public String getName() {
        return _name;
    }
}
