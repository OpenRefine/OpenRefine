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
import org.apache.commons.text.WordUtils;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class ToTitlecase implements Function {

    final static private char[] delimiters = { ' ', '\t', '\r', '\n', '.' };

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] != null) {
            Object o = args[0];
            String s = o instanceof String ? (String) o : o.toString();
            return WordUtils.capitalizeFully(s, delimiters);
        } else if (args.length == 2 && args[0] != null && args[1] != null) {
            Object o = args[0];
            String s = o instanceof String ? (String) o : o.toString();
            o = args[1];
            String delims = o instanceof String ? (String) o : o.toString();
            return WordUtils.capitalizeFully(s, delims.toCharArray());
        } else {
            return new EvalError(EvalErrorMessage.expects_one_or_two_strings(ControlFunctionRegistry.getFunctionName(this)));
        }
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_to_title_case();
    }

    @Override
    public String getParams() {
        return "string s";
    }

    @Override
    public String getReturns() {
        return "string";
    }

}
