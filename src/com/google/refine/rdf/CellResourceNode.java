package com.google.refine.rdf;

import java.io.IOException;
import java.net.URI;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;

import com.google.refine.expr.EvalError;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class CellResourceNode extends ResourceNode implements CellNode{

    final private String uriExpression;
    final private String columnName;
    final private boolean isRowNumberCell; 
    
    public String getUriExpression() {
        return uriExpression;
    }

    
    
    public CellResourceNode(String columnName,String exp,boolean isRowNumberCell) {
    	this.columnName = columnName;
        this.uriExpression = exp;
        this.isRowNumberCell = isRowNumberCell;
    }

    @Override
    public Resource createResource(URI baseUri,ValueFactory factory,Project project,Row row,int rowIndex,BNode[] blanks) {
        try{
        	Object result = Util.evaluateExpression(project, uriExpression, columnName, row, rowIndex);
            if(result.getClass()==EvalError.class){
            	return null;
            }
            if(result.toString().length()>0){
            	String uri = Util.resolveUri(baseUri, result.toString());
                return factory.createURI(uri.toString());
            }else{
                return null;
            }
        }catch(Exception e){
            //an empty cell might result in an exception out of evaluating URI expression... so it is intended to eat the exception
            return null;
            
            
        }
        
    }

    


	@Override
	protected void writeNode(JsonGenerator jwriter)
			throws JsonGenerationException, IOException {
        jwriter.writeStringField("nodeType","cell-as-resource");
        jwriter.writeStringField("expression",uriExpression);
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
