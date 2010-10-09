package com.google.refine.rdf;

import java.net.URI;

import org.json.JSONException;
import org.json.JSONWriter;
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
    public void writeNode(JSONWriter writer) throws JSONException {
        writer.key("nodeType"); writer.value("cell-as-blank");
        writer.key("isRowNumberCell"); writer.value(isRowNumberCell);
        if(columnName!=null){
        	writer.key("columnName");writer.value(columnName);
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
