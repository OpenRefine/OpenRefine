package io.frictionlessdata.datapackage;

import io.frictionlessdata.datapackage.exceptions.DataPackageException;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.net.URL;
import org.json.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.everit.json.schema.ValidationException;

/**
 * Load, validate and create a datapackage object.
 */
public class Package {
    
    private static final int JSON_INDENT_FACTOR = 4;
    private static final String DATAPACKAGE_FILENAME = "datapackage.json";
    public static final String JSON_KEY_RESOURCES = "resources";
    public static final String JSON_KEY_NAME = "name";
    public static final String JSON_KEY_PROFILE = "profile";
    
    private String basePath = null;
    
    private JSONObject jsonObject = new JSONObject();
    private boolean strictValidation = false;
    private List<Resource> resources = new ArrayList();
    private List<Exception> errors = new ArrayList();
    private Validator validator = new Validator();
    
    public Package(){
    }

    /**
     * Load from native Java JSONObject.
     * @param jsonObjectSource
     * @param strict
     * @throws IOException
     * @throws DataPackageException
     * @throws ValidationException 
     */
    public Package(JSONObject jsonObjectSource, boolean strict) throws IOException, DataPackageException, ValidationException{ 
        this.setJson(jsonObjectSource);
        this.strictValidation = strict;
        
        this.validate();
    }
    
    /**
     * Load from native Java JSONObject.
     * @param jsonObjectSource
     * @throws IOException
     * @throws DataPackageException 
     */
    public Package(JSONObject jsonObjectSource) throws IOException, DataPackageException{
        this(jsonObjectSource, false);
    }
    
    /**
     * Load from String representation of JSON object or from a zip file path.
     * @param jsonStringSource
     * @param strict
     * @throws IOException
     * @throws DataPackageException
     * @throws ValidationException
     */
    public Package(String jsonStringSource, boolean strict) throws IOException, DataPackageException, ValidationException{
        this.strictValidation = strict;
        
        // If zip file is given.
        if(jsonStringSource.toLowerCase().endsWith(".zip")){
            // Read in memory the file inside the zip.
            ZipFile zipFile = new ZipFile(jsonStringSource);
            ZipEntry entry = zipFile.getEntry(DATAPACKAGE_FILENAME);
            
            // Throw exception if expected datapackage.json file not found.
            if(entry == null){
                throw new DataPackageException("The zip file does not contain the expected file: " + DATAPACKAGE_FILENAME);
            }
            
            // Read the datapackage.json file inside the zip
            try(InputStream is = zipFile.getInputStream(entry)){
                StringBuilder out = new StringBuilder();
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(is))){
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        out.append(line);
                    }
                }
                
                // Create and set the JSONObject for the datapackage.json that was read from inside the zip file.
                this.setJson(new JSONObject(out.toString()));  
                
