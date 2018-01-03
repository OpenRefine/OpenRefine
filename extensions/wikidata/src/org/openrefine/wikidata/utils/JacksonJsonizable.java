package org.openrefine.wikidata.utils;

import java.io.IOException;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.refine.Jsonizable;

/**
 * This class is inefficient because it serializes the 
 * object to string and then deserializes it back. Unfortunately,
 * this is the only simple way to bridge Jackson to org.json.
 * This conversion should be removed when (if ?) we migrate OpenRefine
 * a better JSON library.
 * 
 * @author antonin
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class JacksonJsonizable implements Jsonizable {

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            writer.value(new JSONObject(mapper.writeValueAsString(this)));
        } catch (JsonProcessingException e) {
            throw new JSONException(e.toString());
        }
    }
    
    public static <T> T fromJSONClass(JSONObject obj, Class<T> klass) throws JSONException {
        ObjectMapper mapper = new ObjectMapper();
        String json = obj.toString();
        try {
            return mapper.readValue(json, klass);
        } catch (JsonParseException e) {
            e.printStackTrace();
            throw new JSONException(e.toString());
        } catch (JsonMappingException e) {
            e.printStackTrace();
            throw new JSONException(e.toString());
        } catch (IOException e) {
            throw new JSONException(e.toString());
        }  
    }

}
