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
        if (args.length >= 2) {
            List<List> input = new ArrayList<>();
            List<List> output;

            for (Object v : args) {
                List ziparg = new ArrayList<>();
                if (v == null || !(v.getClass().isArray() || v instanceof List<?> || v instanceof ArrayNode)) {
                    return new EvalError(
                            EvalErrorMessage.expects_at_least_two_or_more_array_args(ControlFunctionRegistry.getFunctionName(this)));
                }

                if (v.getClass().isArray()) {
                    ziparg.addAll(Arrays.asList((Object[]) v));
                } else if (v instanceof ArrayNode) {
                    ArrayNode a = (ArrayNode) v;
                    if (a.isArray()) {
                        for (JsonNode objNode : a) {
                            ziparg.add(objNode);
                        }
                    }
                } else {
                    for (Object z : ExpressionUtils.toObjectList(v)) {
                        ziparg.add(z);
                    }
                }
                input.add(ziparg);
            }

            output = (List<List>) Streams.zip(input.get(0).stream(),
                    input.get(1).stream(),
                    (a, b) -> collectElements(a, b))
                    .collect(Collectors.toList());

            if (input.size() > 2) {
                for (int i = 2; i < input.size(); i++) {
                    output = (List<List>) Streams.zip(output.stream(),
                            input.get(i).stream(),
                            (a, b) -> collectElements(a, b))
                            .collect(Collectors.toList());
                }
            }
            return output;
        }
        return new EvalError(EvalErrorMessage.expects_at_least_two_or_more_array_args(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.arr_zip();
    }

    @Override
    public String getParams() {
        return "array a, array b, ...";
    }

    @Override
    public String getReturns() {
        return "array of arrays";
    }

    private static List collectElements(Object a, Object b) {
        List returnList = new ArrayList<>();

        if (a instanceof ArrayList) {
            ArrayList x = (ArrayList) a;
            returnList = (ArrayList) x.stream().collect(toCollection(ArrayList::new));
        } else {
            returnList.add(a);
        }

        returnList.add(b);

        return returnList;
    }
}
