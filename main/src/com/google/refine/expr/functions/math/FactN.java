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
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class FactN implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length != 2) {
            return new EvalError(EvalErrorMessage.expects_two_numbers(ControlFunctionRegistry.getFunctionName(this)));
        }
        if (!(args[0] instanceof Number)) {
            // a number");
            return new EvalError(EvalErrorMessage.expects_first_param_number(ControlFunctionRegistry.getFunctionName(this)));
        }
        if (!(args[1] instanceof Number)) {
            // a number");
            return new EvalError(EvalErrorMessage.expects_second_param_number(ControlFunctionRegistry.getFunctionName(this)));
        }

        return FactN.factorial(((Number) args[0]).intValue(), ((Number) args[1]).intValue());

    }

    /*
     * Calculates the factorial of an integer, i, for a decreasing step of n. e.g. A double factorial would have a step
     * of 2. Returns 1 for zero and negative integers.
     */
    public static long factorial(long i, long step) {
        if (i < 0) {
            throw new IllegalArgumentException("Can't compute the factorial of a negative number");
        } else if (i <= 1) {
            return 1;
        } else {
            long result = i * FactN.factorial(i - step, step);
            if (result < 0) {
                throw new ArithmeticException("Integer overflow computing factorial");
            }
            return result;
        }
    }

    @Override
    public String getDescription() {
        return FunctionDescription.math_factn();
    }

    @Override
    public String getParams() {
        return "number n1, number n2";
    }

    @Override
    public String getReturns() {
        return "number";
    }
}
