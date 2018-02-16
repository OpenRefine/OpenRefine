package io.frictionlessdata.tableschema;

import io.frictionlessdata.tableschema.exceptions.ForeignKeyException;
import io.frictionlessdata.tableschema.exceptions.InvalidCastException;
import io.frictionlessdata.tableschema.exceptions.PrimaryKeyException;
import io.frictionlessdata.tableschema.exceptions.TypeInferringException;
import io.frictionlessdata.tableschema.fk.ForeignKey;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * 
 */
public class Schema {
    private static final int JSON_INDENT_FACTOR = 4;
    public static final String JSON_KEY_FIELDS = "fields";
    public static final String JSON_KEY_PRIMARY_KEY = "primaryKey";
    public static final String JSON_KEY_FOREIGN_KEYS = "foreignKeys";
    
    private org.everit.json.schema.Schema tableJsonSchema = null;
    private List<Field> fields = new ArrayList();
    private Object primaryKey = null;
    private List<ForeignKey> foreignKeys = new ArrayList();
    
    private boolean strictValidation = false;
    private List<Exception> errors = new ArrayList();
    
    public Schema(){
        this.initValidator();
    }
    
    public Schema(boolean strict){
        this.strictValidation = strict;
        this.initValidator();
    }
    
    /**
     * Read and create a table schema with JSON Object descriptor.
     * @param schema
     * @throws PrimaryKeyException 
     */
    public Schema(JSONObject schema) throws Exception{
        this(schema, false);   
    }
    
    /**
     * Read, create, and validate a table schema with JSON Object descriptor.
     * @param schema
     * @param strict
     * @throws ValidationException
     * @throws PrimaryKeyException
     * @throws ForeignKeyException 
     */
    public Schema(JSONObject schema, boolean strict) throws ValidationException, PrimaryKeyException, ForeignKeyException{
        this.strictValidation = strict;
        this.initValidator();
        this.initFromSchemaJson(schema);
 
        this.validate();         
    }
    
    /**
     * Read and create a table schema with remote descriptor.
     * @param schemaUrl
     * @throws Exception 
     */
    public Schema(URL schemaUrl) throws Exception{
        this(schemaUrl, false);
    }
    
    /**
     * Read, create, and validate a table schema with remote descriptor.
     * @param schemaUrl
     * @param strict
     * @throws ValidationException
     * @throws PrimaryKeyException
     * @throws ForeignKeyException
     * @throws Exception 
     */
    public Schema(URL schemaUrl, boolean strict) throws ValidationException, PrimaryKeyException, ForeignKeyException, Exception{
        this.strictValidation = strict;
        this.initValidator();
        InputStreamReader inputStreamReader = new InputStreamReader(schemaUrl.openStream(), "UTF-8");
        this.initSchemaFromStream(inputStreamReader);

        this.validate();
    }
    
    /**
     * Read and create a table schema with local descriptor.
     * @param schemaFilePath
     * @throws Exception 
     */
    public Schema(String schemaFilePath) throws Exception{
        this(schemaFilePath, false);
    }
    
    /**
     * Read, create, and validate a table schema with local descriptor.
     * @param schemaFilePath
     * @param strict
     * @throws ValidationException
     * @throws PrimaryKeyException
     * @throws ForeignKeyException
     * @throws Exception 
     */
    public Schema(String schemaFilePath, boolean strict) throws ValidationException, PrimaryKeyException, ForeignKeyException, Exception{
        this.strictValidation = strict;
        this.initValidator(); 
        InputStream is = new FileInputStream(schemaFilePath);
        InputStreamReader inputStreamReader = new InputStreamReader(is);
        this.initSchemaFromStream(inputStreamReader);
        
        this.validate();
    }
    
    /**
     * Reade and create a table schema using list of fields. 
     * @param fields 
     */
    public Schema(List<Field> fields){
        this(fields, false); 
    }
    
    /**
     * Read, create, and validate a table schema using list of fields. 
     * @param fields
     * @param strict
     * @throws ValidationException 
     */
    public Schema(List<Field> fields, boolean strict) throws ValidationException{
        this.strictValidation = strict;
        this.fields = fields;
        
        initValidator(); 
        validate();
    }
    
    /**
     * Infer the data types and return the generated schema.
     * @param data
     * @param headers
     * @return
     * @throws TypeInferringException 
     */
    public JSONObject infer(List<Object[]> data, String[] headers) throws TypeInferringException{
        return TypeInferrer.getInstance().infer(data, headers);
    }
    
