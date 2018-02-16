package io.frictionlessdata.tableschema;

import io.frictionlessdata.tableschema.exceptions.TypeInferringException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


/**
 * The type inferral algorithm tries to cast to available types and each successful
 * type casting increments a popularity score for the successful type cast in question.
 * At the end, the best score so far is returned.
 */
public class TypeInferrer {
    
    /**
     * We are using reflection to go through the cast methods
     * so we want to make this a Singleton to avoid instanciating
     * a new class for every cast method call attempt.
     */
    private static TypeInferrer instance = null;
    
    private Schema geoJsonSchema = null;
    private Schema topoJsonSchema = null;
    
    private Map<String, Map<String, Integer>> typeInferralMap = new HashMap();
    
    // The order in which the types will be attempted to be inferred.
    // Once a type is successfully inferred, we do not bother with the remaining types.
    private static final List<String[]> TYPE_INFERRAL_ORDER_LIST = new ArrayList<>(Arrays.asList(
        new String[]{Field.FIELD_TYPE_GEOPOINT, Field.FIELD_FORMAT_DEFAULT},
        new String[]{Field.FIELD_TYPE_GEOPOINT, Field.FIELD_FORMAT_ARRAY},
        new String[]{Field.FIELD_TYPE_GEOPOINT, Field.FIELD_FORMAT_OBJECT},
        new String[]{Field.FIELD_TYPE_DURATION, Field.FIELD_FORMAT_DEFAULT}, // No different formats, just use default.
        new String[]{Field.FIELD_TYPE_YEAR, Field.FIELD_FORMAT_DEFAULT}, // No different formats, just use default.
        new String[]{Field.FIELD_TYPE_YEARMONTH, Field.FIELD_FORMAT_DEFAULT}, // No different formats, just use default.
        new String[]{Field.FIELD_TYPE_DATE, Field.FIELD_FORMAT_DEFAULT}, // No different formats, just use default.
        new String[]{Field.FIELD_TYPE_TIME, Field.FIELD_FORMAT_DEFAULT}, // No different formats, just use default.
        new String[]{Field.FIELD_TYPE_DATETIME, Field.FIELD_FORMAT_DEFAULT}, // No different formats, just use default.
        new String[]{Field.FIELD_TYPE_INTEGER, Field.FIELD_FORMAT_DEFAULT}, // No different formats, just use default.
        new String[]{Field.FIELD_TYPE_NUMBER, Field.FIELD_FORMAT_DEFAULT}, // No different formats, just use default.
        new String[]{Field.FIELD_TYPE_BOOLEAN, Field.FIELD_FORMAT_DEFAULT}, // No different formats, just use default.
        new String[]{Field.FIELD_TYPE_GEOJSON, Field.FIELD_FORMAT_DEFAULT},
        new String[]{Field.FIELD_TYPE_GEOJSON, Field.FIELD_FORMAT_TOPOJSON},
        new String[]{Field.FIELD_TYPE_OBJECT, Field.FIELD_FORMAT_DEFAULT},
        new String[]{Field.FIELD_TYPE_ARRAY, Field.FIELD_FORMAT_DEFAULT},
        new String[]{Field.FIELD_TYPE_STRING, Field.FIELD_FORMAT_DEFAULT}, // No different formats, just use default.
        new String[]{Field.FIELD_TYPE_ANY, Field.FIELD_FORMAT_DEFAULT})); // No different formats, just use default.
    
    private static final String NUMBER_OPTION_DECIMAL_CHAR = "decimalChar";
    private static final String NUMBER_OPTION_GROUP_CHAR = "groupChar";
    private static final String NUMBER_OPTION_BARE_NUMBER = "bareNumber";
    private static final String NUMBER_DEFAULT_DECIMAL_CHAR = ".";
    private static final String NUMBER_DEFAULT_GROUP_CHAR = "";
    
