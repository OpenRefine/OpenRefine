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

package com.google.refine.expr.functions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.util.CalendarParser;
import com.google.refine.expr.util.CalendarParserException;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.util.ParsingUtilities;

public class EpochToDate implements Function{

    @Override
    public Object call(Properties bindings, Object[] args) {
        if(args.length >= 1 && args[0] instanceof Number) {
            long epoch = (long) args[0];
            ZoneId zoneId = ZoneId.of("America/Chicago");
            Instant instant = null;
            OffsetDateTime date = null;
            if(args.length == 1){
                instant = Instant.ofEpochSecond ( epoch );
                date = OffsetDateTime.ofInstant(instant, zoneId);
                return date;
            } else if(args.length == 2 && args[1] instanceof String){
                Object o2 = args[1];
                String type = ((String ) o2).toLowerCase();
                if(type == "second"){
                    instant = Instant.ofEpochSecond ( epoch );
                    date = OffsetDateTime.ofInstant(instant, zoneId);
                    return date;
                } else if (type == "millisecond"){
                    instant = Instant.ofEpochSecond ( epoch/1000 );
                    date = OffsetDateTime.ofInstant(instant, zoneId);
                    return date;
                } else if (type == "microsecond"){
                    instant = Instant.ofEpochSecond ( epoch/1000000 );
                    date = OffsetDateTime.ofInstant(instant, zoneId);
                    return date;
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expect one argument, " +
                "which is a number of epoch time\n" +
                "expect two argument, the first is a epoch time(second, millisecond, microsecond), the second " +
                "is the type");
    }
}
