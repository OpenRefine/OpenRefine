package com.metaweb.gridworks.expr.functions.date;

import java.util.Calendar;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class Inc implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 3 && 
                args[0] != null && args[0] instanceof Calendar && 
                args[1] != null && args[1] instanceof Number && 
                args[2] != null && args[2] instanceof String) {
            Calendar date = (Calendar) args[0];
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
    
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        writer.key("description"); writer.value("Returns a date changed by the given amount in the given unit of time");
        writer.key("params"); writer.value("date d, number value, string unit (default to 'hour')");
        writer.key("returns"); writer.value("date");
        writer.endObject();
    }
}
