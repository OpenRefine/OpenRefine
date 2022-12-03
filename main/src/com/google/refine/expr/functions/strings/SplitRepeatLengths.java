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

package com.google.refine.expr.functions.strings;

import java.util.ArrayList;
import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class SplitRepeatLengths implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length < 2 || args[0] == null) {
            return new EvalError(EvalErrorMessage.expects_one_string_and_at_least_one_number(ControlFunctionRegistry.getFunctionName(this)));
        }
        Object o = args[0];
        String inputString = o instanceof String ? (String) o : o.toString();
        ArrayList<String> dynamicResults = new ArrayList<>();
        int startPointer = 0;
        int endPointer = 0;
        out: while (endPointer < inputString.length()) {
            for (int i = 1; i < args.length; i++) {
                Object o2 = args[i];
                if (o2 instanceof Number) {
                    endPointer = Math.min(endPointer + ((Number) o2).intValue(), inputString.length());
                    if (endPointer >= inputString.length()) {
                        dynamicResults.add(inputString.substring(startPointer));
                        break out;
                    }
                    dynamicResults.add(inputString.substring(startPointer, endPointer));
                    startPointer = endPointer;
                } else {
                    return new EvalError(EvalErrorMessage.unable_to_parse_as_number());
                }
            }
        }
        return dynamicResults.toArray();
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_split_repeat_lengths();
    }

    @Override
    public String getParams() {
        return "string s, number n1, number n2, ...";
    }

    @Override
    public String getReturns() {
        return "array";
    }
}
