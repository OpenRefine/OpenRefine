package com.google.refine.rdf;

import java.io.IOException;
import java.net.URI;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;

import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class ConstantResourceNode extends ResourceNode{

    private String uri;


    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    

    public ConstantResourceNode(String uri){
        this.uri = uri;
    }

    @Override
    public Resource createResource(URI baseUri, ValueFactory factory, Project project,
            Row row, int rowIndex,BNode[] blanks) {
        if(this.uri!=null & this.uri.length()>0){
            Resource r =  factory.createURI(Util.resolveUri(baseUri, this.uri));
            return r;
        }else{
            return null;
        }
    }

	@Override
	protected void writeNode(JsonGenerator jwriter)
			throws JsonGenerationException, IOException {
		jwriter.writeStringField("nodeType","resource");
        jwriter.writeStringField("value",uri);		
	}


    
}
