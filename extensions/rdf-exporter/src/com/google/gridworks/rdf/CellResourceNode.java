package com.google.gridworks.rdf;

import java.net.URI;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;


import com.google.gridworks.expr.Evaluable;
import com.google.gridworks.expr.ExpressionUtils;
import com.google.gridworks.expr.MetaParser;
import com.google.gridworks.model.Cell;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.Row;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class CellResourceNode extends ResourceNode{

    private String uriExpression;
    final public String columnName;
    public String getUriExpression() {
        return uriExpression;
    }

    
    final public int columnIndex;
    
    public CellResourceNode(int i,String columnName){
        this.columnIndex = i;
        this.columnName = columnName;
    }
    
    public CellResourceNode(int columnIndex,String columnName,String exp) {
        this(columnIndex,columnName);
        this.uriExpression = exp;
    }

    @Override
    public Resource createResource(URI baseUri,Model model,Project project,Row row,int rowIndex,Resource[] blanks) {
        try{
            Properties bindings = ExpressionUtils.createBindings(project);
            Evaluable eval = MetaParser.parse(uriExpression);
            Cell cell = row.getCell(this.columnIndex);
            String colName = this.columnIndex>-1?project.columnModel.getColumnByCellIndex(this.columnIndex).getName():"";
            ExpressionUtils.bind(bindings, row, rowIndex,colName , cell);
            Object result = eval.evaluate(bindings);
            if(result.toString().length()>0){
                String uri = Util.getUri(baseUri, result.toString());
                Resource r =  model.createResource(uri);
                return r;
            }else{
                return null;
            }
        }catch(Exception e){
//            e.printStackTrace();
            //an empty cell might result in an exception out of evaluating URI expression... so it is intended to eat the exception
            return null;
        }
        
    }

    @Override
    protected void writeNode(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("nodeType"); writer.value("cell-as-resource");
        writer.key("uriExpression"); writer.value(uriExpression);
        writer.key("columnIndex"); writer.value(columnIndex);
        if(columnIndex==-1){
            //Row number
            writer.key("isRowNumberCell"); writer.value(true);
        } else {
            writer.key("columnName"); writer.value(columnName);
        }
    }
    
    
}
