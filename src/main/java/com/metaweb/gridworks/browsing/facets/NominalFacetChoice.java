package com.metaweb.gridworks.browsing.facets;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.browsing.DecoratedValue;

public class NominalFacetChoice implements Jsonizable {
    final public DecoratedValue    decoratedValue;
    public int                    count;
    public boolean                selected;
    
    public NominalFacetChoice(DecoratedValue decoratedValue) {
        this.decoratedValue = decoratedValue;
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("v"); decoratedValue.write(writer, options);
        writer.key("c"); writer.value(count);
        writer.key("s"); writer.value(selected);
        writer.endObject();
    }
}
