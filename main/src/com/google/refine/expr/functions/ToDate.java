/*

Copyright 2010,2012. Google Inc.
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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.util.CalendarParserException;
import com.google.refine.expr.util.CalendarParser;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.util.ParsingUtilities;

public class ToDate implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        String o1;
        Boolean month_first = null;
        Locale locale = Locale.getDefault();
        Integer arg_pointer = 0; //pointer used to keep track of which argument we are parsing
        DateFormat formatter;
        OffsetDateTime date;
        
        //Check there is at least one argument
        if (args.length == 0) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects at least one argument");
        } else {
            Object arg0 = args[arg_pointer];
            //check the first argument is something that can be parsed as a date
            if (arg0 instanceof OffsetDateTime) {
                return arg0;
            } else if (arg0 instanceof Long) {
                o1 =  ((Long) arg0).toString(); // treat integers as years
            } else if (arg0 instanceof String && arg0.toString().trim().length() > 0) {
                o1 = (String) arg0;
            } else {
                // ignore cell values that aren't Date, Calendar, Long or String 
                return new EvalError("Unable to parse as date");
            }
            arg_pointer++; //increment arg_pointer to 1
        }
        
        //
        if(args.length==arg_pointer) {
            //if there is just one valid argument, we treat as if month_first set to true
            month_first = true;
        }
        
        //if there are two or more arguments work out what type of arguments they are
        if (args.length>arg_pointer) {
            //is the first argument a boolean? If so use it as the month_first option
            if(args[arg_pointer] instanceof Boolean) {
                month_first = (Boolean) args[arg_pointer];
                arg_pointer++; //increment arg_pointer to 2
            }
            //if first argument isn't Boolean, do nothing
        }
        
        //month first helper maybe set by now so try a parse
        if(month_first != null) {
            try {
               return CalendarParser.parseAsOffsetDateTime( o1, (month_first) ? CalendarParser.MM_DD_YY : CalendarParser.DD_MM_YY);
           } catch (CalendarParserException e) {
               if(args.length-arg_pointer<1) { //no more arguments to try
                   OffsetDateTime d = ParsingUtilities.stringToDate(o1);
                   if (d != null) {
                       return d;
                   } else {
                       try {
                           return javax.xml.bind.DatatypeConverter.parseDateTime(o1).getTime().toInstant().atOffset(ZoneOffset.of("Z"));
                       } catch (IllegalArgumentException e2) {
                           return new EvalError("Unable to parse as date"); 
                       }
                   }
               // if arguments >2 do nothing - there are still things we can try
               }
           }
       }
        
        //check if we have still have arguments to parse
        if (args.length-arg_pointer>=1) {
            if(args[arg_pointer] instanceof String);
            String localeString  = StringUtils.trim((String) args[arg_pointer]);
            Locale possibleLocale = Locale.forLanguageTag(localeString); // Java 1.7+ 
            for (Locale l : DateFormat.getAvailableLocales()) {
                if (l.equals(possibleLocale.toLanguageTag())) {
                    locale = possibleLocale;
                    arg_pointer++;
                    break;
                }
            }
        }
        
        while (arg_pointer < args.length) {
            if (!(args[arg_pointer] instanceof String)) {
                // skip formats that aren't strings
                continue;
            }
            String format  = StringUtils.trim((String) args[arg_pointer]);
            try {
                formatter = new SimpleDateFormat(format,locale);
            } catch (IllegalArgumentException e) {
                return new EvalError("Unknown date format");
            }
            date = parse(o1, formatter);
            if (date != null) {
                return date;
            }
            arg_pointer++;
        }
        
        //no formats, or not managed to convert to date using provided formats, so try default format
        formatter = DateFormat.getDateInstance(DateFormat.DEFAULT, locale);
        date = parse(o1, formatter);
        if (date != null) {
          return date;
        }
        //if we get here without successfully getting a date, try just a basic parse
        date = ParsingUtilities.stringToDate(o1);
        if (date != null) {
            return date;
        } else { //trying one last way
             try {
                 return javax.xml.bind.DatatypeConverter.parseDateTime(o1).getTime().toInstant().atOffset(ZoneOffset.of("Z"));
             } catch (IllegalArgumentException e2) {
                 return new EvalError("Unable to parse as date");
             }
         }
    }
    
    private OffsetDateTime parse(String o1, DateFormat formatter) {
        try {
            formatter.setTimeZone(TimeZone.getTimeZone("Z"));
            Date date = formatter.parse(o1);
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(date);
            return ParsingUtilities.calendarToOffsetDateTime(c);
        } catch (java.text.ParseException e) {
            return null;
        }
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
    throws JSONException {

        writer.object();
        writer.key("description"); writer.value("Returns o converted to a date object, you can hint if the day or the month is listed first, or give an ordered list of possible formats using this syntax: http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html");
        writer.key("params"); writer.value("o, boolean month_first / format1, format2, ... (all optional)");
        writer.key("returns"); writer.value("date");
        writer.endObject();
    }
}
