/*

Copyright 2025, OpenRefine contributors
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
    * Neither the name of the copyright holder nor the names of its
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

package com.google.refine.extension.geo.functions;

import java.util.Locale;
import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;

public class DecToGMS implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length < 1 || args.length > 2) {
            return new EvalError("decToGMS() expects one or two arguments: decimal degrees and optional coordinate type");
        }

        Object decimal = args[0];
        if (!(decimal instanceof Number)) {
            return new EvalError(EvalErrorMessage.expects_first_param_number(ControlFunctionRegistry.getFunctionName(this)));
        }

        double decimalValue = ((Number) decimal).doubleValue();

        String coordType = null;
        if (args.length == 2) {
            if (args[1] != null && args[1] instanceof String) {
                coordType = ((String) args[1]).toLowerCase();
                if (!"lat".equals(coordType) && !"lng".equals(coordType) && !"lon".equals(coordType)) {
                    return new EvalError(EvalErrorMessage.expects_second_param_string(ControlFunctionRegistry.getFunctionName(this)));
                }
                if ("lon".equals(coordType)) {
                    coordType = "lng";
                }
            } else {
                return new EvalError(EvalErrorMessage.expects_second_param_string(ControlFunctionRegistry.getFunctionName(this)));
            }
        }
        if ("lat".equals(coordType) && (decimalValue < -90 || decimalValue > 90)) {
            return new EvalError("Latitude must be between -90 and 90 degrees");
        }
        if ("lng".equals(coordType) && (decimalValue < -180 || decimalValue > 180)) {
            return new EvalError("Longitude must be between -180 and 180 degrees");
        }

        return convertToGMS(decimalValue, coordType);
    }

    private String convertToGMS(double decimal, String coordType) {
        boolean isNegative = decimal < 0;
        double absDecimal = Math.abs(decimal);

        int degrees = (int) absDecimal;
        double minutesDecimal = (absDecimal - degrees) * 60;
        int minutes = (int) minutesDecimal;
        double seconds = (minutesDecimal - minutes) * 60;

        String direction = "";
        if (coordType != null) {
            if ("lat".equals(coordType)) {
                direction = isNegative ? " S" : " N";
            } else if ("lng".equals(coordType)) {
                direction = isNegative ? " W" : " E";
            }
        } else {
            if (isNegative) {
                direction = " (-)";
            }
        }
        return String.format(Locale.ROOT, "%d° %d' %.2f\"%s", degrees, minutes, seconds, direction);
    }

    @Override
    public String getDescription() {
        return "Converts decimal degrees to degrees, minutes, seconds format. Usage: decToGMS(decimal) or decToGMS(decimal, 'lat'|'lng')";
    }

    @Override
    public String getParams() {
        return "number decimal, optional string coordType";
    }

    @Override
    public String getReturns() {
        return "string";
    }
}
