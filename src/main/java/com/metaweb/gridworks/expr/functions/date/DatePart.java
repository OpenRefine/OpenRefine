package com.metaweb.gridworks.expr.functions.date;

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class DatePart implements Function {

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
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a date, a number and a string");
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
            
        } else if ("years".equals(part) || "year".equals(part)) {
            return c.get(Calendar.YEAR);
        } else if ("months".equals(part) || "month".equals(part)) { // avoid 'm' to avoid confusion with minute
            return c.get(Calendar.MONTH);
        } else if ("weeks".equals(part) || "week".equals(part) || "w".equals(part)) {
            return c.get(Calendar.WEEK_OF_MONTH);
        } else if ("days".equals(part) || "day".equals(part) || "d".equals(part)) {
            return c.get(Calendar.DAY_OF_MONTH);
        } else if ("weekday".equals(part)) {
            int r = c.get(Calendar.DAY_OF_WEEK);
            
            return s_daysOfWeek[r];
            
        } else if ("time".equals(part)) {
            return c.getTimeInMillis();
            
        } else {
            return new EvalError("Date unit '" + part + "' not recognized.");
        }
    }
    
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        writer.key("description"); writer.value("Returns part of a date");
        writer.key("params"); writer.value("date d, string part");
        writer.key("returns"); writer.value("date");
        writer.endObject();
    }
}
