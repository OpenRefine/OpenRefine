package io.frictionlessdata.tableschema;

import io.frictionlessdata.tableschema.exceptions.ConstraintsException;
import io.frictionlessdata.tableschema.exceptions.InvalidCastException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * 
 */
public class Field {
    public static final String FIELD_TYPE_STRING = "string";
    public static final String FIELD_TYPE_INTEGER = "integer";
    public static final String FIELD_TYPE_NUMBER = "number";
    public static final String FIELD_TYPE_BOOLEAN = "boolean";
    public static final String FIELD_TYPE_OBJECT = "object";
    public static final String FIELD_TYPE_ARRAY = "array";
    public static final String FIELD_TYPE_DATE = "date";
    public static final String FIELD_TYPE_TIME = "time";
    public static final String FIELD_TYPE_DATETIME = "datetime";
    public static final String FIELD_TYPE_YEAR = "year";
    public static final String FIELD_TYPE_YEARMONTH = "yearmonth";
    public static final String FIELD_TYPE_DURATION = "duration";
    public static final String FIELD_TYPE_GEOPOINT = "geopoint";
    public static final String FIELD_TYPE_GEOJSON = "geojson";
    public static final String FIELD_TYPE_ANY = "any";
    
    public static final String FIELD_FORMAT_DEFAULT = "default";
    public static final String FIELD_FORMAT_ARRAY = "array";
    public static final String FIELD_FORMAT_OBJECT = "object";
    public static final String FIELD_FORMAT_TOPOJSON = "topojson";
    
    public static final String CONSTRAINT_KEY_REQUIRED = "required";
    public static final String CONSTRAINT_KEY_UNIQUE = "unique";
    public static final String CONSTRAINT_KEY_MIN_LENGTH = "minLength";
    public static final String CONSTRAINT_KEY_MAX_LENGTH = "maxLength";
    public static final String CONSTRAINT_KEY_MINIMUM = "minimum";
    public static final String CONSTRAINT_KEY_MAXIMUM = "maximum";
    public static final String CONSTRAINT_KEY_PATTERN = "pattern";
    public static final String CONSTRAINT_KEY_ENUM = "enum";
    
    public static final String JSON_KEY_NAME = "name";
    public static final String JSON_KEY_TYPE = "type";
    public static final String JSON_KEY_FORMAT = "format";
    public static final String JSON_KEY_TITLE = "title";
    public static final String JSON_KEY_DESCRIPTION = "description";
    public static final String JSON_KEY_CONSTRAINTS = "constraints";
  
    private String name = "";
    private String type = "";
    private String format = FIELD_FORMAT_DEFAULT;
    private String title = "";
    private String description = "";
    private Map<String, Object> constraints = null;
    
    public Field(String name, String type){
        this.name = name;
        this.type = type;
    }
    
    public Field(String name, String type, String format){
        this.name = name;
        this.type = type;
        this.format = format;
    }
    
    public Field(String name, String type, String format, String title, String description){
        this.name = name;
        this.type = type;
        this.format = format;
        this.title = title;
        this.description = description;
    }
    
    public Field(String name, String type, String format, String title, String description, Map constraints){
        this.name = name;
        this.type = type;
        this.format = format;
        this.title = title;
        this.description = description;
        this.constraints = constraints;
    }
    
    public Field(JSONObject field){
        //TODO: Maybe use Gson serializer for this instead? Is it worth importing library just for this?      
        this.name = field.has(JSON_KEY_NAME) ? field.getString(JSON_KEY_NAME) : "";
        this.type = field.has(JSON_KEY_TYPE) ? field.getString(JSON_KEY_TYPE) : "";
        this.format = field.has(JSON_KEY_FORMAT) ? field.getString(JSON_KEY_FORMAT) : FIELD_FORMAT_DEFAULT;
        this.title = field.has(JSON_KEY_TITLE) ? field.getString(JSON_KEY_TITLE) : "";
        this.description = field.has(JSON_KEY_DESCRIPTION) ? field.getString(JSON_KEY_DESCRIPTION) : "";
        this.constraints = field.has(JSON_KEY_CONSTRAINTS) ? field.getJSONObject(JSON_KEY_CONSTRAINTS).toMap() : null;
    }
    
