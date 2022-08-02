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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.util.JsonValueConverter;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class Join implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object v = args[0];
            Object s = args[1];

            if (v != null && s != null && s instanceof String) {
                String separator = (String) s;

                if (v.getClass().isArray() || v instanceof List<?> || v instanceof ArrayNode) {
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
                    } else if (v instanceof ArrayNode) {
                        ArrayNode a = (ArrayNode) v;
                        int l = a.size();

                        for (int i = 0; i < l; i++) {
                            if (sb.length() > 0) {
                                sb.append(separator);
                            }
                            sb.append(JsonValueConverter.convert(a.get(i)).toString());
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
        return new EvalError(EvalErrorMessage.expects_one_array_and_string(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.arr_join();
    }

    @Override
    public String getParams() {
        return "array a, string sep";
    }

    @Override
    public String getReturns() {
        return "string";
    }
}
