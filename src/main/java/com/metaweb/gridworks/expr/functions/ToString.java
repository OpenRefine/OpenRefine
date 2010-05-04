package com.metaweb.gridworks.expr.functions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.gel.Function;

public class ToString implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 1) {
            Object o1 = args[0];
            if (o1 != null) {
                if (o1 instanceof Calendar) {
                    if (args.length == 2) {
                       Object o2 = args[1];
                       if (o2 != null && o2 instanceof String) {
                           SimpleDateFormat formatter = new SimpleDateFormat((String) o2);
                           return formatter.format(((Calendar) o1).getTime());
                       }
                    }
                } else {
                    return (o1 instanceof String) ? o1 : o1.toString();
                }
            }
        }
        return null;
    }

    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns o converted to a string");
        writer.key("params"); writer.value("o, string format (optional)");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}