    // ISO 8601 format of yyyy-MM-dd'T'HH:mm:ss.SSSZ in UTC time
    private static final String REGEX_DATETIME = "(-?(?:[1-9][0-9]*)?[0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])(\\.[0-9]+)?(Z|[+-](?:2[0-3]|[01][0-9]):[0-5][0-9])?";
    
    // ISO8601 format yyyy-MM-dd
    private static final String REGEX_DATE = "([0-9]{4})-(1[0-2]|0[1-9])-(3[0-1]|0[1-9]|[1-2][0-9])";
    
    // An ISO8601 time string e.g. HH:mm:ss
    private static final String REGEX_TIME = "(2[0-3]|[01]?[0-9]):?([0-5]?[0-9]):?([0-5]?[0-9])";
    
    // yyyy
    private static final String REGEX_YEAR = "([0-9]{4})";
    
    // yyyy-MM
    private static final String REGEX_YEARMONTH = "([0-9]{4})-(1[0-2]|0[1-9])";
    
    private static final String REGEX_FLOAT = "([+-]?\\d*\\.?\\d*)";
    private static final String REGEX_INTEGER = "[+-]?\\d+";
    private static final String REGEX_BARE_NUMBER = "((^\\D*)|(\\D*$))";
    
    private TypeInferrer(){
        // Private to inforce use of Singleton pattern.
    }
    
    public static TypeInferrer getInstance() {
      if(instance == null) {
         instance = new TypeInferrer();
      }
      return instance;
   }
    
    /**
     * Infer the data types and return the generated schema.
     * @param data
     * @param headers
     * @return
     * @throws TypeInferringException 
     */
    public synchronized JSONObject infer(List<Object[]> data, String[] headers) throws TypeInferringException{
        return this.infer(data, headers, data.size()-1);
    }
    
    /**
     * Infer the data types and return the generated schema.
     * @param data
     * @param headers
     * @param rowLimit
     * @return
     * @throws TypeInferringException 
     */
    public synchronized JSONObject infer(List<Object[]> data, String[] headers, int rowLimit) throws TypeInferringException{
        
        // If the given row limit is bigger than the length of the data
        // then just use the length of the data.
        if(rowLimit > data.size()-1){
            rowLimit = data.size()-1;
        }

        // The JSON Array that will define the fields in the schema JSON Object.
        JSONArray tableFieldJsonArray = new JSONArray();
        
        // Init the type inferral map and init the schema objects
        for (String header : headers) {
            // Init the type inferral map to track our inferences for each row.
            this.getTypeInferralMap().put(header, new HashMap());
            
            // Init the schema objects
            JSONObject fieldObj = new JSONObject();
            fieldObj.put(Field.JSON_KEY_NAME, header);
            fieldObj.put(Field.JSON_KEY_TITLE, ""); // This will stay blank.
            fieldObj.put(Field.JSON_KEY_DESCRIPTION, ""); // This will stay blank.
            fieldObj.put(Field.JSON_KEY_CONSTRAINTS, new JSONObject()); // This will stay blank.
            fieldObj.put(Field.JSON_KEY_FORMAT, ""); // This will bet set post inferral.
            fieldObj.put(Field.JSON_KEY_TYPE, ""); // This will bet set post inferral.
            
            // Wrap it all in an array.
            tableFieldJsonArray.put(fieldObj);
        }

        // Find the type for each column data for each row.
        // This uses method invokation via reflection in a foor loop that iterates
        // for each possible type/format combo. Insprect the findType method for implementation.
        for(int i = 0; i <= rowLimit; i++){
            Object[] row = data.get(i);
            
            for(int j = 0; j < row.length; j++){
                this.findType(headers[j], row[j].toString());
            }
        }
        
        // We are done inferring types.
        // Now for each field we figure out which type was the most inferred and settle for that type
        // as the final type for the field.
        for(int j=0; j < tableFieldJsonArray.length(); j++){
            String fieldName = tableFieldJsonArray.getJSONObject(j).getString(Field.JSON_KEY_NAME);
            HashMap<String, Integer> typeInferralCountMap = (HashMap<String, Integer>)this.getTypeInferralMap().get(fieldName);
            TreeMap<String, Integer> typeInferralCountMapSortedByCount = sortMapByValue(typeInferralCountMap); 
           
            if(!typeInferralCountMapSortedByCount.isEmpty()){
                String inferredType = typeInferralCountMapSortedByCount.firstEntry().getKey();
                tableFieldJsonArray.getJSONObject(j).put(Field.JSON_KEY_TYPE, inferredType);
            }
            
        }
        
        // Need to clear the inferral map for the next inferral call:
        this.getTypeInferralMap().clear();
        
        // Now that the types have been inferred and set, we build and return the schema object.
        JSONObject schemaJsonObject = new JSONObject();
        schemaJsonObject.put(io.frictionlessdata.tableschema.Schema.JSON_KEY_FIELDS, tableFieldJsonArray);
        
        return schemaJsonObject;
    }
    
