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
        if(args.length >= 2 && args[0] != null && args[0] instanceof Number
                && args[1] != null && args[1] instanceof Number && ((Number) args[0]).intValue()<((Number) args[1]).intValue()){
            int low = ((Number) args[0]).intValue();
            int high = ((Number) args[1]).intValue();
            int randomNum = ThreadLocalRandom.current().nextInt(low, high);
            if(args.length == 2){
                return randomNum;
            } else if (args.length == 3){
                Object o3 = args[2];
                if (o3 != null && o3 instanceof String) {
                    String type = ((String ) o3).toLowerCase();
                    if(type == "long"){
                        return (long)randomNum;
                    } else if(type == "double"){
                        double randomDoubleNum = 1.0 * Math.round(100 * (low + Math.random()*(high - low))) / 100;
                        return randomDoubleNum;
                    }
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects two numbers, the first must be less than the second\n" +
                "or two numbers and a parameter \"type\", which may be \"long\" or \"double\"");
    }

    @Override
    public String getDescription() {
        return "Returns a random integer in the interval between the lower and upper bounds (inclusively). Will output a different random number in each cell in a column.";
    }
    
    @Override
    public String getParams() {
        return "number lowerBound, number upperBound, or an optional parameter to determine the return type.";
    }
    
    @Override
    public String getReturns() {
        return "number";
    }
    
}
