/*
Copyright (c) 2013, Jesus M. Castagnetto
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

- Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
- Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.google.refine.expr.functions.booleans;

import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class Xor implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 2 && args[0] instanceof Boolean && args[1] instanceof Boolean) {
            return (Boolean) args[0] ^ (Boolean) args[1];
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 2 or more booleans");
    }

    @Override
    public String getDescription() {
    	return "Uses the logical operator XOR (exclusive-or) on two or more booleans to output a boolean. Evaluates multiple statements, then returns true if only one of them is true. For example, (1 < 3).xor(1 < 7) returns false because more than one of the conditions is true.";
    }
    
    @Override
    public String getParams() {
        return "boolean a, boolean b, ...";
    }
    
    @Override
    public String getReturns() {
        return "boolean";
    }
}
