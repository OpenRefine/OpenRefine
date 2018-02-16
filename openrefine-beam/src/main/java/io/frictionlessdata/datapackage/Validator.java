package io.frictionlessdata.datapackage;
import io.frictionlessdata.datapackage.exceptions.DataPackageException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.validator.routines.UrlValidator;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * Validates against schema.
 */
public class Validator {

    /**
     * Validates a given JSON Object against the default profile schema.
     * @param jsonObjectToValidate
     * @throws IOException
     * @throws DataPackageException
     * @throws ValidationException 
     */
    public void validate(JSONObject jsonObjectToValidate) throws IOException, DataPackageException, ValidationException{
        
        // If a profile value is provided.
        if(jsonObjectToValidate.has(Package.JSON_KEY_PROFILE)){
            String profile = jsonObjectToValidate.getString(Package.JSON_KEY_PROFILE);
            
            String[] schemes = {"http", "https"};
            UrlValidator urlValidator = new UrlValidator(schemes);
            
            if (urlValidator.isValid(profile)) {
                this.validate(jsonObjectToValidate, new URL(profile));
            }else{
                this.validate(jsonObjectToValidate, profile);
            }
            
        }else{
            // If no profile value is provided, use default value.
            this.validate(jsonObjectToValidate, Profile.PROFILE_DEFAULT);
        }   
    }
    
    /**
     * Validates a given JSON Object against the a given profile schema.
     * @param jsonObjectToValidate
     * @param profileId
     * @throws DataPackageException
     * @throws ValidationException 
     */
    public void validate(JSONObject jsonObjectToValidate, String profileId) throws DataPackageException, ValidationException{ 

        InputStream inputStream = Validator.class.getResourceAsStream("/schemas/" + profileId + ".json");
        if(inputStream != null){
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(jsonObjectToValidate); // throws a ValidationException if this object is invalid
            
        }else{
            throw new DataPackageException("Invalid profile id: " + profileId);
        }
        
    }
    
    /**
     * 
     * @param jsonObjectToValidate
     * @param schemaUrl
     * @throws IOException
     * @throws DataPackageException
     * @throws ValidationException 
     */
    public void validate(JSONObject jsonObjectToValidate, URL schemaUrl) throws IOException, DataPackageException, ValidationException{
        try{
            InputStream inputStream = schemaUrl.openStream();
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(jsonObjectToValidate); // throws a ValidationException if this object is invalid
            
        }catch(FileNotFoundException e){
             throw new DataPackageException("Invalid profile schema URL: " + schemaUrl);   
        }  
    }
    
    /**
     * Validates a given JSON String against the default profile schema.
     * @param jsonStringToValidate
     * @throws IOException
     * @throws DataPackageException
     * @throws ValidationException 
     */
    public void validate(String jsonStringToValidate) throws IOException, DataPackageException, ValidationException{
        JSONObject jsonObject = new JSONObject(jsonStringToValidate);
        validate(jsonObject);
    }
}
