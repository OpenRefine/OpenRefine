/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.grel.controls;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.Control;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.ast.VariableExpr;

public class ForNonBlank implements Control {
    @Override
    public String checkArguments(Evaluable[] args) {
        if (args.length != 4) {
            return ControlFunctionRegistry.getControlName(this) + " expects 4 arguments";
        } else if (!(args[1] instanceof VariableExpr)) {
            return ControlFunctionRegistry.getControlName(this) + 
                " expects second argument to be a variable name";
        }
        return null;
    }
    
    @Override
    public Object call(Properties bindings, Evaluable[] args) {
        Object o = args[0].evaluate(bindings);
        
        Evaluable var = args[1];
        String name = ((VariableExpr) var).getName();
        
        if (ExpressionUtils.isNonBlankData(o)) {
            Object oldValue = bindings.get(name);
            bindings.put(name, o);
            
            try {
                return args[2].evaluate(bindings);
            } finally {
                /*
                 *  Restore the old value bound to the variable, if any.
                 */
                if (oldValue != null) {
                    bindings.put(name, oldValue);
                } else {
                    bindings.remove(name);
                }
            }
        } else {
            return args[3].evaluate(bindings);
        }
    }

    @Override
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value(
            "Evaluates expression o. If it is non-blank, binds its value to variable name v, evaluates expression eNonBlank and returns the result. " +
            "Otherwise (if o evaluates to blank), evaluates expression eBlank and returns that result instead."
        );
        writer.key("params"); writer.value("expression o, variable v, expression eNonBlank, expression eBlank");
        writer.key("returns"); writer.value("Depends on actual arguments");
        writer.endObject();
    }
}
