package org.deri.grefine.rdf;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;

import com.google.refine.expr.EvalError;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class CellLiteralNode implements CellNode{

	final private String valueType;
    final private String lang;
    final private String columnName;
    final boolean isRowNumberCell;
    final private String expression;
    
    public String getValueType() {
        return valueType;
    }
    
    public String getLang() {
        return lang;
    }
    
    public CellLiteralNode(String columnName, String exp, String valueType,String lang,boolean isRowNumberCell){
    	this.columnName = columnName;
        this.lang = lang;
        this.valueType = valueType;
        this.isRowNumberCell = isRowNumberCell;
        this.expression = exp;
    }
    @Override
    public Value[] createNode(URI baseUri, ValueFactory factory, RepositoryConnection con, Project project,
            Row row, int rowIndex,BNode[] blanks) {
        String[] val = null;

        	
        try{
            Object result = Util.evaluateExpression(project, expression, columnName, row, rowIndex);
            
            if(result.getClass()==EvalError.class){
            	return null;
            }
            if(result.getClass().isArray()){
            	int lngth = Array.getLength(result);
            	val = new String[lngth];
            	for(int i=0;i<lngth;i++){
            		val[i] = Array.get(result,i).toString();
            	}
            }else if(result.toString().length()>0){
            	val = new String[1];
            	val[0] = result.toString();
            }
    	}catch(Exception e){
    		//an empty cell might result in an exception out of evaluating URI expression... so it is intended to eat the exception
    		val = null;
    	}   
        
        if(val!=null && val.length>0){
        	Literal[] ls = new Literal[val.length];
        	for(int i=0;i<val.length;i++){
        		Literal l;
            	if(this.valueType!=null){
                	l = factory.createLiteral(val[i], factory.createURI(valueType));
            	}else{
            		if(this.lang!=null){
            			l = factory.createLiteral(val[i],lang);
            		}else{
            			l = factory.createLiteral(val[i]);
            		}
            	}
            	ls[i] = l;
        	}
            return ls;
        }else{
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
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();
        writer.key("nodeType"); writer.value("cell-as-literal");
        writer.key("expression"); writer.value(expression);
        writer.key("isRowNumberCell"); writer.value(isRowNumberCell);
        if(valueType!=null){
        	writer.key("valueType"); writer.value(valueType);
        }
        if(lang!=null){
            writer.key("lang"); writer.value(lang);
        }
        if(columnName!=null){
        	writer.key("columnName"); writer.value(columnName);
        }
        writer.endObject();		
	}

}
