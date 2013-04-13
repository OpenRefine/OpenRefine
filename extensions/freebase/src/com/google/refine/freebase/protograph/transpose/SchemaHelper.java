package com.google.refine.freebase.protograph.transpose;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class SchemaHelper {
    static private final String helperURL = "http://openrefine-helper.freebaseapps.com/";
    
    static private class PropertyInfo {
        String fromTypeID;
        String toTypeID;
    }
    
    static private class TypeInfo {
        String[] includedTypes;
    }
    
    protected Map<String, PropertyInfo> properties = new HashMap<String, PropertyInfo>();
    protected Map<String, TypeInfo> types = new HashMap<String, TypeInfo>();
    
    public String getPropertyFromType(String propertyID) {
        ensureProperty(propertyID);
        return properties.get(propertyID).fromTypeID;
    }
    public String getPropertyToType(String propertyID) {
        ensureProperty(propertyID);
        return properties.get(propertyID).toTypeID;
    }
    public String[] getIncludedTypeIDs(String typeID) {
        ensureType(typeID);
        return types.get(typeID).includedTypes;
    }
    
    private void ensureProperty(String propertyID) {
        if (properties.containsKey(propertyID)) {
            return;
        }
        
        PropertyInfo info = new PropertyInfo();
        properties.put(propertyID, info);
        
        JSONObject obj = getJson(helperURL + "get_property_data" + propertyID);
        if (obj != null) {
            try {
                if (!obj.isNull("from")) {
                    info.fromTypeID = obj.getString("from");
                }
            } catch (JSONException e) {
            }
            try {
                if (!obj.isNull("to")) {
                    info.toTypeID = obj.getString("to");
                }
            } catch (JSONException e) {
            }
        }
    }
    
    private void ensureType(String typeID) {
        if (types.containsKey(typeID)) {
            return;
        }
        
        TypeInfo info = new TypeInfo();
        types.put(typeID, info);
        
        JSONObject obj = getJson(helperURL + "get_type_data" + typeID);
        if (obj != null) {
            if (!obj.isNull("includes")) {
                info.includedTypes = JSONUtilities.getStringArray(obj, "includes");
            }
        }
    }
    
    private JSONObject getJson(String urlString) {
        try {
            URL url = new URL(urlString);
            InputStream is = url.openStream();
            try {
                String s = ParsingUtilities.inputStreamToString(is);
                return ParsingUtilities.evaluateJsonStringToObject(s);
            } finally {
                is.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
