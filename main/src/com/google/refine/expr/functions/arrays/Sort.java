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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;
import com.google.refine.util.JSONUtilities;

public class Sort implements Function {

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1) {
            Object v = args[0];
            if (v != null) {
                if (v.getClass().isArray()) {
                    Object[] a = (Object[]) v;
                    Comparable[] r = new Comparable[a.length];
                    for (int i = 0; i < r.length; i++) {
                        if (a[i] instanceof Comparable) {
                            r[i] = (Comparable) a[i];
                        } else if (a[i] == null) {
                            r[i] = null;
                        } else {
                            // of uniform type");
                            return new EvalError(EvalErrorMessage.expects_one_array_uniform(ControlFunctionRegistry.getFunctionName(this)));
                        }
                    }
                    Arrays.sort(r, Comparator.nullsLast(Comparator.naturalOrder()));
                    return r;
                } else if (v instanceof ArrayNode) {
                    // FIXME: Probably need a test for this
                    // Comparable[] is a (slight) lie since nulls can be included, but our comparator will handle them
                    Comparable[] r = (Comparable[]) JSONUtilities.toSortableArray((ArrayNode) v);
                    Arrays.sort(r, Comparator.nullsLast(Comparator.naturalOrder()));
                    return r;
                } else if (v instanceof List<?>) {
                    List<? extends Comparable<Object>> a = (List<? extends Comparable<Object>>) v;
                    Collections.sort(a, Comparator.nullsLast(Comparator.naturalOrder()));
                    return a;
                }
            }
        }
        return new EvalError(EvalErrorMessage.expects_one_array(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.arr_sort();
    }

    @Override
    public String getParams() {
        return "array a of uniform type";
    }

    @Override
    public String getReturns() {
        return "array";
    }
}
