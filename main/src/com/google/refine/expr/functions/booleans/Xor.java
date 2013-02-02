/*
  Implementing a naive XOR operation
  Copyright 2013, Jesus M. Castagnetto
  License: BSD 2-Clause License (http://opensource.org/licenses/BSD-2-Clause)
*/

package com.google.refine.expr.functions.booleans;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class Xor implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2 &&
                args[0] != null && args[0] instanceof Boolean &&
                args[1] != null && args[0] instanceof Boolean) {
            boolean o1 = ((Boolean) args[0]).booleanValue();
            boolean o2 = ((Boolean) args[1]).booleanValue();
            return o1 != o2;
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 2 booleans");
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("description"); writer.value("XORs two boolean values");
        writer.key("params"); writer.value("boolean a, boolean b");
        writer.key("returns"); writer.value("boolean");
        writer.endObject();
    }
}
