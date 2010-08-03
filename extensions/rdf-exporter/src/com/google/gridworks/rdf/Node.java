package com.google.gridworks.rdf;

import java.net.URI;

import com.google.gridworks.Jsonizable;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.Row;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public interface Node extends Jsonizable{

    RDFNode createNode(URI baseUri,Model model,Project project,Row row,int rowIndex,Resource[] blanks);
}
