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

import java.util.Properties;

import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.FunctionDescription;
import org.apache.commons.lang3.StringUtils;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class NGram implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object s = args[0];
            Object n = args[1];

            if (s != null && s instanceof String && n != null && n instanceof Number) {

                String[] tokens = StringUtils.split((String) s);

                int count = ((Number) n).intValue();
                if (count >= tokens.length) {
                    return new String[] { (String) s };
                }

                int len = tokens.length - count + 1;
                String[] ngrams = new String[len];
                for (int i = 0; i < len; i++) {
                    String[] ss = new String[count];
                    for (int j = 0; j < count; j++) {
                        ss[j] = tokens[i + j];
                    }
                    ngrams[i] = StringUtils.join(ss, ' ');
                }

                return ngrams;
            }

            return null;
        }
        return new EvalError(EvalErrorMessage.expects_one_string_and_number(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_ngram();
    }

    @Override
    public String getParams() {
        return "string s, number n";
    }

    @Override
    public String getReturns() {
        return "array of strings";
    }
}