    private void findType(String header, String datum){
        
        // Invoke all the type casting methods using reflection.
        for(String[] typeInferralDefinition: TYPE_INFERRAL_ORDER_LIST){
            try{
                // Keep invoking the type casting methods until one doesn't throw an exception
                String dataType = typeInferralDefinition[0];
                String castMethodName = "cast" + (dataType.substring(0, 1).toUpperCase() + dataType.substring(1));
                String format = typeInferralDefinition[1];
                 
                Method method = TypeInferrer.class.getMethod(castMethodName, String.class, String.class);
                // Single pattern is useful here:
                method.invoke(TypeInferrer.getInstance(), format, datum);
                
                // If no exception is thrown, in means that a type has been inferred.
                // Let's keep track of it in the inferral map.
                this.updateInferralScoreMap(header, dataType);
                
                // We no longer need to try to infer other types. 
                // Let's break out of the loop.
                break;

            }catch (Exception e) {
                // Do nothing.
                // An exception here means that we failed to infer with the current type.
                // Move on to attempt with the next type in the following iteration.
            }
        }
    }
    
    /**
     * The type inferral map is where we keep track of the types inferred for values within the same field.
     * @param header
     * @param typeKey 
     */
    private void updateInferralScoreMap(String header, String typeKey){
        if(this.getTypeInferralMap().get(header).containsKey(typeKey)){
            int newCount = this.typeInferralMap.get(header).get(typeKey) + 1;
            this.getTypeInferralMap().get(header).replace(typeKey, newCount);
        }else{
            this.getTypeInferralMap().get(header).put(typeKey, 1);
        }
    }
    
    /**
     * We use a map to keep track the inferred type counts for each field.
     * Once we are done inferring, we settle for the type with that was inferred the most for the same field.
     * @param map
     * @return 
     */
    private TreeMap<String, Integer> sortMapByValue(Map<String, Integer> map){
        Comparator<String> comparator = new MapValueComparator(map);
        TreeMap<String, Integer> result = new TreeMap<>(comparator);
        result.putAll(map);

        return result;
    }
    
    public Duration castDuration(String format, String value) throws TypeInferringException{
        return this.castDuration(format, value, null);
    }
    
    /**
     * Using regex only tests the pattern.
     * Unfortunately, this approach does not test the validity of the date value itself.
     * @param format
     * @param value
     * @param options
     * @return
     * @throws TypeInferringException 
     */
    public Duration castDuration(String format, String value, Map<String, Object> options) throws TypeInferringException{
        try{
            return Duration.parse(value); 
        }catch(Exception e){
            throw new TypeInferringException();
        }
    }
    
    public JSONObject castGeojson(String format, String value) throws TypeInferringException{
        return this.castGeojson(format, value, null);
    }
    