                // Validate.
                this.validate();
            }
   
        }else{
            // Create and set the JSONObject fpr the String representation of desriptor JSON object.
            this.setJson(new JSONObject(jsonStringSource)); 
            
            // If String representation of desriptor JSON object is provided.
            this.validate(); 
        }
    }
    
    /**
     * Load from String representation of JSON object or from a zip file path.
     * @param jsonStringSource
     * @throws DataPackageException
     * @throws ValidationException
     * @throws IOException 
     */
    public Package(String jsonStringSource) throws DataPackageException, ValidationException, IOException{
        this(jsonStringSource, false);
    }
    
    /**
     * Load from URL (must be in either 'http' or 'https' schemes).
     * @param urlSource
     * @param strict
     * @throws DataPackageException
     * @throws ValidationException
     * @throws IOException
     * @throws FileNotFoundException 
     */
    public Package(URL urlSource, boolean strict) throws DataPackageException, ValidationException, IOException, FileNotFoundException{
        this.strictValidation = strict;

        // Get string content of given remove file.
        String jsonString = getJsonStringContentFromRemoteFile(urlSource);

        // Create JSONObject and validate.
        this.setJson(new JSONObject(jsonString));
        this.validate();  
        
    }

    /**
     * Load from URL (must be in either 'http' or 'https' schemes).
     * No validation by default.
     * @param urlSource
     * @throws DataPackageException
     * @throws IOException
     * @throws FileNotFoundException 
     */
    public Package(URL urlSource) throws DataPackageException, IOException, FileNotFoundException{
        this(urlSource, false);
    }
    
    /**
     * Load from local file system path.
     * @param filePath
     * @param basePath
     * @param strict
     * @throws DataPackageException
     * @throws ValidationException
     * @throws FileNotFoundException 
     */
    public Package(String filePath, String basePath, boolean strict) throws IOException, DataPackageException, ValidationException, FileNotFoundException {
        this.strictValidation = strict;
        File sourceFile = null;
        
        if(StringUtils.isEmpty(basePath)){
            // There is no basePath, i.e. it is empty ("") or null.
            // Hence the source is the absolute path of the file.
            // In this case we grab the directory of the source path and set it as the basePath.
            sourceFile = new File(filePath);
                  
        }else{
            // There is a basePath. Construct the absolute path and load it.
            String absoluteFilePath = basePath + "/" + filePath;
            sourceFile = new File(absoluteFilePath);  
        }
        
        if(sourceFile.exists()){
            // Set base path
            this.setBasePath(sourceFile.getParent());

            // Read file, it should be a JSON.
            String sourceJsonString = this.getJsonStringContentFromLocalFile(sourceFile.getAbsolutePath());
            JSONObject sourceJsonObject = new JSONObject(sourceJsonString);
            
            this.setJson(sourceJsonObject);
            this.validate();

        }else{
            throw new FileNotFoundException();
        }
    }
    
    /**
     * Load from local file system path.
     * No validation by default.
     * @param filePath
     * @param basePath
     * @throws IOException
     * @throws DataPackageException
     * @throws FileNotFoundException 
     */
    public Package(String filePath, String basePath) throws IOException, DataPackageException, FileNotFoundException {
        this(filePath, basePath, false); 
    }
    
    public void save(String outputFilePath) throws IOException, DataPackageException{
        if(outputFilePath.toLowerCase().endsWith(".json")){
            this.saveJson(outputFilePath);
            
        }else if(outputFilePath.toLowerCase().endsWith(".zip")){
            this.saveZip(outputFilePath);
            
        }else{
            throw new DataPackageException("Unrecognized file format.");
        }
    }
    
    private void saveJson(String outputFilePath) throws IOException, DataPackageException{
        try (FileWriter file = new FileWriter(outputFilePath)) {
            file.write(this.getJson().toString(JSON_INDENT_FACTOR));
        }
    }
    
    private void saveZip(String outputFilePath) throws IOException, DataPackageException{
        try(FileOutputStream fos = new FileOutputStream(outputFilePath)){
            try(BufferedOutputStream bos = new BufferedOutputStream(fos)){
                try(ZipOutputStream zos = new ZipOutputStream(bos)){
                    // File is not on the disk, test.txt indicates
                    // only the file name to be put into the zip.
                    ZipEntry entry = new ZipEntry("datapackage.json"); 

                    zos.putNextEntry(entry);
                    zos.write(this.getJson().toString(JSON_INDENT_FACTOR).getBytes());
                    zos.closeEntry();
                }           
            }
        }
    }
    
    
    public void infer(){
        this.infer(false);
    }
    
    public void infer(boolean pattern){
        throw new UnsupportedOperationException();
    }
    
    public Resource getResource(String resourceName){
        Iterator<Resource> iter = this.resources.iterator();
        while(iter.hasNext()) {
            Resource resource = iter.next();
            if(resource.getName().equalsIgnoreCase(resourceName)){
                return resource;
            }
        }
        return null;
    }
    
    public List<Resource> getResources(){
        return this.resources;
    }
    
    public void addResource(Resource resource) throws IOException, ValidationException, DataPackageException{
        
        // If a name property isn't given...
        if(resource.getName() == null){
            DataPackageException dpe = new DataPackageException("The resource does not have a name property.");

            if(this.strictValidation){
                throw dpe;
            }else{
                errors.add(dpe);
            }
            
        }else if(resource.getPath() == null && (resource.getData() == null || resource.getFormat() == null)){
            DataPackageException dpe = new DataPackageException("Invalid Resource. The path property or the data and format properties cannot be null.");
            
            if(this.strictValidation){
                throw dpe;
            }else{
                errors.add(dpe);
            }
            
        }else{
            Iterator<Resource> iter = this.resources.iterator();

            // Check if there is duplication.
            while(iter.hasNext()){
                if(iter.next().getName().equalsIgnoreCase(resource.getName())){
                    DataPackageException dpe = new DataPackageException("A resource with the same name already exists.");

                    if(this.strictValidation){
                        throw dpe;
                    }else{
                        errors.add(dpe);
                    }
                }
            }
        }
        
        // Validate.
        this.validate();
        
        this.resources.add(resource);
    }
    
    public void removeResource(String name){
        this.resources.removeIf(resource -> resource.getName().equalsIgnoreCase(name));
    }
    
    public Object getProperty(String key){
        return this.getJson().get(key);
    }
    
    public Object getPropertyString(String key){
        return this.getJson().getString(key);
    }
    
    public Object getPropertyJSONObject(String key){
        return this.getJson().getJSONObject(key);
    }
    
    public Object getPropertyJSONArray(String key){
        return this.getJson().getJSONArray(key);
    }
    
    public void addProperty(String key, String value) throws DataPackageException{
        if(this.getJson().has(key)){
            throw new DataPackageException("A property with the same key already exists.");
        }else{
            this.getJson().put(key, value);
        }
    }
    
    public void addProperty(String key, JSONObject value) throws DataPackageException{
        if(this.getJson().has(key)){
            throw new DataPackageException("A property with the same key already exists.");
        }else{
            this.getJson().put(key, value);
        }
    }
    
    public void addProperty(String key, JSONArray value) throws DataPackageException{
        if(this.getJson().has(key)){
            throw new DataPackageException("A property with the same key already exists.");
        }else{
            this.getJson().put(key, value);
        }
    }
    
    public void removeProperty(String key){
        this.getJson().remove(key);
    }
    
    /**
     * Validation is strict or unstrict depending on how the package was
     * instanciated with the strict flag.
     * @throws IOException
     * @throws DataPackageException
     * @throws ValidationException 
     */
    public final void validate() throws IOException, DataPackageException, ValidationException{
        try{
            this.validator.validate(this.getJson());
            
        }catch(ValidationException ve){
            if(this.strictValidation){
                throw ve;
            }else{
                errors.add(ve);
            }
        }
    }
    
    final public String getBasePath(){
        return this.basePath;
    }
    
    final public void setBasePath(String basePath){
        this.basePath = basePath;
    }
    
    public JSONObject getJson(){
        Iterator<Resource> resourceIter = resources.iterator();
        
        JSONArray resourcesJsonArray = new JSONArray();
        while(resourceIter.hasNext()){
            Resource resource = resourceIter.next();
            resourcesJsonArray.put(resource.getJson());
        }
        
        if(resourcesJsonArray.length() > 0){
            this.jsonObject.put(JSON_KEY_RESOURCES, resourcesJsonArray);
        }

        return this.jsonObject;
    }
    
    public List<Exception> getErrors(){
        return this.errors;
    }
    
    private String getJsonStringContentFromRemoteFile(URL url) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            StringBuilder builder = new StringBuilder();
            int read;
            char[] chars = new char[1024];
            
            while ((read = reader.read(chars)) != -1){
                builder.append(chars, 0, read); 
            }

            return builder.toString();
        }
    }
    
    private String getJsonStringContentFromLocalFile(String absoluteFilePath) throws JSONException{
        // Read file, it should be a JSON.
        try{
            String jsonString = new String(Files.readAllBytes(Paths.get(absoluteFilePath)));
            return jsonString;
            
        }catch(IOException ioe){
            // FIXME: Come up with better exception handling?
            return null;
        }
    }
    
    private void setJson(JSONObject jsonObjectSource) throws IOException, MalformedURLException, FileNotFoundException, DataPackageException{
        this.jsonObject = jsonObjectSource;
        
        // Create Resource list, is there are resources.
        if(jsonObjectSource.has(JSON_KEY_RESOURCES)){
            JSONArray resourcesJsonArray = jsonObjectSource.getJSONArray(JSON_KEY_RESOURCES);
            for(int i=0; i < resourcesJsonArray.length(); i++){
                JSONObject resourceJson = resourcesJsonArray.getJSONObject(i);

                //FIXME: Again, could be greatly simplified amd much more
                // elegant if we use a library like GJSON...
                String name = resourceJson.has(Resource.JSON_KEY_NAME) ? resourceJson.getString(Resource.JSON_KEY_NAME) : null;
                Object path = resourceJson.has(Resource.JSON_KEY_PATH) ? resourceJson.get(Resource.JSON_KEY_PATH) : null;
                Object data = resourceJson.has(Resource.JSON_KEY_DATA) ? resourceJson.get(Resource.JSON_KEY_DATA) : null;
                String profile = resourceJson.has(Resource.JSON_KEY_PROFILE) ? resourceJson.getString(Resource.JSON_KEY_PROFILE) : null;
                String title = resourceJson.has(Resource.JSON_KEY_TITLE) ? resourceJson.getString(Resource.JSON_KEY_TITLE) : null;
                String description = resourceJson.has(Resource.JSON_KEY_DESCRIPTION) ? resourceJson.getString(Resource.JSON_KEY_DESCRIPTION) : null;
                String format = resourceJson.has(Resource.JSON_KEY_FORMAT) ? resourceJson.getString(Resource.JSON_KEY_FORMAT) : null;
                String mediaType = resourceJson.has(Resource.JSON_KEY_MEDIA_TYPE) ? resourceJson.getString(Resource.JSON_KEY_MEDIA_TYPE) : null;
                String encoding = resourceJson.has(Resource.JSON_KEY_ENCODING) ? resourceJson.getString(Resource.JSON_KEY_ENCODING) : null;
                Integer bytes = resourceJson.has(Resource.JSON_KEY_BYTES) ? resourceJson.getInt(Resource.JSON_KEY_BYTES) : null;
                String hash = resourceJson.has(Resource.JSON_KEY_HASH) ? resourceJson.getString(Resource.JSON_KEY_HASH) : null;
 
                JSONArray sources = resourceJson.has(Resource.JSON_KEY_SOURCES) ? resourceJson.getJSONArray(Resource.JSON_KEY_SOURCES) : null;
                JSONArray licenses = resourceJson.has(Resource.JSON_KEY_LICENSES) ? resourceJson.getJSONArray(Resource.JSON_KEY_LICENSES) : null;
                
                // Get the schema and dereference it. Enables validation against it.
                Object schemaObj = resourceJson.has(Resource.JSON_KEY_SCHEMA) ? resourceJson.get(Resource.JSON_KEY_SCHEMA) : null;
                JSONObject dereferencedSchema = this.getDereferencedObject(schemaObj);

                // Now we can build the resource objects
                Resource resource = null;
                
                if(path != null){
                    // Get the dialect and dereference it. Enables validation against it.
                    Object dialectObj = resourceJson.has(Resource.JSON_KEY_DIALECT) ? resourceJson.get(Resource.JSON_KEY_DIALECT) : null; 
                    JSONObject dereferencedDialect = this.getDereferencedObject(dialectObj);
                
                    resource = new Resource(name, path, dereferencedSchema, dereferencedDialect,
                        profile, title, description, mediaType, encoding, bytes, hash, sources, licenses);
                    
                }else if(data != null && format != null){
                    resource = new Resource(name, data, format, dereferencedSchema,
                        profile, title, description, mediaType, encoding, bytes, hash, sources, licenses);
                    
                }else{
                    DataPackageException dpe = new DataPackageException("Invalid Resource. The path property or the data and format properties cannot be null.");

                    if(this.strictValidation){
                        this.jsonObject = null;
                        this.resources.clear();

                        throw dpe;
                        
                    }else{
                        this.errors.add(dpe);
                    }
                }

                if(resource != null){
                    this.resources.add(resource);
                }
                
            }         
        }  
    }
    
    private JSONObject getDereferencedObject(Object obj) throws IOException, FileNotFoundException, MalformedURLException{
        // The JSONObject that will represent the schema.
        JSONObject dereferencedObj = null;

        // Object is already a dereferences object.
        if(obj instanceof JSONObject){
            
            // Don't need to do anything, just cast and return.
            dereferencedObj = (JSONObject)obj;

        }else if(obj instanceof String){
            
            // The string value of the given object value.
            String objStr = (String)obj;

            // If object value is Url.
            // Grab the JSON string content of that remote file.
            String[] schemes = {"http", "https"};
            UrlValidator urlValidator = new UrlValidator(schemes);

            if (urlValidator.isValid(objStr)) {

                // Create the dereferenced object from the remote file.
                String jsonContentString = this.getJsonStringContentFromRemoteFile(new URL(objStr));
                dereferencedObj = new JSONObject(jsonContentString);

            }else{
                // If schema is file path.
                File sourceFile = new File(objStr);  
                if(sourceFile.exists()){
                    // Create the dereferenced schema object from the local file.
                    String jsonContentString = this.getJsonStringContentFromLocalFile(sourceFile.getAbsolutePath());
                    dereferencedObj = new JSONObject(jsonContentString);

                }else{
                    throw new FileNotFoundException("Local file not found: " + sourceFile);
                }
            }
        }
        
        return dereferencedObj;
    }
}
