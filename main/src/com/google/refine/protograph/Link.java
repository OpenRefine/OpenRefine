package com.google.refine.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;

public class Link implements Jsonizable {
    final public FreebaseProperty    property;
    final public Node                target;
    final public Condition           condition;
    final public boolean             load;
    
    public Link(FreebaseProperty property, Node target, Condition condition, boolean load) {
        this.property = property;
        this.target = target;
        this.condition = condition;
        this.load = load;
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
        if (target != null) {
            writer.key("target");
            target.write(writer, options);
        }
        if (condition != null) {
            writer.key("condition");
            condition.write(writer, options);
        }
        writer.endObject();
    }

}
