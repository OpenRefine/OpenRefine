package com.metaweb.gridworks.rdf.vocab;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class Vocabulary implements Jsonizable{
    private final String name;
    private final String uri;
    private List<RDFSClass> classes = new ArrayList<RDFSClass>();
    private List<RDFSProperty> properties = new ArrayList<RDFSProperty>();
    
    public Vocabulary(String name,String uri){
        this.name = name;
        this.uri = uri;
    }
    
    public void addClass(RDFSClass clazz){
        this.classes.add(clazz);
    }
    
    public void addProperty(RDFSProperty prop){
        this.properties.add(prop);
    }
    
    public List<RDFSClass> getClasses() {
        return classes;
    }

    public void setClasses(List<RDFSClass> classes) {
        this.classes = classes;
    }

    public List<RDFSProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<RDFSProperty> properties) {
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        
        writer.key("name"); writer.value(name);
        writer.key("uri"); writer.value(uri);
        
        writer.endObject();
    }
}
