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

import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class UnicodeType implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] != null) {
            Object o = args[0];
            String s = (o instanceof String) ? (String) o : o.toString();
            String[] output = new String[s.length()];
            for (int i = 0; i < s.length(); i++) {
                output[i] = translateType(Character.getType(s.codePointAt(i)));
            }
            return output;
        }
        return null;
    }

    private String translateType(int type) {
        switch (type) {
            case 0:
                return "unassigned";
            case 1:
                return "uppercase letter";
            case 2:
                return "lowercase letter";
            case 3:
                return "titlecase letter";
            case 4:
                return "modifier letter";
            case 5:
                return "other letter";
            case 6:
                return "non spacing mark";
            case 7:
                return "enclosing mark";
            case 8:
                return "combining spacing mark";
            case 9:
                return "decimal digit number";
            case 10:
                return "letter number";
            case 11:
                return "other number";
            case 12:
                return "space separator";
            case 13:
                return "line separator";
            case 14:
                return "paragraph separator";
            case 15:
                return "control";
            case 16:
                return "format";
            // 17 does not seem to be used
            case 18:
                return "private use";
            case 19:
                return "surrogate";
            case 20:
                return "dash punctuation";
            case 21:
                return "start punctuation";
            case 22:
                return "end punctuation";
            case 23:
                return "connector punctuation";
            case 24:
                return "other punctuation";
            case 25:
                return "math symbol";
            case 26:
                return "currency symbol";
            case 27:
                return "modifier symbol";
            case 28:
                return "other symbol";
            case 29:
                return "initial quote punctuation";
            case 30:
                return "final quote punctuation";
            default:
                return "unknown";
        }
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_unicode_type();
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
