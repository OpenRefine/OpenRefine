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

public class CellBlankNode extends ResourceNode implements CellNode{

    final private String columnName;
    final boolean isRowNumberCell;
    
    public CellBlankNode(String columnName,boolean isRowNumberCell){
        this.columnName = columnName;
        this.isRowNumberCell = isRowNumberCell;
    }
    
    @Override
    public Resource createResource(URI baseUri, ValueFactory factory, Project project,
            Row row, int rowIndex,BNode[] blanks) {
        return factory.createBNode();
    }

    @Override
    public void writeNode(JsonGenerator jwriter) throws  JsonGenerationException, IOException{
        jwriter.writeStringField("nodeType","cell-as-blank");
        jwriter.writeBooleanField("isRowNumberCell",isRowNumberCell);
        if(columnName!=null){
        	jwriter.writeStringField("columnName",columnName);
        }
    }

	@Override
	public boolean isRowNumberCellNode() {
		return isRowNumberCell;
	}

	@Override
	public String getColumnName() {
		return columnName;
	}

}
