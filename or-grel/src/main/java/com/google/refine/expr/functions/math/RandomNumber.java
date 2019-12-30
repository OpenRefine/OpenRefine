/*

Copyright 2010, Knowledge Integration Ltd.
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
    * Neither the name of Knowledge Integration Ltd. nor the names of its
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

package com.google.refine.expr.functions.math;

import java.util.Properties;

import java.util.concurrent.ThreadLocalRandom;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class RandomNumber implements Function {
    
    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2 && args[0] != null && args[0] instanceof Number
                && args[1] != null && args[1] instanceof Number && ((Number) args[0]).intValue()<((Number) args[1]).intValue()) {
            int randomNum = ThreadLocalRandom.current().nextInt(((Number) args[0]).intValue(), ((Number) args[1]).intValue()+1);
            return randomNum;
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects two numbers, the first must be less than the second");
    }

    @Override
    public String getDescription() {
        return "Returns a pseudo-random integer between the lower and upper bound (inclusive)";
    }
    
    @Override
    public String getParams() {
        return "number lower bound, number upper bound";
    }
    
    @Override
    public String getReturns() {
        return "number";
    }
    
}