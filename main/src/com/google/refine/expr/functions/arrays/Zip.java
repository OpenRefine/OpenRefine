/*

Copyright 2024, OpenRefine contributors
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

import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Streams;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class Zip implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1) {
            Object[] v = (Object[]) args[0];
            List<List> output = new ArrayList<>();
            List arg1 = new ArrayList<>();
            List arg2 = new ArrayList<>();

            boolean isFirst = true;
            if (v != null) {
                for (Object o : v) {
                    if (o.getClass().isArray() || o instanceof List<?> || o instanceof ArrayNode) {
                        if (o.getClass().isArray()) {
                            arg2.addAll(Arrays.asList((Object[]) o));
                        } else if (o instanceof ArrayNode) {
                            ArrayNode a = (ArrayNode) o;
                            if (a.isArray()) {
                                for (JsonNode objNode : a) {
                                    arg2.add(objNode);
                                }
                            }
                        } else {
                            for (Object z : ExpressionUtils.toObjectList(o)) {
                                if (z != null) {
                                    arg2.add(z.toString());
                                } else {
                                    arg2.add(z);
                                }
                            }
                        }
                        if (isFirst) {
                            for (Object x : arg2) {
                                arg1 = new ArrayList() {

                                    {
                                        add(x);
                                    }
                                };
                                output.add(arg1);
                            }
                        } else {
                            output = (List<ArrayList>) Streams.zip(output.stream(),
                                    arg2.stream(),
                                    (a, b) -> collectElements(a, b))
                                    .collect(Collectors.toList());
                        }
                        isFirst = false;
                    }
                    arg2.clear();
                }
            }
            return output;
        }
        return new EvalError(EvalErrorMessage.expects_one_array(ControlFunctionRegistry.getFunctionName(this)));
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

    private static ArrayList collectElements(Object a, Object b) {
        ArrayList returnList = new ArrayList<>();

        if (a == null) {
            returnList.add(a);
        } else if (a instanceof ArrayList) {
            ArrayList x = (ArrayList) a;
            returnList = (ArrayList) x.stream().collect(toCollection(ArrayList::new));
        } else {
            returnList.add(a.toString());
        }

        if (b == null) {
            returnList.add(b);
        } else if (b instanceof ArrayList) {
            ArrayList y = (ArrayList) b;
            int i;
            for (i = 0; i < y.size(); i++) {
                returnList.add(y.get(i).toString());
            }
        } else {
            returnList.add(b.toString());
        }
        return returnList;
    }
}
