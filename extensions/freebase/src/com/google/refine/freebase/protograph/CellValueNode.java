package com.google.refine.freebase.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class CellValueNode extends CellNode {
    final public String        valueType;
    final public String     lang;

    public CellValueNode(
        String  valueType, 
        String  lang
    ) {
        this.valueType = valueType;
        this.lang = lang;
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {

        writer.object();
        writer.key("nodeType"); writer.value("cell-as-value");
        writer.key("columnNames");
        writer.array();
        for (String name : columnNames) {
            writer.value(name);
        }
        writer.endArray();
        writer.key("valueType"); writer.value(valueType);
        writer.key("lang"); writer.value(lang);
        writer.endObject();
    }

}
