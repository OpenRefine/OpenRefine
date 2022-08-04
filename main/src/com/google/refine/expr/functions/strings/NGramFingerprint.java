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
import java.util.TreeSet;

import com.google.refine.clustering.binning.Keyer;
import com.google.refine.clustering.binning.NGramFingerprintKeyer;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class NGramFingerprint implements Function {

    static Keyer ngram_fingerprint = new NGramFingerprintKeyer();

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 || args.length == 2) {
            if (args[0] != null) {
                int ngram_size = 1;
                if (args.length == 2 && args[1] != null) {
                    ngram_size = (args[1] instanceof Number) ? ((Number) args[1]).intValue() : Integer.parseInt(args[1].toString());
                }
                Object o = args[0];
                String s = (o instanceof String) ? (String) o : o.toString();
                return ngram_fingerprint.key(s, ngram_size);
            }
            return null;
        }
        return new EvalError(EvalErrorMessage.expects_at_least_one_string(ControlFunctionRegistry.getFunctionName(this)));
    }

    protected TreeSet<String> ngram_split(String s, int size) {
        TreeSet<String> set = new TreeSet<String>();
        char[] chars = s.toCharArray();
        for (int i = 0; i + size <= chars.length; i++) {
            set.add(new String(chars, i, size));
        }
        return set;
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_ngram_fingerprint();
    }

    @Override
    public String getParams() {
        return "string s, number n";
    }

    @Override
    public String getReturns() {
        return "string";
    }
}
