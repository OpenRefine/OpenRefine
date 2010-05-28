package com.metaweb.gridworks.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class CellValueNode extends CellNode {
    final public String        valueType;
    final public String     lang;

    public CellValueNode(
        String    columnName,
        String     valueType, 
        String  lang
    ) {
        super(columnName);
        
        this.valueType = valueType;
        this.lang = lang;
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {

        writer.object();
        writer.key("nodeType"); writer.value("cell-as-value");
        writer.key("columnName"); writer.value(columnName);
        writer.key("valueType"); writer.value(valueType);
        writer.key("lang"); writer.value(lang);
        writer.endObject();
    }

}
