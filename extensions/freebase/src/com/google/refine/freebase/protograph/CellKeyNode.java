package com.google.refine.freebase.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.freebase.FreebaseTopic;

public class CellKeyNode extends CellNode {
    final public FreebaseTopic namespace;

    public CellKeyNode(
        FreebaseTopic namespace
    ) {
        this.namespace = namespace;
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {

        writer.object();
        writer.key("nodeType"); writer.value("cell-as-key");
        
        writer.key("columnNames");
        writer.array();
        for (String name : columnNames) {
            writer.value(name);
        }
        writer.endArray();
        
        writer.key("namespace"); namespace.write(writer, options);
        writer.endObject();
    }
}
