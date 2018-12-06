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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
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

public class ToDate implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        String o1;
        Boolean month_first = null;
        List<String> formats =  new ArrayList<String>();  
        OffsetDateTime date = null;
        
        //Check there is at least one argument
        if (args.length == 0) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects at least one argument");
        } else {
            Object arg0 = args[0];
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
        }
        
        if(args.length==1) {
            date = parse(o1,true,formats);
        } else if (args.length > 1) {
            if(args[1] instanceof Boolean) {
                month_first = (Boolean) args[1];
            } else if (args[1] instanceof String) {
                formats.add(StringUtils.trim((String) args[1]));
            } else {
                return new EvalError("Invalid argument");
            }
            for(int i=2;i<args.length;i++) {
                if (!(args[i] instanceof String)) {
                    // skip formats that aren't strings
                    continue;
                }
                formats.add(StringUtils.trim((String) args[i]));
            }
            if(month_first != null) {
                date = parse(o1,month_first,formats);
            } else {
                date = parse(o1,formats);
            }
            
        }
        if(date != null) {
            return date;
        }
        return new EvalError("Unable to convert to a date");
    }
    
    private OffsetDateTime parse(String o1, Boolean month_first, List<String> formats) {
        if(month_first != null) {
            try {
               return CalendarParser.parseAsOffsetDateTime( o1, (month_first) ? CalendarParser.MM_DD_YY : CalendarParser.DD_MM_YY);
            } catch (CalendarParserException e) {
           }
        }
        return parse(o1,formats);
    }
    
    private OffsetDateTime parse(String o1, List<String> formats) {
        if(formats.size()>0) {
            String f1 = formats.get(0);
            formats.remove(0);
            return parse(o1,f1,formats);   
        } else {
            return parse(o1,Locale.getDefault(),formats);
        }
    }
    
    private OffsetDateTime parse(String o1, String f1, List<String> formats) {
        Locale locale = Locale.getDefault();
        Locale possibleLocale = Locale.forLanguageTag(f1); // Java 1.7+ 
        for (Locale l : DateFormat.getAvailableLocales()) {
            if (l.equals(possibleLocale)) {
                locale = possibleLocale;
            } else {
                formats.add(0,f1);
            }
        }
        return parse(o1,locale,formats);
    }
    
    private OffsetDateTime parse(String o1, Locale locale, List<String> formats) {
        DateFormat formatter;
        OffsetDateTime date;
        //need to try using each format in the formats list!
        if(formats.size()>0) {
            for(int i=0;i<formats.size();i++) {
                try {
                    formatter = new SimpleDateFormat(formats.get(i),locale);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                date = parse(o1, formatter);
                if (date != null) {
                    return date;
                }
            }
        } 
        date = ParsingUtilities.stringToDate(o1);
        if (date != null) {
            return date;
        } else {
            try {
                return javax.xml.bind.DatatypeConverter.parseDateTime(o1).getTime().toInstant()
                		.plusSeconds(ZonedDateTime.now().getOffset().getTotalSeconds())
                		.atOffset(ZoneOffset.of("Z"));
            } catch (IllegalArgumentException e2) {
                return null;
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
    public String getDescription() {
        return "Returns o converted to a date object, you can hint if the day or the month is listed first, or give an ordered list of possible formats using this syntax: http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html";
    }
    
    @Override
    public String getParams() {
        return "o, boolean month_first / format1, format2, ... (all optional)";
    }
    
    @Override
    public String getReturns() {
        return "date";
    }
}
