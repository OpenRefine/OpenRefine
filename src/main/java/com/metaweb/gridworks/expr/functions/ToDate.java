package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import com.metaweb.gridworks.expr.util.CalendarParser;
import com.metaweb.gridworks.expr.util.CalendarParserException;
import com.metaweb.gridworks.gel.Function;

public class ToDate implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 0) {
            // missing value, can this happen?
        }
        if (!(args[0] instanceof String)) {
            // ignore cell values that aren't strings
            return null;
        }
        String o1 = (String) args[0];

        // "o, boolean month_first (optional)"
        if (args.length == 1 || (args.length == 2 && args[1] instanceof Boolean)) {
            boolean month_first = true;
            if (args.length == 2) {
                month_first = (Boolean) args[1];
            }
            try {
                return CalendarParser.parse( o1, (month_first) ? CalendarParser.MM_DD_YY : CalendarParser.DD_MM_YY);
            } catch (CalendarParserException e) {
                // do something about 
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
                    // ignore
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


    public void write(JSONWriter writer, Properties options)
    throws JSONException {

        writer.object();
        writer.key("description"); writer.value("Returns o converted to a date object, you can hint if the day or the month is listed first, or give an ordered list of possible formats using this syntax: http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html");
        writer.key("params"); writer.value("o, boolean month_first / format1, format2, ... (all optional)");
        writer.key("returns"); writer.value("date");
        writer.endObject();
    }
}