    /**
     * 
     * @param <Any>
     * @param value
     * @return
     * @throws InvalidCastException
     * @throws ConstraintsException 
     */
    public <Any> Any castValue(String value) throws InvalidCastException, ConstraintsException{
        return this.castValue(value, true, null);
    }
    
    /**
     * Use the Field definition to cast a value into the Field type.
     * Enforces constraints by default.
     * @param <Any>
     * @param value
     * @param options
     * @return
     * @throws InvalidCastException
     * @throws ConstraintsException 
     */
    public <Any> Any castValue(String value, HashMap<String, Object> options) throws InvalidCastException, ConstraintsException{
        return this.castValue(value, true, options);
    }
    
    /**
     * 
     * @param <Any>
     * @param value
     * @param enforceConstraints
     * @return
     * @throws InvalidCastException
     * @throws ConstraintsException 
     */
    public <Any> Any castValue(String value, boolean enforceConstraints) throws InvalidCastException, ConstraintsException{
        return this.castValue(value, enforceConstraints, null);
    }
    
    /**
     * 
     * @param <Any>
     * @param value
     * @param enforceConstraints
     * @param options
     * @return
     * @throws InvalidCastException
     * @throws ConstraintsException 
     */
    public <Any> Any castValue(String value, boolean enforceConstraints, Map<String, Object> options) throws InvalidCastException, ConstraintsException{
        if(this.type.isEmpty()){
            throw new InvalidCastException();
        }else{
            try{
                // Using reflection to invoke appropriate type casting method from the TypeInferrer class
                String castMethodName = "cast" + (this.type.substring(0, 1).toUpperCase() + this.type.substring(1));
                Method method = TypeInferrer.class.getMethod(castMethodName, String.class, String.class, Map.class);
                Object castValue = method.invoke(TypeInferrer.getInstance(), this.format, value, options);
            
                // Check for constraint violations
                if(enforceConstraints && this.constraints != null){
                    Map<String, Object> violatedConstraints = checkConstraintViolations(castValue);
                    if(!violatedConstraints.isEmpty()){
                        throw new ConstraintsException();
                    }
                }
                
                return (Any)castValue;
                
            }catch(ConstraintsException ce){
                throw ce;
                
            }catch(Exception e){
                throw new InvalidCastException();
            }
        } 
    }
    
