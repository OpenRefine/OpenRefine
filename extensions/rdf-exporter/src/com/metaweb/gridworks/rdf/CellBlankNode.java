package com.metaweb.gridworks.rdf;

import java.net.URI;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class CellBlankNode extends ResourceNode{

    final public int columnIndex;
    final public String columnName;
    
    public CellBlankNode(int i,String columnName){
        this.columnIndex = i;
        this.columnName = columnName;
    }
    
    @Override
    public Resource createResource(URI baseUri, Model model, Project project,
            Row row, int rowIndex,Resource[] blanks) {
        return model.createResource();
    }

    @Override
    protected void writeNode(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("nodeType");writer.value("cell-as-blank");
        writer.key("columnIndex");writer.value(columnIndex);
        writer.key("columnName");writer.value(columnName);
    }

}
