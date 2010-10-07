package com.google.refine.rdf;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import com.google.refine.model.Project;
import com.google.refine.model.Row;


abstract public class ResourceNode implements Node {

    private List<Link> links = new ArrayList<Link>();
    
    public List<Link> getLinks() {
		return links;
	}

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

    protected abstract void writeNode(JsonGenerator jwriter) throws  JsonGenerationException, IOException;
    public void write(JsonGenerator jwriter)
    	throws  JsonGenerationException, IOException {
        jwriter.writeStartObject();
        //writer node
        writeNode(jwriter);
        //write types
        jwriter.writeFieldName("rdfTypes");
        jwriter.writeStartArray();
        for(RdfType type:this.getTypes()){
            jwriter.writeStartObject();
            jwriter.writeStringField("uri",type.uri);
            jwriter.writeStringField("curie",type.curie);
            jwriter.writeEndObject();
        }
        jwriter.writeEndArray();
        //write links
        jwriter.writeFieldName("links");
        jwriter.writeStartArray();
        for(int i=0;i<getLinkCount();i++){
            Link l = getLink(i);
            l.write(jwriter);
        }
        jwriter.writeEndArray();
        
        jwriter.writeEndObject();
        
    }

    
    protected void addTypes(Resource r,ValueFactory factory, RepositoryConnection con, URI baseUri) throws RepositoryException {
        for(RdfType type:this.getTypes()){
        	Statement stmt = factory.createStatement(r, RDF.TYPE, factory.createURI(Util.resolveUri(baseUri, type.uri)));
        	con.add(stmt);
        }
    }
    
    protected Resource addLinks(Resource r,URI baseUri,ValueFactory factory,RepositoryConnection con, Project project,Row row,int rowIndex,BNode[] blanks) throws RepositoryException{
        for(int i=0;i<getLinkCount();i++){
            Link l = getLink(i);
            org.openrdf.model.URI p = factory.createURI(Util.resolveUri(baseUri, l.propertyUri));
            Value o = l.target.createNode(baseUri, factory, con, project, row, rowIndex,blanks);
            if(o!=null){
                con.add(factory.createStatement(r, p, o));
            }
        }
        return r;
    }

    public void setTypes(List<RdfType> types) {
        this.rdfTypes = types;
    }
    
    public Value createNode(URI baseUri,ValueFactory factory,RepositoryConnection con, Project project,Row row,int rowIndex,BNode[] blanks) {
        Resource r = createResource(baseUri, factory, project, row, rowIndex,blanks);
        if(r==null){
            return null;
        }
        try{
        	addTypes(r, factory,con, baseUri);
        	return addLinks(r,baseUri,factory,con, project,row,rowIndex,blanks);
        }catch(RepositoryException e){
        	throw new RuntimeException(e);
        }
    }
    
    public abstract Resource createResource(URI baseUri,ValueFactory factory, Project project,Row row,int rowIndex,BNode[] blanks) ;
    
    public static class RdfType{
        String uri;
        public String getUri() {
			return uri;
		}
		String curie;
        public RdfType(String uri,String curie){
            this.uri = uri;
            this.curie = curie;
            
        }
    }
}
