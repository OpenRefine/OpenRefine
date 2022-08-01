/*

Copyright 2016, Knowledge Integration Ltd
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
    * Neither the name of Knowledge Integration Ltd nor the names of its
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
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.node.ArrayNode;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;
import com.google.refine.util.JSONUtilities;

public class InArray implements Function {

    @Override
    @SuppressWarnings("unchecked")
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object v = args[0];
            Object s = args[1];

            if (v != null && s != null && s instanceof String) {
                if (v.getClass().isArray()) {
                    Object[] a = (Object[]) v;
                    Object[] r = a.clone();
                    return Arrays.asList(r).contains(s);
                } else if (v instanceof ArrayNode) {
                    Object[] r = JSONUtilities.toArray((ArrayNode) v);
                    return Arrays.asList(r).contains(s);
                } else if (v instanceof List<?>) {
                    List<? extends Comparable<Object>> a = (List<? extends Comparable<Object>>) v;
                    return a.contains(s);
                }
                return new EvalError(EvalErrorMessage.expects_one_array(ControlFunctionRegistry.getFunctionName(this)));
            }
            return new EvalError(EvalErrorMessage.expects_one_string(ControlFunctionRegistry.getFunctionName(this)));
        }
        // string");
        return new EvalError(EvalErrorMessage.expects_one_array_and_string(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.arr_in_array();
    }

    @Override
    public String getParams() {
        return "array a, string s";
    }

    @Override
    public String getReturns() {
        return "boolean";
    }
}
