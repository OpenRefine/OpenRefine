package com.metaweb.gridworks.expr.functions.date;

import java.util.Calendar;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Function;

public class Now implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 0) {
            return Calendar.getInstance();
        }
        return null;
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the current time");
        writer.key("returns"); writer.value("date");
        writer.endObject();
    }
}