    /**
     * Infer the data types and return the generated schema.
     * @param data
     * @param headers
     * @param rowLimit
     * @return
     * @throws TypeInferringException 
     */
    public JSONObject infer(List<Object[]> data, String[] headers, int rowLimit) throws TypeInferringException{
        return TypeInferrer.getInstance().infer(data, headers, rowLimit);
    }
    
    /**
     * Initializes the schema from given stream.
     * Used for Schema class instanciation with remote or local schema file.
     * @param schemaStreamReader
     * @throws Exception 
     */
    private void initSchemaFromStream(InputStreamReader schemaStreamReader) throws Exception{
        BufferedReader br = new BufferedReader(schemaStreamReader);
        String line = br.readLine();

        StringBuilder sb = new StringBuilder();
        while(line != null){
            sb.append(line);
            line = br.readLine();
        }

        String schemaString = sb.toString();
        JSONObject schemaJson = new JSONObject(schemaString);
        
        this.initFromSchemaJson(schemaJson);
    }
    
    private void initFromSchemaJson(JSONObject schema) throws PrimaryKeyException, ForeignKeyException{
        // Set Fields
        if(schema.has(JSON_KEY_FIELDS)){
            Iterator iter = schema.getJSONArray(JSON_KEY_FIELDS).iterator();
            while(iter.hasNext()){
                JSONObject fieldJsonObj = (JSONObject)iter.next();
                Field field = new Field(fieldJsonObj);
                this.fields.add(field);
            }  
        }
        
        // Set Primary Key
        if(schema.has(JSON_KEY_PRIMARY_KEY)){
            
            // If primary key is a composite key.
            if(schema.get(JSON_KEY_PRIMARY_KEY) instanceof JSONArray){
                
                JSONArray keyJSONArray = schema.getJSONArray(JSON_KEY_PRIMARY_KEY);
                String[] composityKey = new String[keyJSONArray.length()];
                for(int i=0; i<keyJSONArray.length(); i++){
                    composityKey[i] = keyJSONArray.getString(i);
                }
                
                this.setPrimaryKey(composityKey);
                
            }else{
                // Else if primary key is a single String key.
                this.setPrimaryKey(schema.getString(JSON_KEY_PRIMARY_KEY));
            }
        }
        
        // Set Foreign Keys
        if(schema.has(JSON_KEY_FOREIGN_KEYS)){

            JSONArray fkJsonArray = schema.getJSONArray(JSON_KEY_FOREIGN_KEYS);
            for(int i=0; i<fkJsonArray.length(); i++){
                
                JSONObject fkJsonObj = fkJsonArray.getJSONObject(i);
                ForeignKey fk = new ForeignKey(fkJsonObj, this.strictValidation);
                this.addForeignKey(fk);
                
                if(!this.strictValidation){
                    this.getErrors().addAll(fk.getErrors());
                }     
            }
        }
    }
    
    private void initValidator(){
        // Init for validation
        InputStream tableSchemaInputStream = TypeInferrer.class.getResourceAsStream("/schemas/table-schema.json");
        JSONObject rawTableJsonSchema = new JSONObject(new JSONTokener(tableSchemaInputStream));
        this.tableJsonSchema = SchemaLoader.load(rawTableJsonSchema);
    }
    
    /**
     * Check if schema is valid or not.
     * @return 
     */
    public boolean isValid(){
        try{
            validate();
            return true;
            
        }catch(ValidationException ve){
            return false;
        }
    }
    
    /**
     * Validate the loaded Schema.
     * Validation is strict or unstrict depending on how the package was
     * instanciated with the strict flag.
     * @throws ValidationException 
     */
    public final void validate() throws ValidationException{
        try{
             this.tableJsonSchema.validate(this.getJson());
            
        }catch(ValidationException ve){
            if(this.strictValidation){
                throw ve;
            }else{
                this.getErrors().add(ve);
            }
        }
    }
    
    public List<Exception> getErrors(){
        return this.errors;
    }
    
