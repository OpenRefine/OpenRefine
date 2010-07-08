package com.metaweb.gridworks.rdf;

import java.net.URI;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class CellLiteralNode extends CellNode{

    private String valueType;
    private String lang;
    public String getValueType() {
        return valueType;
    }
    public void setValueType(String valueType) {
        this.valueType = valueType;
    }
    public String getLang() {
        return lang;
    }
    public void setLang(String lang) {
        this.lang = lang;
    }
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("nodeType");writer.value("cell-as-literal");
        writer.key("columnIndex");writer.value(columnIndex);
        writer.key("columnName");writer.value(columnName);
        if(valueType!=null){
            writer.key("valueType");writer.value(valueType);
        }
        if(lang!=null){
            writer.key("lang");writer.value(lang);
        }
        writer.endObject();
    }
    
    public CellLiteralNode(int index,String columnName){
        super(index,columnName);
    }
    
    public CellLiteralNode(int index,String columnName,String valueType,String lang){
        this(index,columnName);
        this.lang = lang;
        this.valueType = valueType;
    }
    public RDFNode createNode(URI baseUri, Model model, Project project,
            Row row, int rowIndex,Resource[] blanks) {
        String val;
        try{
            val= row.getCell(this.columnIndex).value.toString();
        }catch(NullPointerException ne){
            return null;
        }
        if(val!=null && val.length()>0){
            //TODO language and datatype
            Literal l;
            if(this.valueType!=null){
                l = model.createTypedLiteral(val, valueType);
            }else{
                l = model.createLiteral(val);
            }
            return l;
        }else{
            return null;
        }
    }

}
