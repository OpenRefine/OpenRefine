package org.deri.grefine.rdf;

import java.lang.reflect.Array;
import java.net.URI;

import org.json.JSONException;
import org.json.JSONWriter;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;

import com.google.refine.expr.EvalError;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class CellBlankNode extends ResourceNode implements CellNode{

    final private String columnName;
    final boolean isRowNumberCell;
    final private String expression;
    
    public CellBlankNode(String columnName,String exp,boolean isRowNumberCell){
        this.columnName = columnName;
        this.isRowNumberCell = isRowNumberCell;
        this.expression = exp;
    }
    
    @Override
    public Resource[] createResource(URI baseUri, ValueFactory factory, Project project,
            Row row, int rowIndex,BNode[] blanks) {
    	try{
    		Object result = Util.evaluateExpression(project, expression, columnName, row, rowIndex);
    		if(result.getClass()==EvalError.class){
    			return null;
    		}
    		if(result.getClass().isArray()){
    			int lngth = Array.getLength(result);
    			Resource[] bs = new BNode[lngth];
    			for(int i=0;i<lngth;i++){
    				bs[i] = factory.createBNode();
    			}
    			return bs;
    		}
    		return new Resource[]{factory.createBNode()};
    	}catch(Exception e){
    		return null;
    	}
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
