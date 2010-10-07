package com.google.refine.rdf;

import java.io.IOException;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

public class Link {

    public final String propertyUri;
    public final String curie;
    public final Node target;

    public Link(String uri,String curie,Node t){
        this.propertyUri = uri;
        this.target = t;
        this.curie = curie;
    }
    
    public void write(JsonGenerator jwriter)
    	throws  JsonGenerationException, IOException {

        jwriter.writeStartObject();
        jwriter.writeStringField("uri",propertyUri);
        jwriter.writeStringField("curie",curie);
        if (target != null) {
            jwriter.writeFieldName("target");
            target.write(jwriter);
        }
        jwriter.writeEndObject();
    }
}
