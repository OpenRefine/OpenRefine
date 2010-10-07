package com.google.refine.rdf;

import java.io.IOException;
import java.net.URI;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
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
    
    public void write(JsonGenerator writer)
            throws  JsonGenerationException, IOException {
        writer.writeStartObject();
        writer.writeStringField("nodeType","cell-as-literal");
        writer.writeStringField("expression",expression);
        writer.writeBooleanField("isRowNumberCell",isRowNumberCell);
        if(valueType!=null){
        	writer.writeStringField("valueType",valueType);
        }
        if(lang!=null){
            writer.writeStringField("lang",lang);
        }
        if(columnName!=null){
        	writer.writeStringField("columnName",columnName);
        }
        writer.writeEndObject();
    }
    
    public CellLiteralNode(String columnName, String exp, String valueType,String lang,boolean isRowNumberCell){
    	this.columnName = columnName;
        this.lang = lang;
        this.valueType = valueType;
        this.isRowNumberCell = isRowNumberCell;
        this.expression = exp;
    }
    @Override
    public Value createNode(URI baseUri, ValueFactory factory, RepositoryConnection con, Project project,
            Row row, int rowIndex,BNode[] blanks) {
        String val = null;

        	
        try{
            Object result = Util.evaluateExpression(project, expression, columnName, row, rowIndex);
            
            if(result.getClass()==EvalError.class){
            	return null;
            }
            if(result.toString().length()>0){
            	val = result.toString();
            }
    	}catch(Exception e){
    		//an empty cell might result in an exception out of evaluating URI expression... so it is intended to eat the exception
    		val = null;
    	}   
            
        if(val!=null && val.length()>0){
            Literal l;
            if(this.valueType!=null){
            	//TODO handle exception when valueType is not a valid URI
                l = factory.createLiteral(val, factory.createURI(valueType));
            }else{
            	if(this.lang!=null){
            		l = factory.createLiteral(val,lang);
            	}else{
            		l = factory.createLiteral(val);
            	}
            }
            return l;
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

}
