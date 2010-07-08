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

public class ConstantLiteralNode implements Node{

    private String valueType;
    private String lang;
    private String value;
    
    
    public ConstantLiteralNode(String val,String type,String l){
        this.value = val;
        this.valueType = type;
        this.lang = l;
    }
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


    public String getValue() {
        return value;
    }


    public void setValue(String value) {
        this.value = value;
    }


    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("nodeType"); writer.value("literal");
        writer.key("value"); writer.value(value);
        if(valueType!=null){
            writer.key("valueType");
            writer.value(valueType);
        }
        if(lang!=null){
            writer.key("lang");
            writer.value(lang);
        }
        writer.endObject();
    }
    public RDFNode createNode(URI baseUri, Model model, Project project,
            Row row, int rowIndex,Resource[] blanks) {
        if(this.value!=null && this.value.length()>0){
            
            Literal l ;
            if(this.valueType!=null){
                l = model.createTypedLiteral(this.value, valueType);
            }else{
                l = model.createLiteral(this.value);
            }
            return l;
        }else{
            return null;
        }
    }

}
