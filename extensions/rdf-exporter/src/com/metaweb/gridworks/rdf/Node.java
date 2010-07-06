package com.metaweb.gridworks.rdf;

import java.net.URI;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public interface Node extends Jsonizable{

	RDFNode createNode(URI baseUri,Model model,Project project,Row row,int rowIndex,Resource[] blanks);
}
