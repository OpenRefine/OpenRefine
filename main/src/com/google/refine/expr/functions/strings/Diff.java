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

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.FunctionDescription;
import org.apache.commons.lang3.StringUtils;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.Function;

public class Diff implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 2) {
            Object o1 = args[0];
            Object o2 = args[1];
            if (o1 != null && o2 != null) {
                if (args.length == 2 && o1 instanceof String && o2 instanceof String) {
                    return StringUtils.difference((String) o1, (String) o2);
                } else if (o1 instanceof OffsetDateTime && o2 instanceof OffsetDateTime && args.length == 3) {
                    Object o3 = args[2];
                    if (o3 != null && o3 instanceof String) {
                        String unit = ((String) o3).toLowerCase();
                        OffsetDateTime c1 = (OffsetDateTime) o1;
                        OffsetDateTime c2 = (OffsetDateTime) o2;
                        try {
                            if ("nanos".equals(unit)) {
                                return ChronoUnit.NANOS.between(c2, c1);
                            }
                            if ("milliseconds".equals(unit)) {
                                return ChronoUnit.MILLIS.between(c2, c1);
                            }
                            if ("seconds".equals(unit)) {
                                return ChronoUnit.SECONDS.between(c2, c1);
                            }
                            if ("minutes".equals(unit)) {
                                return ChronoUnit.MINUTES.between(c2, c1);
                            }
                            if ("hours".equals(unit)) {
                                return ChronoUnit.HOURS.between(c2, c1);
                            }
                            if ("days".equals(unit)) {
                                return ChronoUnit.DAYS.between(c2, c1);
                            }
                            if ("weeks".equals(unit)) {
                                return ChronoUnit.WEEKS.between(c2, c1);
                            }
                            if ("months".equals(unit)) {
                                return ChronoUnit.MONTHS.between(c2, c1);
                            }
                            if ("years".equals(unit)) {
                                return ChronoUnit.YEARS.between(c2, c1);
                            }
                            return new EvalError(EvalErrorMessage.unknown_time_unit(unit));
                        } catch (ArithmeticException arithmeticException) {
                            return new EvalError(EvalErrorMessage.string_diff_long_overflow(unit));
                        }
                    }
                }
            }
        }
        return new EvalError(
                EvalErrorMessage.expects_two_strings_or_two_dates_and_unit_string(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_diff();
    }

    @Override
    public String getParams() {
        return "o1, o2, time unit (optional)";
    }

    @Override
    public String getReturns() {
        return "string for strings, number for dates";
    }
}
