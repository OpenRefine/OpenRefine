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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class RPartition implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 2 && args.length <= 3) {
            Object o1 = args[0];
            Object o2 = args[1];

            boolean omitFragment = false;
            if (args.length == 3) {
                Object o3 = args[2];
                if (o3 instanceof Boolean) {
                    omitFragment = ((Boolean) o3).booleanValue();
                }
            }

            if (o1 != null && o2 != null && o1 instanceof String) {
                String s = (String) o1;

                int from = -1;
                int to = -1;

                if (o2 instanceof String) {
                    String frag = (String) o2;

                    from = s.lastIndexOf(frag);
                    to = from + frag.length();
                } else if (o2 instanceof Pattern) {
                    Pattern pattern = (Pattern) o2;
                    Matcher matcher = pattern.matcher(s);

                    while (matcher.find()) {
                        from = matcher.start();
                        to = matcher.end();
                    }
                }

                String[] output = omitFragment ? new String[2] : new String[3];
                if (from > -1) {
                    output[0] = s.substring(0, from);
                    if (omitFragment) {
                        output[1] = s.substring(to);
                    } else {
                        output[1] = s.substring(from, to);
                        output[2] = s.substring(to);
                    }
                } else {
                    output[0] = s;
                    output[1] = "";
                    if (!omitFragment) {
                        output[2] = "";
                    }
                }
                return output;
            }
        }
        return null;
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_rpartition();
    }

    @Override
    public String getParams() {
        return "string s, string or regex fragment, optional boolean omitFragment";
    }

    @Override
    public String getReturns() {
        return "array";
    }
}
