package org.deri.grefine.rdf;

import java.net.URI;

import org.openrdf.model.BNode;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;

import com.google.refine.Jsonizable;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public interface Node extends Jsonizable{
    Value[] createNode(URI baseUri,ValueFactory factory,RepositoryConnection con,Project project,Row row,int rowIndex,BNode[] blanks);
}
