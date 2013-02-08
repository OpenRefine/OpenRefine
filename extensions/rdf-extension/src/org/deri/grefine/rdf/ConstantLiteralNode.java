package org.deri.grefine.rdf;

import java.net.URI;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;

import com.google.refine.model.Project;
import com.google.refine.model.Row;

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
    
    @Override
	public void write(JSONWriter writer, Properties options) throws JSONException {
    	writer.object();
        writer.key("nodeType"); writer.value("literal");
        writer.key("value"); writer.value(value);
        if(valueType!=null){
        	writer.key("valueType"); writer.value(valueType);
        }
        if(lang!=null){
        	writer.key("lang"); writer.value(lang);
        }
        writer.endObject();
	}
    
    @Override
	public Value[] createNode(URI baseUri, ValueFactory factory, RepositoryConnection con, Project project,
            Row row, int rowIndex,BNode[] blanks) {
        if(this.value!=null && this.value.length()>0){
            
            Literal l ;
            if(this.valueType!=null){
            	//TODO handle exception when valueType is not a valid URI
                l = factory.createLiteral(this.value, factory.createURI(valueType));
            }else{
            	if(this.lang!=null){
            		l = factory.createLiteral(this.value, lang);
            	}else{
            		l = factory.createLiteral(this.value);
            	}
            }
            return new Literal[]{l};
        }else{
            return null;
        }
    }

}
