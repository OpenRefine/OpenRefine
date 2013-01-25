package org.deri.grefine.rdf;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;
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

    protected abstract void writeNode(JSONWriter writer) throws JSONException;
    @Override
    public void write(JSONWriter writer, Properties options)throws JSONException{
        writer.object();
        //writer node
        writeNode(writer);
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
            l.write(writer,options);
        }
        writer.endArray();
        
        writer.endObject();
        
    }

    
    protected void addTypes(Resource[] rs,ValueFactory factory, RepositoryConnection con, URI baseUri) throws RepositoryException {
    	for(Resource r:rs){
    		for(RdfType type:this.getTypes()){
    			Statement stmt = factory.createStatement(r, RDF.TYPE, factory.createURI(Util.resolveUri(baseUri, type.uri)));
    			con.add(stmt);
    		}
    	}
    }
    
    protected Resource[] addLinks(Resource[] rs,URI baseUri,ValueFactory factory,RepositoryConnection con, Project project,Row row,int rowIndex,BNode[] blanks) throws RepositoryException{
   		for(int i=0;i<getLinkCount();i++){
           	Link l = getLink(i);
           	org.openrdf.model.URI p = factory.createURI(Util.resolveUri(baseUri, l.propertyUri));
           	Value[] os = l.target.createNode(baseUri, factory, con, project, row, rowIndex,blanks);
           	if(os!=null){
           		for(Value o:os){
           			for(Resource r:rs){
           				con.add(factory.createStatement(r, p, o));
           			}
           		}
           	}
       	}
        return rs;
    }

    public void setTypes(List<RdfType> types) {
        this.rdfTypes = types;
    }
    
    public Value[] createNode(URI baseUri,ValueFactory factory,RepositoryConnection con, Project project,Row row,int rowIndex,BNode[] blanks) {
        Resource[] r = createResource(baseUri, factory, project, row, rowIndex,blanks);
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
    
    public abstract Resource[] createResource(URI baseUri,ValueFactory factory, Project project,Row row,int rowIndex,BNode[] blanks) ;
    
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
