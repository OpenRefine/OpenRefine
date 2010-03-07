package com.metaweb.gridworks.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class ValueNode implements Node {
    final public Object value;
    final public String    valueType;
    final public String lang;
    
    public ValueNode(Object value, String valueType, String lang) {
        this.value = value;
        this.valueType = valueType;
        this.lang = lang;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("nodeType"); writer.value("value");
        writer.key("value"); writer.value(value);
        writer.key("valueType"); writer.value(valueType);
        writer.key("lang"); writer.value(lang);
        writer.endObject();
    }
}
