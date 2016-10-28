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

package com.google.refine.expr.functions.arrays;

import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class Join implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object v = args[0];
            Object s = args[1];
            
            if (v != null && s != null && s instanceof String) {
                String separator = (String) s;
                
                if (v.getClass().isArray() || v instanceof List<?> || v instanceof JSONArray) {
                    StringBuffer sb = new StringBuffer();
                    if (v.getClass().isArray()) {
                        for (Object o : (Object[]) v) {
                            if (o != null) {
                                if (sb.length() > 0) {
                                    sb.append(separator);
                                }
                                sb.append(o.toString());
                            }
                        }
                    } else if (v instanceof JSONArray) {
                        JSONArray a = (JSONArray) v;
                        int l = a.length();
                        
                        for (int i = 0; i < l; i++) {
                            if (sb.length() > 0) {
                                sb.append(separator);
                            }
                            try {
                                sb.append(a.get(i).toString());
                            } catch (JSONException e) {
                                return new EvalError(ControlFunctionRegistry.getFunctionName(this) + 
                                    " cannot retrieve element " + i + " of array");
                            }
                        }
                    } else {
                        for (Object o : ExpressionUtils.toObjectList(v)) {
                            if (o != null) {
                                if (sb.length() > 0) {
                                    sb.append(separator);
                                }
                                sb.append(o.toString());
                            }
                        }
                    }
                    
                    return sb.toString();
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects an array and a string");
    }

    @Override
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the string obtained by joining the array a with the separator sep");
        writer.key("params"); writer.value("array a, string sep");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}