    /**
     * Validate against GeoJSON or TopoJSON schema.
     * @param format
     * @param value
     * @param options
     * @return
     * @throws TypeInferringException 
     */
    public JSONObject castGeojson(String format, String value, Map<String, Object> options) throws TypeInferringException{
        JSONObject jsonObj = null;
        
        try {

            jsonObj = new JSONObject(value);

            try{
                if(format.equalsIgnoreCase(Field.FIELD_FORMAT_DEFAULT)){
                    validateGeoJsonSchema(jsonObj);

                }else if(format.equalsIgnoreCase(Field.FIELD_FORMAT_TOPOJSON)){
                    validateTopoJsonSchema(jsonObj);

                }else{
                    throw new TypeInferringException();
                }

            }catch(ValidationException ve){
                // Not a valid GeoJSON or TopoJSON.
                throw new TypeInferringException();
            }
        }catch(JSONException je){
            // Not a valid JSON.
            throw new TypeInferringException();
        }
        
        return jsonObj;
    }
    
    /**
     * We only want to go through this initialization if we have to because it's a
     * performance issue the first time it is executed.
     * Because of this, so we don't include this logic in the constructor and only
     * call it when it is actually required after trying all other type inferral.
     * @param geoJson
     * @throws ValidationException 
     */
    private void validateGeoJsonSchema(JSONObject geoJson) throws ValidationException{
        if(this.getGeoJsonSchema() == null){
            // FIXME: Maybe this infering against geojson scheme is too much.
            // Grabbed geojson schema from here: https://github.com/fge/sample-json-schemas/tree/master/geojson
            InputStream geoJsonSchemaInputStream = TypeInferrer.class.getResourceAsStream("/schemas/geojson/geojson.json");
            JSONObject rawGeoJsonSchema = new JSONObject(new JSONTokener(geoJsonSchemaInputStream));
            this.setGeoJsonSchema(SchemaLoader.load(rawGeoJsonSchema));
        }
        this.getGeoJsonSchema().validate(geoJson);
    }
    
    /**
     * We only want to go through this initialization if we have to because it's a
     * performance issue the first time it is executed.
     * Because of this, so we don't include this logic in the constructor and only
     * call it when it is actually required after trying all other type inferral.
     * @param topoJson 
     */
    private void validateTopoJsonSchema(JSONObject topoJson){
        if(this.getTopoJsonSchema() == null){
            // FIXME: Maybe this infering against topojson scheme is too much.
            // Grabbed topojson schema from here: https://github.com/nhuebel/TopoJSON_schema
            InputStream topoJsonSchemaInputStream = TypeInferrer.class.getResourceAsStream("/schemas/geojson/topojson.json");
            JSONObject rawTopoJsonSchema = new JSONObject(new JSONTokener(topoJsonSchemaInputStream));
            this.setTopoJsonSchema(SchemaLoader.load(rawTopoJsonSchema));
        }
        this.getTopoJsonSchema().validate(topoJson);
    }
    
    public int[] castGeopoint(String format, String value) throws TypeInferringException{
        return this.castGeopoint(format, value, null);
    }
    
  
    /**
     * Only validates against pattern.
     * Does not validate against min/max brackets -180:180 for lon and -90:90 for lat.
     * @param format can be either default, array, or object.
     * @param value
     * @param options
     * @return
     * @throws TypeInferringException 
     */
    public int[] castGeopoint(String format, String value, Map<String, Object> options) throws TypeInferringException{
        try{
            if(format.equalsIgnoreCase(Field.FIELD_FORMAT_DEFAULT)){
                String[] geopoint = value.split(",");

                if(geopoint.length == 2){
                    int lon = Integer.parseInt(geopoint[0]);
                    int lat = Integer.parseInt(geopoint[1]);
                    
                    // No exceptions? It's a valid geopoint object.
                    return new int[]{lon, lat};
                    
                }else{
                    throw new TypeInferringException();
                }

            }else if(format.equalsIgnoreCase(Field.FIELD_FORMAT_ARRAY)){

                // This will throw an exception if the value is not an array.
                JSONArray jsonArray = new JSONArray(value);
                
                if (jsonArray.length() == 2){
                    int lon = jsonArray.getInt(0);
                    int lat = jsonArray.getInt(1);

                    // No exceptions? It's a valid geopoint object.
                    return new int[]{lon, lat};

                }else{
                    throw new TypeInferringException();
                }     

            }else if(format.equalsIgnoreCase(Field.FIELD_FORMAT_OBJECT)){

                // This will throw an exception if the value is not an object.
                JSONObject jsonObj = new JSONObject(value);
                
                if (jsonObj.length() == 2 && jsonObj.has("lon") && jsonObj.has("lat")){
                    int lon = jsonObj.getInt("lon");
                    int lat = jsonObj.getInt("lat");

                    // No exceptions? It's a valid geopoint object.
                    return new int[]{lon, lat};

                }else{
                    throw new TypeInferringException();
                }

            }else{
                throw new TypeInferringException();
            }

        }catch(Exception e){
            throw new TypeInferringException();
        }
    }
    
