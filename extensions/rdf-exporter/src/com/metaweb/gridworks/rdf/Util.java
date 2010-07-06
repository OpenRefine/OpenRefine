package com.metaweb.gridworks.rdf;

import java.net.URI;
import java.net.URISyntaxException;

import com.metaweb.gridworks.model.Project;

public class Util {

    private static final String XSD_INT_URI ="http://www.w3.org/2001/XMLSchema#int";
    private static final String XSD_DOUBLE_URI ="http://www.w3.org/2001/XMLSchema#double";
    private static final String XSD_DATE_URI ="http://www.w3.org/2001/XMLSchema#date";
    
    public static String getUri(URI base,String rel)throws URISyntaxException{
        return base.resolve(rel).toString();
    }
    
    public static String getDataType(String s){
        if(s==null){
            return null;
        }
        if(s.equals(XSD_INT_URI)){
            return XSD_INT_URI;
        }
        if(s.equals(XSD_DOUBLE_URI)){
            return XSD_DOUBLE_URI;
        }
        if(s.equals(XSD_DATE_URI)){
            return XSD_DATE_URI;
        }
        return null;
    }
    
    public static RdfSchema getProjectSchema(Project project) {
        synchronized (project) {
            RdfSchema rdfSchema = (RdfSchema) project.overlayModels.get("rdfSchema");
            if (rdfSchema == null) {
                rdfSchema = new RdfSchema();
                
                project.overlayModels.put("rdfSchema", rdfSchema);
                project.getMetadata().updateModified();
            }
            
            return rdfSchema;
        }
    }
}
