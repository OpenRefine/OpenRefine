package com.google.refine.rdf;

import java.io.IOException;
import java.net.URI;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.openrdf.model.BNode;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;

import com.google.refine.model.Project;
import com.google.refine.model.Row;

public interface Node {

    Value createNode(URI baseUri,ValueFactory factory,RepositoryConnection con,Project project,Row row,int rowIndex,BNode[] blanks);

	void write(JsonGenerator jwriter) throws  JsonGenerationException, IOException;
}