    public JSONObject castObject(String format, String value) throws TypeInferringException{
        return this.castObject(format, value, null);
    }
 
    public JSONObject castObject(String format, String value, Map<String, Object> options) throws TypeInferringException{
        try {
            return new JSONObject(value);
        }catch(JSONException je){
            throw new TypeInferringException();
        }       
    }
    
    public JSONArray castArray(String format, String value) throws TypeInferringException{
        return this.castArray(format, value, null);
    }
    
    public JSONArray castArray(String format, String value, Map<String, Object> options) throws TypeInferringException{
        try {
            return new JSONArray(value);
        }catch(JSONException je){
            throw new TypeInferringException();
        } 
    }
    
    public DateTime castDatetime(String format, String value) throws TypeInferringException{
        return this.castDatetime(format, value, null);
    }
    
    public DateTime castDatetime(String format, String value, Map<String, Object> options) throws TypeInferringException{
   
        Pattern pattern = Pattern.compile(REGEX_DATETIME);
        Matcher matcher = pattern.matcher(value);
        
        if(matcher.matches()){
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            DateTime dt = formatter.parseDateTime(value);
            
            return dt;
            
        }else{
            throw new TypeInferringException();
        }  
    }
    
    public DateTime castTime(String format, String value) throws TypeInferringException{
        return this.castTime(format, value, null);
    }
    
    public DateTime castTime(String format, String value, Map<String, Object> options) throws TypeInferringException{
        Pattern pattern = Pattern.compile(REGEX_TIME);
        Matcher matcher = pattern.matcher(value);
        
        if(matcher.matches()){
            DateTimeFormatter formatter = DateTimeFormat.forPattern("HH:mm:ss");
            DateTime dt = formatter.parseDateTime(value);
            
            return dt;
            
        }else{
            throw new TypeInferringException();
        } 
    }
    
    public DateTime castDate(String format, String value) throws TypeInferringException{
        return this.castDate(format, value, null);
    }
    
    public DateTime castDate(String format, String value, Map<String, Object> options) throws TypeInferringException{
        
        Pattern pattern = Pattern.compile(REGEX_DATE);
        Matcher matcher = pattern.matcher(value);
        
        if(matcher.matches()){
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
            DateTime dt = formatter.parseDateTime(value);
            
            return dt;
            
        }else{
            throw new TypeInferringException();
        } 
    }
    
    public int castYear(String format, String value) throws TypeInferringException{
        return this.castYear(format, value, null);
    }
    
    public int castYear(String format, String value, Map<String, Object> options) throws TypeInferringException{
        Pattern pattern = Pattern.compile(REGEX_YEAR);
        Matcher matcher = pattern.matcher(value);
        
        if(matcher.matches()){
            int year = Integer.parseInt(value);
            return year;
            
        }else{
            throw new TypeInferringException();
        } 
    }
    
    public DateTime castYearmonth(String format, String value) throws TypeInferringException{
        return this.castYearmonth(format, value, null);
    }
    
    public DateTime castYearmonth(String format, String value, Map<String, Object> options) throws TypeInferringException{
        Pattern pattern = Pattern.compile(REGEX_YEARMONTH);
        Matcher matcher = pattern.matcher(value);
        
        if(matcher.matches()){
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM");
            DateTime dt = formatter.parseDateTime(value);
            
            return dt;
            
        }else{
            throw new TypeInferringException();
        } 
    }
    
