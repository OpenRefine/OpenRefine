package com.metaweb.gridworks.rdf;

import java.net.URI;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ConstantBlankNode extends ResourceNode{

    private int _id;
    ConstantBlankNode(int id){
        this._id = id;
    }
    
    @Override
    public Resource createResource(URI baseUri, Model model, Project project,
            Row row, int rowIndex,Resource[] blanks) {
        return blanks[this._id];
    }

    @Override
    protected void writeNode(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("nodeType");writer.value("blank");
    }

}
