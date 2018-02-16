package io.frictionlessdata.tableschema.fk;

import io.frictionlessdata.tableschema.exceptions.ForeignKeyException;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * 
 */
public class Reference {
    private static final String JSON_KEY_DATAPACKAGE = "datapackage";
    private static final String JSON_KEY_RESOURCE = "resource";
    private static final String JSON_KEY_FIELDS = "fields";
    
    private URL datapackage = null;
    private String resource = null;
    private Object fields = null;
    
    public Reference(){
    }
    
    public Reference(String resource, Object fields) throws ForeignKeyException{
        this(resource, fields, false);
    }
    
    public Reference(String resource, Object fields, boolean strict) throws ForeignKeyException{
        this.resource = resource;
        this.fields = fields;
        
        if(strict){
            this.validate();
        }
    }
    
    public Reference(URL datapackage, String resource, Object fields) throws ForeignKeyException{
        this(datapackage, resource, fields, false);
    }
    
    public Reference(URL datapackage, String resource, Object fields, boolean strict) throws ForeignKeyException{
        this.resource = resource;
        this.fields = fields;
        this.datapackage = datapackage;
        
        if(strict){
            this.validate();
        }
    }
    
    public Reference(JSONObject refJsonObject, boolean strict) throws ForeignKeyException{
        if(refJsonObject.has(JSON_KEY_DATAPACKAGE)){
            try{
                this.datapackage = new URL(refJsonObject.getString(JSON_KEY_DATAPACKAGE));
                
            }catch(MalformedURLException mue){
                // leave datapackage set to null;
                this.datapackage = null;
            }  
        }
        
        if(refJsonObject.has(JSON_KEY_RESOURCE)){
            this.resource = refJsonObject.getString(JSON_KEY_RESOURCE);
        }
        
        if(refJsonObject.has(JSON_KEY_FIELDS)){
            this.fields = refJsonObject.get(JSON_KEY_FIELDS);   
        }
        
        if(strict){
            this.validate();
        }
    }
    
    public URL getDatapackage(){
        return this.datapackage;
    }
    
    public void setDatapackage(URL datapackage){
        this.datapackage = datapackage;
    }
    
    public String getResource(){
        return this.resource;
    }
    
    public void setResource(String resource){
        this.resource = resource;
    }
    
    public <Any> Any getFields(){
        return (Any)this.fields;
    }
    
    public void setFields(Object fields){
        this.fields = fields;
    }
    
    public final void validate() throws ForeignKeyException{
        if(this.resource == null || this.fields == null){
            throw new ForeignKeyException("A foreign key's reference must have the fields and resource properties.");
            
        }else if(!(this.fields instanceof String) && !(this.fields instanceof JSONArray)){
            throw new ForeignKeyException("The foreign key's reference fields property must be a string or an array.");
        }
    }
    
    public JSONObject getJson(){
        //FIXME: Maybe we should use JSON serializer like Gson?

        JSONObject json = new JSONObject();
        if(this.datapackage != null){
            json.put(JSON_KEY_DATAPACKAGE, this.datapackage.toString());
        }
        
        if(this.resource != null){
            json.put(JSON_KEY_RESOURCE, this.resource);
        }
        
        if(this.fields != null){
            json.put(JSON_KEY_FIELDS, this.fields);
        }

        return json;
    }
}
