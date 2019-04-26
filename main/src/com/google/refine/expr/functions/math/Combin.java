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

package com.google.refine.expr.functions.math;

import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class Combin implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if(args.length != 2) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects two numbers");
        }
        if(args[0] == null || !(args[0] instanceof Number)) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects the first argument to be a number");
        }
        if(args[1] == null || !(args[1] instanceof Number)) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects the second argument to be a number");
        }

        return Combin.combination(((Number) args[0]).intValue(), ((Number) args[1]).intValue());
    }

    /*
     * Compute binomial coefficient using dynamic programming which takes 
     * advantage of Pascal's identity as described in:
     * http://introcs.cs.princeton.edu/java/96optimization/
     */
    public static long combination(int n, int k) {
        long[][] binomial = new long[n+1][k+1];

        for (int j = 1; j <= k; j++) {
            binomial[0][j] = 0;
        }
        for (int i = 0; i <= n; i++) {
            binomial[i][0] = 1;
        }

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= k; j++) {
                binomial[i][j] = binomial[i-1][j-1] + binomial[i-1][j];
                if (binomial[i][j] > Long.MAX_VALUE || binomial[i][j] < 0) {
                    throw new RuntimeException("Range limit exceeded");
                }
            }
        }

        return binomial[n][k];
    }

    @Override
    public String getDescription() {
        return "Returns the number of combinations for n elements as divided into k";
    }
    
    @Override
    public String getParams() {
        return "number d";
    }
    
    @Override
    public String getReturns() {
        return "number";
    }
}