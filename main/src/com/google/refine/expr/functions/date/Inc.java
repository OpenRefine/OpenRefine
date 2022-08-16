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

package com.google.refine.expr.functions.date;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class Inc implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 3 &&
                args[0] != null && (args[0] instanceof OffsetDateTime) &&
                args[1] != null && args[1] instanceof Number &&
                args[2] != null && args[2] instanceof String) {
            OffsetDateTime date = (OffsetDateTime) args[0];

            int amount = ((Number) args[1]).intValue();
            String unit = (String) args[2];

            return date.plus(amount, getField(unit));
        }
        // string");
        return new EvalError(EvalErrorMessage.expects_date_number_string(ControlFunctionRegistry.getFunctionName(this)));
    }

    private TemporalUnit getField(String unit) {
        if ("hours".equals(unit) || "hour".equals(unit) || "h".equals(unit)) {
            return ChronoUnit.HOURS;
        } else if ("days".equals(unit) || "day".equals(unit) || "d".equals(unit)) {
            return ChronoUnit.DAYS;
        } else if ("years".equals(unit) || "year".equals(unit)) {
            return ChronoUnit.YEARS;
        } else if ("months".equals(unit) || "month".equals(unit)) { // avoid 'm' to avoid confusion with minute
            return ChronoUnit.MONTHS;
        } else if ("minutes".equals(unit) || "minute".equals(unit) || "min".equals(unit)) { // avoid 'm' to avoid
                                                                                            // confusion with month
            return ChronoUnit.MINUTES;
        } else if ("weeks".equals(unit) || "week".equals(unit) || "w".equals(unit)) {
            return ChronoUnit.WEEKS;
        } else if ("seconds".equals(unit) || "sec".equals(unit) || "s".equals(unit)) {
            return ChronoUnit.SECONDS;
        } else if ("milliseconds".equals(unit) || "ms".equals(unit) || "S".equals(unit)) {
            return ChronoUnit.MILLIS;
        } else if ("nanos".equals(unit) || "nano".equals(unit) || "n".equals(unit)) {
            return ChronoUnit.NANOS;
        } else {
            throw new RuntimeException("Unit '" + unit + "' not recognized.");
        }
    }

    @Override
    public String getDescription() {
        return FunctionDescription.date_inc();
    }

    @Override
    public String getParams() {
        return "date d, number n, string unit";
    }

    @Override
    public String getReturns() {
        return "date";
    }
}
