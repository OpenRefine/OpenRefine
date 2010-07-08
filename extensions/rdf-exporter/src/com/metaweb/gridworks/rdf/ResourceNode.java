package com.metaweb.gridworks.rdf;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;


abstract public class ResourceNode implements Node {

    private List<Link> links = new ArrayList<Link>();
    
    private List<RdfType> rdfTypes = new ArrayList<RdfType>();
    
    public void addLink(Link link) {
        this.links.add(link);
    }

    public void addType(RdfType type) {
        this.rdfTypes.add(type);
    }

    public Link getLink(int index) {
        return this.links.get(index);
    }

    public int getLinkCount() {
        return this.links.size();
    }

    public List<RdfType> getTypes() {
        return this.rdfTypes;
    }

    protected abstract void writeNode(JSONWriter writer, Properties options) throws JSONException;
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        //writer node
        writeNode(writer,options);
        //write types
        writer.key("rdfTypes");
        writer.array();
        for(RdfType type:this.getTypes()){
            writer.object();
            writer.key("uri");writer.value(type.uri);
            writer.key("curie");writer.value(type.curie);
            writer.endObject();
        }
        writer.endArray();
        //write links
        writer.key("links");
        writer.array();
        for(int i=0;i<getLinkCount();i++){
            Link l = getLink(i);
            /*writer.object();
            writer.key("uri");writer.value(l.propertyUri);
            writer.key("target");
            l.target.write(writer, options);
            writer.endObject();*/
            
            l.write(writer, options);
        }
        writer.endArray();
        
        writer.endObject();
        
    }

    
    protected void addTypes(Resource r,Model model){
        for(RdfType type:this.getTypes()){
            r.addProperty(RDF.type, model.createResource(type.uri));
        }
    }
    
    protected Resource addLinks(Resource r,URI baseUri,Model model,Project project,Row row,int rowIndex,Resource[] blanks){
        for(int i=0;i<getLinkCount();i++){
            Link l = getLink(i);
            String propertyUri;
            try {
                propertyUri = Util.getUri(baseUri, l.propertyUri);
                Property p = model.createProperty(propertyUri);
                RDFNode o = l.target.createNode(baseUri, model, project, row, rowIndex,blanks);
                if(o!=null){
                    r.addProperty(p, o);
                }
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
        return r;
    }

    public void setTypes(List<RdfType> types) {
        this.rdfTypes = types;
    }
    
    public RDFNode createNode(URI baseUri,Model model,Project project,Row row,int rowIndex,Resource[] blanks) {
        Resource r = createResource(baseUri, model, project, row, rowIndex,blanks);
        if(r==null){
            return null;
        }
        addTypes(r, model);
        return addLinks(r,baseUri,model,project,row,rowIndex,blanks);
    }
    
    public abstract Resource createResource(URI baseUri,Model model,Project project,Row row,int rowIndex,Resource[] blanks) ;
    
    public static class RdfType{
        String uri;
        String curie;
        public RdfType(String uri,String curie){
            this.uri = uri;
            this.curie = curie;
            
        }
    }
}
