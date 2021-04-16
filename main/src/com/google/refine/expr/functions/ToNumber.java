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

package com.google.refine.expr.functions;

import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class ToNumber implements Function {

  @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] != null) {
            if (args[0] instanceof Number) {
                return args[0];
            } else {
                String s;
                if (args[0] instanceof String) {
                    s = (String)args[0];
                } else {
                    s = args[0].toString();
                }
                if (s.length() > 0) {
                    if (!s.contains(".")) { // lightweight test for strings which will definitely fail
                        try {
                            return Long.valueOf(s, 10);
                        } catch (NumberFormatException e) {
                        }
                    }
                    try {
                        return Double.valueOf(s);
                    } catch (NumberFormatException e) {
                    }
                }
                return new EvalError("Unable to parse as number");
            }
        } else {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects one non-null argument");
        }
    }

    @Override
    public String getDescription() {
        return "Returns a string converted to a number. Will attempt to convert other formats into a string, then into a number. If the value is already a number, it will return the number.";
    }
    
    @Override
    public String getParams() {
        return "o";
    }
    
    @Override
    public String getReturns() {
        return "number";
    }

}
