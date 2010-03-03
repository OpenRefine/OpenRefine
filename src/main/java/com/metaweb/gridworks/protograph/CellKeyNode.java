package com.metaweb.gridworks.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class CellKeyNode extends CellNode {
    private static final long serialVersionUID = 1684854896739592911L;
    
    final public FreebaseTopic    namespace;

    public CellKeyNode(
        String             columnName,
        FreebaseTopic    namespace
    ) {
        super(columnName);
        
        this.namespace = namespace;
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {

        writer.object();
        writer.key("nodeType"); writer.value("cell-as-key");
        writer.key("columnName"); writer.value(columnName);
        writer.key("namespace"); namespace.write(writer, options);
        writer.endObject();
    }
}
