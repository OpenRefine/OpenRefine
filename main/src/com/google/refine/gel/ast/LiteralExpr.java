package com.google.refine.gel.ast;

import java.util.Properties;

import org.json.JSONObject;

import com.google.refine.expr.Evaluable;

/**
 * An abstract syntax tree node encapsulating a literal value.
 */
public class LiteralExpr implements Evaluable {
    final protected Object _value;
    
    public LiteralExpr(Object value) {
        _value = value;
    }
                              
    public Object evaluate(Properties bindings) {
        return _value;
    }

    public String toString() {
        return _value instanceof String ? JSONObject.quote((String) _value) : _value.toString();
    }
}
