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

import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Metaphone;
import org.apache.commons.codec.language.Soundex;

import com.google.refine.clustering.binning.Keyer;
import com.google.refine.clustering.binning.KeyerFactory;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class Phonetic implements Function {

    // TODO deprecate and drop those legacy encodings?
    static private Metaphone metaphone = null;
    static private DoubleMetaphone doubleMetaphone = null;
    static private Soundex soundex = null;

    private Metaphone getMetaphone() {
        if (metaphone == null) {
            metaphone = new Metaphone();
            metaphone.setMaxCodeLen(2000);
        }
        return metaphone;
    }

    private DoubleMetaphone getDoubleMetaphone() {
        if (doubleMetaphone == null) {
            doubleMetaphone = new DoubleMetaphone();
            doubleMetaphone.setMaxCodeLen(2000);
        }
        return doubleMetaphone;
    }

    private Soundex getSoundex() {
        if (soundex == null) {
            soundex = new Soundex();
        }
        return soundex;
    }

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
                    return new EvalError(EvalErrorMessage.expects_second_param_string(ControlFunctionRegistry.getFunctionName(this)));
                }
            } else {
                return new EvalError(EvalErrorMessage.expects_second_param_string_phonetic(ControlFunctionRegistry.getFunctionName(this)));
            }
        }
        if (args.length < 3) {
            if ("doublemetaphone".equalsIgnoreCase(encoding)) {
                return getDoubleMetaphone().doubleMetaphone(str);
            } else if ("metaphone".equalsIgnoreCase(encoding)) {
                return getMetaphone().metaphone(str);
            } else if ("soundex".equalsIgnoreCase(encoding)) {
                return getSoundex().soundex(str);
            } else {
                Keyer keyer = KeyerFactory.get(encoding.toLowerCase());
                if (keyer != null) {
                    return keyer.key(str);
                }
                return new EvalError(EvalErrorMessage.unable_to_handle_encoding(ControlFunctionRegistry.getFunctionName(this), encoding));
            }
        } else {
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
