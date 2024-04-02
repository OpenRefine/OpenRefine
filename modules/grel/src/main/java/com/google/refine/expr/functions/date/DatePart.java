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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class DatePart implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2 &&
                args[0] != null && (args[0] instanceof Calendar || args[0] instanceof Date || args[0] instanceof OffsetDateTime) &&
                args[1] != null && args[1] instanceof String) {
            String part = (String) args[1];
            if (args[0] instanceof Calendar) {
                return getPart((Calendar) args[0], part);
            } else if (args[0] instanceof Date) {
                Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                c.setTime((Date) args[0]);
                return getPart(c, part);
            } else {
                return getPart((OffsetDateTime) args[0], part);
            }
        }
        return new EvalError(EvalErrorMessage.expects_date_and_string(ControlFunctionRegistry.getFunctionName(this)));
    }

    private Object getPart(OffsetDateTime offsetDateTime, String part) {
        if ("hours".equals(part) || "hour".equals(part) || "h".equals(part)) {
            return offsetDateTime.getHour();
        } else if ("minutes".equals(part) || "minute".equals(part) || "min".equals(part)) { // avoid 'm' to avoid
                                                                                            // confusion with month
            return offsetDateTime.getMinute();
        } else if ("seconds".equals(part) || "sec".equals(part) || "s".equals(part)) {
            return offsetDateTime.getSecond();
        } else if ("milliseconds".equals(part) || "ms".equals(part) || "S".equals(part)) {
            return Math.round(offsetDateTime.getNano() / 1000);
        } else if ("nanos".equals(part) || "nano".equals(part) || "n".equals(part)) {
            // JSR-310 is based on nanoseconds, not milliseconds.
            return offsetDateTime.getNano();
        } else if ("years".equals(part) || "year".equals(part)) {
            return offsetDateTime.getYear();
        } else if ("months".equals(part) || "month".equals(part)) { // avoid 'm' to avoid confusion with minute
            return offsetDateTime.getMonth().getValue();
        } else if ("weeks".equals(part) || "week".equals(part) || "w".equals(part)) {
            return getWeekOfMonth(offsetDateTime);
        } else if ("days".equals(part) || "day".equals(part) || "d".equals(part)) {
            return offsetDateTime.getDayOfMonth();
        } else if ("weekday".equals(part)) {
            return offsetDateTime.getDayOfWeek().name();
        } else if ("time".equals(part)) { // get Time In Millis
            return offsetDateTime.toInstant().toEpochMilli();
        } else {
            return new EvalError(EvalErrorMessage.unrecognized_date_part(part));
        }
    }

    private int getWeekOfMonth(OffsetDateTime offsetDateTime) {
        LocalDate date = offsetDateTime.toLocalDate();
        DayOfWeek firstDayOfWeek = DayOfWeek.SUNDAY;
        int minDays = 1;
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField womField = week.weekOfMonth();

        return date.get(womField);
    }

    static private String[] s_daysOfWeek = new String[] {
            "Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    private Object getPart(Calendar c, String part) {
        if ("hours".equals(part) || "hour".equals(part) || "h".equals(part)) {
            return c.get(Calendar.HOUR_OF_DAY);
        } else if ("minutes".equals(part) || "minute".equals(part) || "min".equals(part)) { // avoid 'm' to avoid
                                                                                            // confusion with month
            return c.get(Calendar.MINUTE);
        } else if ("seconds".equals(part) || "sec".equals(part) || "s".equals(part)) {
            return c.get(Calendar.SECOND);
        } else if ("milliseconds".equals(part) || "ms".equals(part) || "S".equals(part)) {
            return c.get(Calendar.MILLISECOND);
        } else if ("years".equals(part) || "year".equals(part)) {
            return c.get(Calendar.YEAR);
        } else if ("months".equals(part) || "month".equals(part)) { // avoid 'm' to avoid confusion with minute
            return c.get(Calendar.MONTH) + 1; // ISSUE 115 - people expect January to be 1 not 0
        } else if ("weeks".equals(part) || "week".equals(part) || "w".equals(part)) {
            return c.get(Calendar.WEEK_OF_MONTH);
        } else if ("days".equals(part) || "day".equals(part) || "d".equals(part)) {
            return c.get(Calendar.DAY_OF_MONTH);
        } else if ("weekday".equals(part)) {
            return s_daysOfWeek[c.get(Calendar.DAY_OF_WEEK)];
        } else if ("time".equals(part)) {
            return c.getTimeInMillis();
        } else {
            return new EvalError(EvalErrorMessage.unrecognized_date_part(part));
        }
    }

    @Override
    public String getDescription() {
        return FunctionDescription.date_part();
    }

    @Override
    public String getParams() {
        return "date d, string timeUnit";
    }

    @Override
    public String getReturns() {
        return "date";
    }
}
