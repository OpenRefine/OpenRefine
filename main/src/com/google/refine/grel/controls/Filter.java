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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.Control;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.ast.VariableExpr;

public class Filter implements Control {
    @Override
    public String checkArguments(Evaluable[] args) {
        if (args.length != 3) {
            return ControlFunctionRegistry.getControlName(this) + " expects 3 arguments";
        } else if (!(args[1] instanceof VariableExpr)) {
            return ControlFunctionRegistry.getControlName(this) + 
                " expects second argument to be a variable name";
        }
        return null;
    }

    @Override
    public Object call(Properties bindings, Evaluable[] args) {
        Object o = args[0].evaluate(bindings);
        if (ExpressionUtils.isError(o)) {
            return o;
        } else if (!ExpressionUtils.isArrayOrCollection(o) && !(o instanceof JSONArray)) {
            return new EvalError("First argument is not an array");
        }
        
        String name = ((VariableExpr) args[1]).getName();
        
        Object oldValue = bindings.get(name);
        try {
            List<Object> results = null;
            
            if (o.getClass().isArray()) {
                Object[] values = (Object[]) o;
                
                results = new ArrayList<Object>(values.length);
                for (Object v : values) {
                    if (v != null) {
                        bindings.put(name, v);
                    } else {
                        bindings.remove(name);
                    }
                    
                    Object r = args[2].evaluate(bindings);
                    if (r instanceof Boolean && ((Boolean) r).booleanValue()) {
                        results.add(v);
                    }
                }
            } else if (o instanceof JSONArray) {
                JSONArray a = (JSONArray) o;
                int l = a.length();
                
                results = new ArrayList<Object>(l);
                for (int i = 0; i < l; i++) {
                    try {
                        Object v = a.get(i);
                        
                        if (v != null) {
                            bindings.put(name, v);
                        } else {
                            bindings.remove(name);
                        }
                        
                        Object r = args[2].evaluate(bindings);
                        if (r instanceof Boolean && ((Boolean) r).booleanValue()) {
                            results.add(v);
                        }
                    } catch (JSONException e) {
                        results.add(new EvalError(e.getMessage()));
                    }
                }
            } else {
                Collection<Object> collection = ExpressionUtils.toObjectCollection(o);
                
                results = new ArrayList<Object>(collection.size());
                
                for (Object v : collection) {
                    if (v != null) {
                        bindings.put(name, v);
                    } else {
                        bindings.remove(name);
                    }
                    
                    Object r = args[2].evaluate(bindings);
                    if (r instanceof Boolean && ((Boolean) r).booleanValue()) {
                        results.add(v);
                    }
                }
            }
            
            return results.toArray(); 
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
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value(
            "Evaluates expression a to an array. Then for each array element, binds its value to variable name v, evaluates expression test which should return a boolean. If the boolean is true, pushes v onto the result array."
        );
        writer.key("params"); writer.value("expression a, variable v, expression test");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
