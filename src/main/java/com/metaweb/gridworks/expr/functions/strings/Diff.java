package com.metaweb.gridworks.expr.functions.strings;

import java.util.Calendar;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.CalendarParser;
import com.metaweb.gridworks.expr.CalendarParserException;
import com.metaweb.gridworks.expr.Function;

public class Diff implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 2 && args.length <= 3) {
            Object o1 = args[0];
            Object o2 = args[1];
            if (o1 != null && o2 != null) {
                if (o1 instanceof String && o2 instanceof String) {
                    return StringUtils.difference((String) o1,(String) o2);
                } else if (o1 instanceof Calendar) {
                    if (args.length == 3) {
                        Object o3 = args[3];
                        if (o3 != null && o3 instanceof String) {
                            try {
                                String unit = ((String) o3).toLowerCase();
                                Calendar c1 = (Calendar) o1;
                                Calendar c2 = (o2 instanceof Calendar) ? (Calendar) o2 : CalendarParser.parse((o2 instanceof String) ? (String) o2 : o2.toString());
                                long delta = (c1.getTimeInMillis() - c2.getTimeInMillis()) / 1000;
                                if ("seconds".equals(unit)) return delta;
                                delta /= 60;
                                if ("minutes".equals(unit)) return delta;
                                delta /= 60;
                                if ("hours".equals(unit)) return delta;
                                long days = delta / 24;
                                if ("days".equals(unit)) return days;
                                if ("weeks".equals(unit)) return days / 7;
                                if ("months".equals(unit)) return days / 30;
                                if ("years".equals(unit)) return days / 365;
                            } catch (CalendarParserException e) {
                                // we should throw at this point because it's important to know that date parsing failed
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("For strings, returns the portion where they differ. For dates, it returns the difference in given time units");
        writer.key("params"); writer.value("o1, o2, time unit (optional)");
        writer.key("returns"); writer.value("string for strings, number for dates");
        writer.endObject();
    }
}