    public int castInteger(String format, String value) throws TypeInferringException{
        return this.castInteger(format, value, null);
    }
    
    public int castInteger(String format, String value, Map<String, Object> options) throws TypeInferringException{
        try{
            return Integer.parseInt(value);
        }catch(NumberFormatException nfe){
            throw new TypeInferringException();
        }
    }
    
    public Object castNumber(String format, String value) throws TypeInferringException{   
        return castNumber(format, value, null);
    }
    
    public Object castNumber(String format, String value, Map<String, Object> options) throws TypeInferringException{ 
        try{
            
            if(options != null){
                if(options.containsKey(NUMBER_OPTION_DECIMAL_CHAR)){
                    value = value.replace((String)options.get(NUMBER_OPTION_DECIMAL_CHAR), NUMBER_DEFAULT_DECIMAL_CHAR);
                }
            
                if(options.containsKey(NUMBER_OPTION_GROUP_CHAR)){
                    value = value.replace((String)options.get(NUMBER_OPTION_GROUP_CHAR), NUMBER_DEFAULT_GROUP_CHAR);
                }

                if(options.containsKey(NUMBER_OPTION_BARE_NUMBER) && !(boolean)options.get(NUMBER_OPTION_BARE_NUMBER)){
                    value = value.replaceAll(REGEX_BARE_NUMBER, "");
                }             
            }
            
            // Try to match integer pattern
            Pattern intergerPattern = Pattern.compile(REGEX_INTEGER);
            Matcher integerMatcher = intergerPattern.matcher(value);
            
            if(integerMatcher.matches()){
                return Integer.parseInt(value);
            }
            
            // Try to match float pattern
            Pattern floatPattern = Pattern.compile(REGEX_FLOAT);
            Matcher floatMatcher = floatPattern.matcher(value);
        
            if(floatMatcher.matches()){
                return Float.parseFloat(value);
            }
                        
            // The value failed to match neither the Float or the Integer value.
            // Throw exception.
            throw new TypeInferringException();
            
        }catch(Exception e){
            throw new TypeInferringException();
        } 
    }
    
    public boolean castBoolean(String format, String value) throws TypeInferringException{
        return this.castBoolean(format, value, null);
    }
    
    public boolean castBoolean(String format, String value, Map<String, Object> options) throws TypeInferringException{
        if(Arrays.asList(new String[]{"yes", "y", "true", "t", "1"}).contains(value.toLowerCase())){
            return true;
            
        }else if(Arrays.asList(new String[]{"no", "n", "false", "f", "0"}).contains(value.toLowerCase())){
            return false;
            
        }else{
            throw new TypeInferringException();
        }
    }
    
    public String castString(String format, String value) throws TypeInferringException{
        return this.castAny(format, value, null);
    }
    
    /**
     * Can be either default, e-mail, uri, binary, or uuid.
     * @param format
     * @param value
     * @param options
     * @return
     * @throws TypeInferringException 
     */
    public String castString(String format, String value, Map<String, Object> options) throws TypeInferringException{
        return value;
    }
    
    public String castAny(String format, String value) throws TypeInferringException{
        return this.castAny(format, value, null);
    }
    
    public String castAny(String format, String value, Map<String, Object> options) throws TypeInferringException{
        return value;
    }
    
    private Schema getGeoJsonSchema(){
        return this.geoJsonSchema;
    }
    
    private void setGeoJsonSchema(Schema geoJsonSchema){
        this.geoJsonSchema = geoJsonSchema;
    }
    
    private Schema getTopoJsonSchema(){
        return this.topoJsonSchema;
    }
    
    private void setTopoJsonSchema(Schema topoJsonSchema){
        this.topoJsonSchema = topoJsonSchema;
    }
    
    private Map<String, Map<String, Integer>> getTypeInferralMap(){
        return this.typeInferralMap;
    }
}