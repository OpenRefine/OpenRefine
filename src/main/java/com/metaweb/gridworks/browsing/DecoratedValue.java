package com.metaweb.gridworks.browsing;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

/**
 * Store a value and its text label, in case the value is not a string itself.
 * For instance, if a value is a date, then its label can be one particular
 * rendering of that date.
 * 
 * Facet choices that are presented to the user as text are stored as decorated values.
 */
public class DecoratedValue implements Jsonizable {
    final public Object value;
    final public String label;
    
    public DecoratedValue(Object value, String label) {
        this.value = value;
        this.label = label;
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("v"); writer.value(value);
        writer.key("l"); writer.value(label);
        writer.endObject();
    }
}
