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

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class SplitByLengths implements Function {
    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 2 && args[0] != null) {
            Object o = args[0];
            String s = o instanceof String ? (String) o : o.toString();
            
            String[] results = new String[args.length - 1];
            
            int lastIndex = 0;
            
            for (int i = 1; i < args.length; i++) {
                int thisIndex = lastIndex;
                
                Object o2 = args[i];
                if (o2 instanceof Number) {
                    thisIndex = Math.min(s.length(), lastIndex + Math.max(0, ((Number) o2).intValue()));
                }
                
                results[i - 1] = s.substring(lastIndex, thisIndex);
                lastIndex = thisIndex;
            }
            
            return results;
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 1 string and 1 or more numbers");
    }
    
    @Override
    public String getDescription() {
        return "Returns the array of strings obtained by splitting s into substrings with the given lengths. For example, \"internationalization\".splitByLengths(5, 6, 3) returns an array of 3 strings: [ \"inter\", \"nation\", \"ali\" ]. Excess characters are discarded.";
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
