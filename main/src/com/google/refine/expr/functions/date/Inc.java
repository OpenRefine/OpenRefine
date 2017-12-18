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

public class Inc implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 3 && 
                args[0] != null && (args[0] instanceof Calendar || args[0] instanceof Date) && 
                args[1] != null && args[1] instanceof Number && 
                args[2] != null && args[2] instanceof String) {
            Calendar date;
            if (args[0] instanceof Calendar) {
                date = (Calendar) ((Calendar) args[0]).clone(); // must copy so not to modify original
            } else {
                date = Calendar.getInstance();
                date.setTime((Date) args[0]);
            }
            
            int amount = ((Number) args[1]).intValue();
            String unit = (String) args[2];
            
            date.add(getField(unit), amount);
            
            return date; 
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a date, a number and a string");
    }

    private int getField(String unit) {
        if ("hours".equals(unit) || "hour".equals(unit) || "h".equals(unit)) {
            return Calendar.HOUR;
        } else if ("days".equals(unit) || "day".equals(unit) || "d".equals(unit)) {
            return Calendar.DAY_OF_MONTH;
        } else if ("years".equals(unit) || "year".equals(unit)) {
            return Calendar.YEAR;
        } else if ("months".equals(unit) || "month".equals(unit)) { // avoid 'm' to avoid confusion with minute
            return Calendar.MONTH;
        } else if ("minutes".equals(unit) || "minute".equals(unit) || "min".equals(unit)) { // avoid 'm' to avoid confusion with month
            return Calendar.MINUTE;
        } else if ("weeks".equals(unit) || "week".equals(unit) || "w".equals(unit)) {
            return Calendar.WEEK_OF_MONTH;
        } else if ("seconds".equals(unit) || "sec".equals(unit) || "s".equals(unit)) {
            return Calendar.SECOND;
        } else {
            throw new RuntimeException("Unit '" + unit + "' not recognized.");
        }
    }
    
    @Override
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        writer.key("description"); writer.value("Returns a date changed by the given amount in the given unit of time");
        writer.key("params"); writer.value("date d, number value, string unit (default to 'hour')");
        writer.key("returns"); writer.value("date");
        writer.endObject();
    }
}
