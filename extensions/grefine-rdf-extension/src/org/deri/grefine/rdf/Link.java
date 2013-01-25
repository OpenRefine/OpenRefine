package org.deri.grefine.rdf;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;

public class Link implements Jsonizable{

    public final String propertyUri;
    public final String curie;
    public final Node target;

    public Link(String uri,String curie,Node t){
        this.propertyUri = uri;
        this.target = t;
        this.curie = curie;
    }
    
    public void write(JSONWriter writer, Properties options)throws  JSONException{

        writer.object();
        writer.key("uri"); writer.value(propertyUri);
        writer.key("curie"); writer.value(curie);
        if (target != null) {
            writer.key("target");
            target.write(writer, options);
        }
        writer.endObject();
    }
}