    public JSONObject getJson(){
        //FIXME: Maybe we should use JSON serializer like Gson?
        JSONObject schemaJson = new JSONObject();
        
        // Fields
        if(this.fields != null && this.fields.size() > 0){
            schemaJson.put(JSON_KEY_FIELDS, new JSONArray());
            this.fields.forEach((field) -> {
                schemaJson.getJSONArray(JSON_KEY_FIELDS).put(field.getJson());
            });
        }
        
        // Primary Key
        if(this.primaryKey != null){
            schemaJson.put(JSON_KEY_PRIMARY_KEY, this.primaryKey);
        }
        
        //Foreign Keys
        if(this.foreignKeys != null && this.foreignKeys.size() > 0){
            schemaJson.put(JSON_KEY_FOREIGN_KEYS, new JSONArray());

            this.foreignKeys.forEach((fk) -> {
                schemaJson.getJSONArray(JSON_KEY_FOREIGN_KEYS).put(fk.getJson());
            });            
        }
        
        return schemaJson;
    }
    
    public Object[] castRow(String[] row) throws InvalidCastException{
        
        if(row.length != this.fields.size()){
            throw new InvalidCastException("Row length is not equal to the number of defined fields.");
        }
        
        try{
            Object[] castRow = new Object[this.fields.size()];
        
            for(int i=0; i<row.length; i++){
                Field field = this.fields.get(i);

                String castMethodName = "cast" + (field.getType().substring(0, 1).toUpperCase() + field.getType().substring(1));;
                Method method = TypeInferrer.class.getMethod(castMethodName, String.class, String.class);

                castRow[i] = method.invoke(TypeInferrer.getInstance(), field.getFormat(), row[i]);
            }

            return castRow;
            
        }catch(Exception e){
            throw new InvalidCastException();
        }
        
    }
    
    public void save(String outputFilePath) throws IOException{
        try (FileWriter file = new FileWriter(outputFilePath)) {
            file.write(this.getJson().toString(JSON_INDENT_FACTOR));
        }
    }
    
    public void addField(Field field){
        this.fields.add(field);
        this.validate();
    }
    
    public void addField(JSONObject fieldJson){
        Field field = new Field(fieldJson);
        this.addField(field);
    }
    
    public List<Field> getFields(){
        return this.fields;
    }
    
    public Field getField(String name){
        Iterator<Field> iter = this.fields.iterator();
        while(iter.hasNext()){
            Field field = iter.next();
            if(field.getName().equalsIgnoreCase(name)){
                return field;
            }
        }
        return null;
    }
    
    public List<String> getFieldNames(){
        // Would be more elegant with Java 8 .map and .collect but it is certainly
        // best to keep logic backward compatible to Java 7.
        List<String> fieldNames = new ArrayList();
        Iterator<Field> iter = this.fields.iterator();
        while(iter.hasNext()){
            Field field = iter.next();
            fieldNames.add(field.getName());
        }
        
        return fieldNames;
    }
    
    public boolean hasField(String name){
        Iterator<Field> iter = this.fields.iterator();
        while(iter.hasNext()){
            Field field = iter.next();
            if(field.getName().equalsIgnoreCase(name)){
                return true;
            }
        }
        
        return false;
    }
    
    public boolean hasFields(){
        return !this.getFields().isEmpty();
    }
    

    
    /**
     * Set single primary key with the option of validation.
     * @param key
     * @throws PrimaryKeyException 
     */
    public void setPrimaryKey(String key) throws PrimaryKeyException{        
        if(!this.hasField(key)){
            PrimaryKeyException pke = new PrimaryKeyException("No such field as: " + key + ".");
            if(this.strictValidation){
                throw pke;
            }else{
                this.getErrors().add(pke);
            }
        }
        
        this.primaryKey = key; 
    }
    
    /**
     * Set composite primary key with the option of validation.
     * @param compositeKey
     * @throws PrimaryKeyException 
     */
    public void setPrimaryKey(String[] compositeKey) throws PrimaryKeyException{        
        for (String aKey : compositeKey) {
            if (!this.hasField(aKey)) {
                PrimaryKeyException pke = new PrimaryKeyException("No such field as: " + aKey + ".");
                
                if(this.strictValidation){
                    throw pke;
                }else{
                    this.getErrors().add(pke);
                }
            }
        }
        
        this.primaryKey = compositeKey;
    }
    
    public <Any> Any getPrimaryKey(){
        return (Any)this.primaryKey;
    }
    
    public List<ForeignKey> getForeignKeys(){
        return this.foreignKeys;
    }
    
    public void addForeignKey(ForeignKey foreignKey){
        this.foreignKeys.add(foreignKey);
    }
   
}