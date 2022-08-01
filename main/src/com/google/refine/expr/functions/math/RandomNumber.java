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
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class RandomNumber implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 0) {
            // Return a double in the interval 0.0 <= x < 1.0
            return ThreadLocalRandom.current().nextDouble();
        } else if (args.length == 2) {
            // Return a double in the interval lowerBound <= x < upperBound
            if (args[0] instanceof Number && args[1] instanceof Number
                    && ((Number) args[0]).intValue() < ((Number) args[1]).intValue()) {

                // check if arguments are long
                if (args[0] instanceof Long && args[1] instanceof Long) {
                    // return a long
                    return ThreadLocalRandom.current().nextLong(((Number) args[0]).longValue(), ((Number) args[1]).longValue() + 1);
                } else {
                    // return a double
                    return ThreadLocalRandom.current().nextDouble(((Number) args[0]).doubleValue(), ((Number) args[1]).doubleValue());
                }
            }
        }
        // the first must be less than the second");
        return new EvalError(EvalErrorMessage.expects_no_arg_or_two_numbers_asc(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.math_random_number();
    }

    @Override
    public String getParams() {
        return "number lowerBound, number upperBound";
    }

    @Override
    public String getReturns() {
        return "number";
    }

}
