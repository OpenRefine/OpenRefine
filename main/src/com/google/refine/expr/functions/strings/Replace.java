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

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class Replace implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 3) {
            Object o1 = args[0];
            Object o2 = args[1];
            Object o3 = args[2];
            if (o1 != null && o2 != null && o3 != null && o3 instanceof String) {
                String str = (o1 instanceof String) ? (String) o1 : o1.toString();
                
                if (o2 instanceof String) {
                    return str.replace((String) o2, (String) o3);
                } else if (o2 instanceof Pattern) {
                    Pattern pattern = (Pattern) o2;
                    return pattern.matcher(str).replaceAll((String) o3);
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 3 strings, or 1 string, 1 regex, and 1 string");
    }

    
    @Override
    public String getDescription() {
        return "Returns the string obtained by replacing the find string with the replace string in the inputted string. For example, 'The cow jumps over the moon and moos'.replace('oo', 'ee') returns the string 'The cow jumps over the meen and mees'. Find can be a regex pattern.";
    }
    
    @Override
    public String getParams() {
        return "string s, string or regex find, string replace";
    }
    
    @Override
    public String getReturns() {
        return "string";
    }
}
