package com.metaweb.gridworks.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class Link implements Jsonizable {
    final public FreebaseProperty     property;
    final public Node                target;
    
    public Link(FreebaseProperty property, Node target) {
        this.property = property;
        this.target = target;
    }
    
    public FreebaseProperty getProperty() {
        return property;
    }
    
    public Node getTarget() {
        return target;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {

        writer.object();
        writer.key("property"); property.write(writer, options);
        writer.key("target"); target.write(writer, options);
        writer.endObject();
    }

}
