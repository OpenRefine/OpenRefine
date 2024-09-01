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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Spliterator;

import com.fasterxml.jackson.databind.node.ArrayNode;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class Zip implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 2) {
            List<Spliterator> iterators = new ArrayList<>();
            for (Object v : args) {
                if (v == null || !(v.getClass().isArray() || v instanceof List<?> || v instanceof ArrayNode)) {
                    return new EvalError(
                            EvalErrorMessage.expects_at_least_two_or_more_array_args(ControlFunctionRegistry.getFunctionName(this)));
                } else if (v.getClass().isArray()) {
                    iterators.add(Arrays.stream((Object[]) v).spliterator());
                } else if (v instanceof ArrayNode) {
                    iterators.add(((ArrayNode) v).spliterator());
                } else {
                    iterators.add(((List<Object>) v).spliterator());
                }
            }

            List<List> output = new ArrayList<>();
            boolean done = false;
            while (!done) {
                List<Object> currentElements = new ArrayList<>();
                for (Spliterator<?> iterator : iterators) {
                    if (!iterator.tryAdvance(e -> currentElements.add(e))) {
                        done = true;
                    }
                }
                if (!done) {
                    output.add(currentElements);
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

}
