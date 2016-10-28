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

public class ForEachIndex implements Control {
    @Override
    public String checkArguments(Evaluable[] args) {
        if (args.length != 4) {
            return ControlFunctionRegistry.getControlName(this) + " expects 4 arguments";
        } else if (!(args[1] instanceof VariableExpr)) {
            return ControlFunctionRegistry.getControlName(this) + 
                " expects second argument to be the index's variable name";
        } else if (!(args[2] instanceof VariableExpr)) {
            return ControlFunctionRegistry.getControlName(this) + 
                " expects third argument to be the element's variable name";
        }
        return null;
    }

    @Override
    public Object call(Properties bindings, Evaluable[] args) {
        Object o = args[0].evaluate(bindings);
        if (ExpressionUtils.isError(o)) {
            return o;
        } else if (!ExpressionUtils.isArrayOrCollection(o) && !(o instanceof JSONArray)) {
            return new EvalError("First argument to forEach is not an array");
        }
        
        String indexName = ((VariableExpr) args[1]).getName();
        String elementName = ((VariableExpr) args[2]).getName();
        
        Object oldIndexValue = bindings.get(indexName);
        Object oldElementValue = bindings.get(elementName);
        try {
            List<Object> results = null;
            
            if (o.getClass().isArray()) {
                Object[] values = (Object[]) o;
                
                results = new ArrayList<Object>(values.length);
                
                for (int i = 0; i < values.length; i++) {
                    Object v = values[i];
                    
                    bindings.put(indexName, i);
                    bindings.put(elementName, v);
                    
                    Object r = args[3].evaluate(bindings);
                    
                    results.add(r);
                }
            } else if (o instanceof JSONArray) {
                JSONArray a = (JSONArray) o;
                int l = a.length();
                
                results = new ArrayList<Object>(l);
                for (int i = 0; i < l; i++) {
                    try {
                        Object v = a.get(i);
                        
                        bindings.put(indexName, i);
                        bindings.put(elementName, v);
                        
                        Object r = args[3].evaluate(bindings);
                        
                        results.add(r);
                    } catch (JSONException e) {
                        results.add(new EvalError(e.getMessage()));
                    }
                }
            } else {
                List<Object> list = ExpressionUtils.toObjectList(o);
                
                results = new ArrayList<Object>(list.size());
                
                for (int i = 0; i < list.size(); i++) {
                    Object v = list.get(i);

                    bindings.put(indexName, i);
                    bindings.put(elementName, v);
                    
                    Object r = args[3].evaluate(bindings);
                    
                    results.add(r);
                }
            }
            
            return results.toArray(); 
        } finally {
            /*
             *  Restore the old values bound to the variables, if any.
             */
            if (oldIndexValue != null) {
                bindings.put(indexName, oldIndexValue);
            } else {
                bindings.remove(indexName);
            }
            if (oldElementValue != null) {
                bindings.put(elementName, oldElementValue);
            } else {
                bindings.remove(elementName);
            }
        }
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value(
            "Evaluates expression a to an array. Then for each array element, binds its index to variable i and its value to variable name v, evaluates expression e, and pushes the result onto the result array."
        );
        writer.key("params"); writer.value("expression a, variable i, variable v, expression e");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
