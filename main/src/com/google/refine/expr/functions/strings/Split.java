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
import java.util.regex.Pattern;

import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.FunctionDescription;
import org.apache.commons.lang3.StringUtils;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class Split implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 2 && args.length <= 3) {
            boolean preserveAllTokens = false;

            Object v = args[0];
            Object split = args[1];
            if (args.length == 3) {
                Object preserve = args[2];
                if (preserve instanceof Boolean) {
                    preserveAllTokens = ((Boolean) preserve);
                }
            }

            if (v != null && split != null) {
                String str = (v instanceof String ? (String) v : v.toString());
                if (split instanceof String) {
                    return preserveAllTokens ? StringUtils.splitByWholeSeparatorPreserveAllTokens(str, (String) split)
                            : StringUtils.splitByWholeSeparator(str, (String) split);
                } else if (split instanceof Pattern) {
                    Pattern pattern = (Pattern) split;
                    return pattern.split(str);
                }
            }
        }
        // regex, followed by an optional boolean");
        return new EvalError(EvalErrorMessage.expects_two_strings_as_string_regex_opt_bool(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_split();
    }

    @Override
    public String getParams() {
        return "string s, string or regex sep, optional boolean preserveTokens";
    }

    @Override
    public String getReturns() {
        return "array";
    }
}