    /**
     * Returns a Map with all the constraints that have been violated.
     * @param value
     * @return 
     */
    public Map<String, Object> checkConstraintViolations(Object value){
       
        Map<String, Object> violatedConstraints = new HashMap();
        
        // Indicates whether this field is allowed to be null. If required is true, then null is disallowed. 
        if(this.constraints.containsKey(CONSTRAINT_KEY_REQUIRED)){
            if((boolean)this.constraints.get(CONSTRAINT_KEY_REQUIRED) == true && value == null){
                violatedConstraints.put(CONSTRAINT_KEY_REQUIRED, true);
            }
        }
        
        // All values for that field MUST be unique within the data file in which it is found.
        // Can't check UNIQUE constraint when operating with only one value.
        // TODO: Implement a method that takes List<Object> value as argument.
        /**
        if(this.constraints.containsKey(CONSTRAINT_KEY_UNIQUE)){
    
        }**/
        
        // An integer that specifies the minimum length of a value.
        if(this.constraints.containsKey(CONSTRAINT_KEY_MIN_LENGTH)){
            int minLength = (int)this.constraints.get(CONSTRAINT_KEY_MIN_LENGTH);
 
            if(value instanceof String){
                if(((String)value).length() < minLength){
                    violatedConstraints.put(CONSTRAINT_KEY_MIN_LENGTH, minLength);
                }
                
            }else if (value instanceof JSONObject){
                if(((JSONObject)value).length() < minLength){
                    violatedConstraints.put(CONSTRAINT_KEY_MIN_LENGTH, minLength);
                }
                 
            }else if (value instanceof JSONArray){
                if(((JSONArray)value).length() < minLength){
                    violatedConstraints.put(CONSTRAINT_KEY_MIN_LENGTH, minLength);
                }                
            }
        }
        
        // An integer that specifies the maximum length of a value.
        if(this.constraints.containsKey(CONSTRAINT_KEY_MAX_LENGTH)){
            int maxLength = (int)this.constraints.get(CONSTRAINT_KEY_MAX_LENGTH);
            
            if(value instanceof String){
                if(((String)value).length() > maxLength){
                    violatedConstraints.put(CONSTRAINT_KEY_MAX_LENGTH, maxLength);
                }
                
            }else if (value instanceof JSONObject){
                if(((JSONObject)value).length() > maxLength){
                    violatedConstraints.put(CONSTRAINT_KEY_MAX_LENGTH, maxLength);
                }
                 
            }else if (value instanceof JSONArray){
                if(((JSONArray)value).length() > maxLength){
                    violatedConstraints.put(CONSTRAINT_KEY_MAX_LENGTH, maxLength);
                }                
            }  
        }
        
        /**
         * Specifies a minimum value for a field.
         * This is different to minLength which checks the number of items in the value.
         * A minimum value constraint checks whether a field value is greater than or equal to the specified value.
         * The range checking depends on the type of the field.
         * E.g. an integer field may have a minimum value of 100; a date field might have a minimum date.
         * If a minimum value constraint is specified then the field descriptor MUST contain a type key.
         **/
        if(this.constraints.containsKey(CONSTRAINT_KEY_MINIMUM)){
            
            if(value instanceof Integer){
                int minInt = (int)this.constraints.get(CONSTRAINT_KEY_MINIMUM);
                if((int)value < minInt){
                    violatedConstraints.put(CONSTRAINT_KEY_MINIMUM, minInt);
                }
                
            }else if(value instanceof DateTime){
                DateTime minDateTime = (DateTime)this.constraints.get(CONSTRAINT_KEY_MINIMUM);
                if(((DateTime)value).isBefore(minDateTime)){
                    violatedConstraints.put(CONSTRAINT_KEY_MINIMUM, minDateTime);
                }
                
            }else if(value instanceof Duration){
                Duration minDuration = (Duration)this.constraints.get(CONSTRAINT_KEY_MINIMUM);
                if(((Duration)value).compareTo(minDuration) < 0){
                    violatedConstraints.put(CONSTRAINT_KEY_MINIMUM, minDuration);
                }
            } 
        }
        
        // As for minimum, but specifies a maximum value for a field.
        if(this.constraints.containsKey(CONSTRAINT_KEY_MAXIMUM)){
            
            if(value instanceof Integer){
                int maxInt = (int)this.constraints.get(CONSTRAINT_KEY_MAXIMUM);
                if((int)value > maxInt){
                    violatedConstraints.put(CONSTRAINT_KEY_MAXIMUM, maxInt);
                }
                
            }else if(value instanceof DateTime){
                DateTime maxDateTime = (DateTime)this.constraints.get(CONSTRAINT_KEY_MAXIMUM);
                
                if(((DateTime)value).isAfter(maxDateTime)){
                    violatedConstraints.put(CONSTRAINT_KEY_MAXIMUM, maxDateTime);
                }
                
            }else if(value instanceof Duration){
                Duration maxDuration = (Duration)this.constraints.get(CONSTRAINT_KEY_MAXIMUM);
                if(((Duration)value).compareTo(maxDuration) > 0){
                    violatedConstraints.put(CONSTRAINT_KEY_MAXIMUM, maxDuration);
                }
            } 
        }
        
        // A regular expression that can be used to test field values. If the regular expression matches then the value is valid.
        if(this.constraints.containsKey(CONSTRAINT_KEY_PATTERN)){
            String regexPatternString = (String)this.constraints.get(CONSTRAINT_KEY_PATTERN);
            
            // Constraint only applies to a String value.
            if(value instanceof String){       
                Pattern pattern = Pattern.compile(regexPatternString);
                Matcher matcher = pattern.matcher((String)value);
                
                if(!matcher.matches()){
                    violatedConstraints.put(CONSTRAINT_KEY_PATTERN, regexPatternString);
                }
            
            }else{
                // If the value is not a String, then just interpret as a constraint violation.
                violatedConstraints.put(CONSTRAINT_KEY_PATTERN, regexPatternString);
            }
        }
        
        // The value of the field must exactly match a value in the enum array.
        if(this.constraints.containsKey(CONSTRAINT_KEY_ENUM)){
            boolean violatesEnumConstraint = true;
            
            if(value instanceof String){
                List<String> stringList = (List<String>)this.constraints.get(CONSTRAINT_KEY_ENUM); 
                
                Iterator<String> iter = stringList.iterator();
                while(iter.hasNext()){
                    if(iter.next().equalsIgnoreCase((String)value)){
                        violatesEnumConstraint = false;
                        break;
                    }
                }
                
            }else if(value instanceof JSONObject){
                List<JSONObject> jsonObjList = (List<JSONObject>)this.constraints.get(CONSTRAINT_KEY_ENUM);
                
                Iterator<JSONObject> iter = jsonObjList.iterator();
                while(iter.hasNext()){
                    if(iter.next().similar((JSONObject)value)){
                        violatesEnumConstraint = false;
                        break;
                    }
                }
                
            }else if(value instanceof JSONArray){
                List<JSONArray> jsonArrList = (List<JSONArray>)this.constraints.get(CONSTRAINT_KEY_ENUM); 
                
                Iterator<JSONArray> iter = jsonArrList.iterator();
                while(iter.hasNext()){
                    if(iter.next().similar((JSONArray)value)){
                        violatesEnumConstraint = false;
                        break;
                    }
                }
                
            }else if(value instanceof Integer){
                List<Integer> intList = (List<Integer>)this.constraints.get(CONSTRAINT_KEY_ENUM);
                
                Iterator<Integer> iter = intList.iterator();
                while(iter.hasNext()){
                    if(iter.next() == (int)value){
                        violatesEnumConstraint = false;
                        break;
                    }
                }
                
            }else if(value instanceof DateTime){
                List<DateTime> dateTimeList = (List<DateTime>)this.constraints.get(CONSTRAINT_KEY_ENUM);
                
                Iterator<DateTime> iter = dateTimeList.iterator();
                while(iter.hasNext()){
                    if(iter.next().compareTo((DateTime)value) == 0){
                        violatesEnumConstraint = false;
                        break;
                    }
                }
                
            }else if(value instanceof Duration){
                List<Duration> durationList = (List<Duration>)this.constraints.get(CONSTRAINT_KEY_ENUM);
                
                Iterator<Duration> iter = durationList.iterator();
                while(iter.hasNext()){
                    if(iter.next().compareTo((Duration)value) == 0){
                        violatesEnumConstraint = false;
                        break;
                    }
                }
                
            }
            
            if(violatesEnumConstraint){
                violatedConstraints.put(CONSTRAINT_KEY_ENUM, this.constraints.get(CONSTRAINT_KEY_ENUM));
            }
        }
        
        return violatedConstraints;
    }
    
    /**
     * Get the JSON representation of the Field.
     * @return 
     */
    public JSONObject getJson(){
        //FIXME: Maybe we should use JSON serializer like Gson?
        JSONObject json = new JSONObject();
        json.put(JSON_KEY_NAME, this.name);
        json.put(JSON_KEY_TYPE, this.type);
        json.put(JSON_KEY_FORMAT, this.format);
        json.put(JSON_KEY_TITLE, this.title);
        json.put(JSON_KEY_DESCRIPTION, this.description);
        json.put(JSON_KEY_CONSTRAINTS, this.constraints);
        
        return json;
    }
    
    public String getName(){
        return this.name;
    }
    
    public String getType(){
        return this.type;
    }
    
    public String getFormat(){
        return this.format;
    }
    
    public String getTitle(){
        return this.title;
    }
    
    public String getDescription(){
        return this.description;
    }
    
    public Map<String, Object> getConstraints(){
        return this.constraints;
    }
}
