/*

Copyright 2010,2011. Google Inc.
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.util.CalendarParser;
import com.google.refine.expr.util.CalendarParserException;
import com.google.refine.grel.Function;
import com.google.refine.util.ParsingUtilities;

public class ToDate implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 0) {
            // missing value, can this happen?
            return null;
        }
        Object arg0 = args[0];
        if (arg0 instanceof Date) {
            return arg0;
        } else if (arg0 instanceof Calendar) {
            return ((Calendar) arg0).getTime();
        } else if (!(arg0 instanceof String)) {
            // ignore cell values that aren't strings
            return new EvalError("Not a String - cannot parse to date");
        }
        String o1 = (String) arg0;

        // "o, boolean month_first (optional)"
        if (args.length == 1 || (args.length == 2 && args[1] instanceof Boolean)) {
            boolean month_first = true;
            if (args.length == 2) {
                month_first = (Boolean) args[1];
            }
            try {
                return CalendarParser.parse( o1, (month_first) ? CalendarParser.MM_DD_YY : CalendarParser.DD_MM_YY);
            } catch (CalendarParserException e) {
                Date d = ParsingUtilities.stringToDate(o1);
                if (d != null) {
                    return d;
                } else {
                    try {
                        return javax.xml.bind.DatatypeConverter.parseDateTime(o1).getTime();
                    } catch (IllegalArgumentException e2) {
                    }
                    // alternate implementation which may be useful on some JVMs?
//                    try {
//                        return javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(o1).toGregorianCalendar().getTime();
//                    } catch (DatatypeConfigurationException e2) {     
//                    }
                }
                return new EvalError("Cannot parse to date");
            }
        }

        // "o, format1, format2 (optional), ..."
        if (args.length>=2) {
            for (int i=1;i<args.length;i++) {
                if (!(args[i] instanceof String)) {
                    // skip formats that aren't strings
                    continue;
                }
                String format  = (String) args[i];
                SimpleDateFormat formatter = new SimpleDateFormat(format);
                Date date = null;
                try {
                    date = formatter.parse(o1);
                } catch (java.text.ParseException e) {
                    return new EvalError("Cannot parse to date");
                }
                if (date != null) {
                    GregorianCalendar c = new GregorianCalendar();
                    c.setTime(date);
                    return c;
                }
            }
        }

        return null;
    }


    @Override
    public void write(JSONWriter writer, Properties options)
    throws JSONException {

        writer.object();
        writer.key("description"); writer.value("Returns o converted to a date object, you can hint if the day or the month is listed first, or give an ordered list of possible formats using this syntax: http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html");
        writer.key("params"); writer.value("o, boolean month_first / format1, format2, ... (all optional)");
        writer.key("returns"); writer.value("date");
        writer.endObject();
    }
}
