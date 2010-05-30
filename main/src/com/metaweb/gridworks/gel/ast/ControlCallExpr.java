package com.metaweb.gridworks.gel.ast;

import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.gel.Control;

/**
 * An abstract syntax tree node encapsulating a control call, such as "if".
 */
public class ControlCallExpr implements Evaluable {
    final protected Evaluable[] _args;
    final protected Control     _control;
    
    public ControlCallExpr(Evaluable[] args, Control c) {
        _args = args;
        _control = c;
    }
                              
    public Object evaluate(Properties bindings) {
        return _control.call(bindings, _args);
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
        
        return _control.getClass().getSimpleName() + "(" + sb.toString() + ")";
    }
}
