package com.metaweb.gridworks.expr;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

/**
 * An error that occurs during the evaluation of an Evaluable. Errors are values, too
 * because they can be stored in cells just like strings, numbers, etc. Errors are not
 * thrown because an error might occupy just one element in an array and doesn't need
 * to make the whole array erroneous.
 */
public class EvalError implements Serializable, Jsonizable {
    private static final long serialVersionUID = -102681220092874080L;
    
    final public String message;
    
    public EvalError(String message) {
        this.message = message;
    }
    
    public String toString() {
        return this.message;
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("type"); writer.value("error");
        writer.key("message"); writer.value(message);
        writer.endObject();
    }

}
