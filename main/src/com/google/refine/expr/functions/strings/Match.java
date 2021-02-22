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

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class Match implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object s = args[0];
            Object p = args[1];
            
            if (s != null && p != null && (p instanceof String || p instanceof Pattern)) {
                
                Pattern pattern = (p instanceof String) ? Pattern.compile((String) p) : (Pattern) p;

                Matcher matcher = pattern.matcher(s.toString());
                
                if (matcher.matches()) {
                    int count = matcher.groupCount();
    
                    String[] groups = new String[count];
                    for (int i = 0; i < count; i++) {
                        groups[i] = matcher.group(i + 1);
                    }
    
                    return groups;
                } else {
                    return null;
                }
            }
            
            return null;
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects regex");
    }
    
    @Override
    public String getDescription() {
        return "Attempts to match the string s in its entirety against the regex pattern p and, if the pattern is found, outputs an array of all capturing groups (found in order).";
    }
    
    @Override
    public String getParams() {
        return "string s, regex p";
    }
    
    @Override
    public String getReturns() {
        return "array of strings";
    }
}
