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

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class DatePart implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2 && 
                args[0] != null && (args[0] instanceof Calendar || args[0] instanceof Date) && 
                args[1] != null && args[1] instanceof String) {
            
            String part = (String) args[1];
            if (args[0] instanceof Calendar) {
                return getPart((Calendar) args[0], part);
            } else {
                Calendar c = Calendar.getInstance();
                c.setTime((Date) args[0]);
                return getPart(c, part);
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a date and a string");
    }
    
    static private String[] s_daysOfWeek = new String[] {
        "Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    private Object getPart(Calendar c, String part) {
        if ("hours".equals(part) || "hour".equals(part) || "h".equals(part)) {
            return c.get(Calendar.HOUR_OF_DAY);
        } else if ("minutes".equals(part) || "minute".equals(part) || "min".equals(part)) { // avoid 'm' to avoid confusion with month
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
            return new EvalError("Date unit '" + part + "' not recognized.");
        }
    }
    
    @Override
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        writer.key("description"); writer.value("Returns part of a date");
        writer.key("params"); writer.value("date d, string part");
        writer.key("returns"); writer.value("date");
        writer.endObject();
    }
}
