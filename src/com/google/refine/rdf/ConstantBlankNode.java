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

public class ConstantBlankNode extends ResourceNode{

    private int _id;
    ConstantBlankNode(int id){
        this._id = id;
    }
    
    @Override
    public Resource createResource(URI baseUri, ValueFactory factory, Project project,
            Row row, int rowIndex,BNode[] blanks) {
        return blanks[this._id];
    }

	@Override
	protected void writeNode(JsonGenerator jwriter)
			throws JsonGenerationException, IOException {
		jwriter.writeStringField("nodeType","blank");
		
	}

    

}
