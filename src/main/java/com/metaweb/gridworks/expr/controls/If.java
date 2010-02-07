package com.metaweb.gridworks.expr.controls;

 import java.util.Properties;

import com.metaweb.gridworks.expr.Control;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;

public class If implements Control {

    public Object call(Properties bindings, Evaluable[] args) {
        if (args.length >= 3) {
            Object o = args[0].evaluate(bindings);
            
            if (ExpressionUtils.isTrue(o)) {
                return args[1].evaluate(bindings);
            } else if (args.length >= 3) {
                return args[2].evaluate(bindings);
            }
        }
        return null;
    }

}
