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
    public Resource[] createResource(URI baseUri,ValueFactory factory,Project project,Row row,int rowIndex,BNode[] blanks) {
        try{
        	Object result = Util.evaluateExpression(project, uriExpression, columnName, row, rowIndex);
            if(result.getClass()==EvalError.class){
            	return null;
            }
            if(result.getClass().isArray()){
            	int lngth = Array.getLength(result);
            	Resource[] rs = new org.openrdf.model.URI[lngth];
            	for(int i=0;i<lngth;i++){
            		String uri = Util.resolveUri(baseUri,  Array.get(result, i).toString());
            		rs[i] = factory.createURI(uri);
            	}
            	return rs;
            }
            if(result.toString().length()>0){
            	String uri = Util.resolveUri(baseUri, result.toString());
                return new Resource[] {factory.createURI(uri)};
            }else{
                return null;
            }
        }catch(Exception e){
            //an empty cell might result in an exception out of evaluating URI expression... so it is intended to eat the exception
            return null;
            
            
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



	@Override
	protected void writeNode(JSONWriter writer) throws JSONException {
		writer.key("nodeType"); writer.value("cell-as-resource");
        writer.key("expression"); writer.value(uriExpression);
        writer.key("isRowNumberCell"); writer.value(isRowNumberCell);
        if(columnName!=null){
        	writer.key("columnName"); writer.value(columnName);
        }
		
	}
}
