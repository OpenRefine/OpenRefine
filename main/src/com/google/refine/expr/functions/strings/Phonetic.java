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

import com.google.refine.clustering.binning.ColognePhoneticKeyer;
import com.google.refine.clustering.binning.DoubleMetaphoneKeyer;
import com.google.refine.clustering.binning.Metaphone3Keyer;
import com.google.refine.clustering.binning.MetaphoneKeyer;
import com.google.refine.clustering.binning.SoundexKeyer;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class Phonetic implements Function {

    // TODO: We could probably lazily initialize these when needed for efficiency
    static private Metaphone3Keyer metaphone3 = new Metaphone3Keyer();
    static private DoubleMetaphoneKeyer metaphone2 = new DoubleMetaphoneKeyer();
    static private MetaphoneKeyer metaphone = new MetaphoneKeyer();
    static private SoundexKeyer soundex = new SoundexKeyer();
    static private ColognePhoneticKeyer cologne = new ColognePhoneticKeyer();

    @Override
    public Object call(Properties bindings, Object[] args) {
        String str;
        if (args.length > 0 && args[0] != null) {
            Object o1 = args[0];
            str = (o1 instanceof String) ? (String) o1 : o1.toString();
        } else {
            return new EvalError(EvalErrorMessage.expects_at_least_one_arg(ControlFunctionRegistry.getFunctionName(this)));
        }
        String encoding = "metaphone3";
        if (args.length > 1) {
            Object o2 = args[1];
            if (o2 != null) {
                if (o2 instanceof String) {
                    encoding = ((String) o2).toLowerCase();
                } else {
                    // + " expects a string for the second argument");
                    return new EvalError(EvalErrorMessage.expects_second_param_string(ControlFunctionRegistry.getFunctionName(this)));
                }
            } else {
                // + " expects a string for the second argument, the phonetic encoding to use.");
                return new EvalError(EvalErrorMessage.expects_second_param_string_phonetic(ControlFunctionRegistry.getFunctionName(this)));
            }
        }
        if (args.length < 3) {
            if ("doublemetaphone".equalsIgnoreCase(encoding)) {
                return metaphone2.key(str);
            } else if ("metaphone3".equalsIgnoreCase(encoding)) {
                return metaphone3.key(str);
            } else if ("metaphone".equalsIgnoreCase(encoding)) {
                return metaphone.key(str);
            } else if ("soundex".equalsIgnoreCase(encoding)) {
                return soundex.key(str);
            } else if ("cologne".equalsIgnoreCase(encoding)) {
                return cologne.key(str);
            } else {
                // + " doesn't know how to handle the '"
                // + encoding + "' encoding.");
                return new EvalError(EvalErrorMessage.unable_to_handle_encoding(ControlFunctionRegistry.getFunctionName(this), encoding));
            }
        } else {
            // + " expects one or two string arguments");
            return new EvalError(EvalErrorMessage.expects_one_or_two_strings(ControlFunctionRegistry.getFunctionName(this)));
        }
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_phonetic();
    }

    @Override
    public String getParams() {
        return "string s, string encoding (optional, defaults to 'metaphone3')";
    }

    @Override
    public String getReturns() {
        return "string";
    }
}
