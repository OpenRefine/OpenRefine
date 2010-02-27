package com.metaweb.gridworks.expr;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class EvalError implements Jsonizable {
    final public String message;
    
    public EvalError(String message) {
        this.message = message;
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("type"); writer.value("error");
        writer.key("message"); writer.value(message);
        writer.endObject();
    }

}
