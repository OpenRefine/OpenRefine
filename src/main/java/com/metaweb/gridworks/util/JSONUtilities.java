package com.metaweb.gridworks.util;

import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

public class JSONUtilities {
    static public String getString(JSONObject obj, String key, String def) {
        try {
            return obj.getString(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public int getInt(JSONObject obj, String key, int def) {
        try {
            return obj.getInt(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public boolean getBoolean(JSONObject obj, String key, boolean def) {
        try {
            return obj.getBoolean(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public double getDouble(JSONObject obj, String key, double def) {
        try {
            return obj.getDouble(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public long getLong(JSONObject obj, String key, long def) {
        try {
            return obj.getLong(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public Date getDate(JSONObject obj, String key, Date def) {
        try {
            Date d = ParsingUtilities.stringToDate(obj.getString(key));
            
            return d != null ? d : def;
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public int[] getIntArray(JSONObject obj, String key) {
        try {
            JSONArray a = obj.getJSONArray(key);
            int[] r = new int[a.length()];
            
            for (int i = 0; i < r.length; i++) {
                r[i] = a.getInt(i);
            }
            
            return r;
        } catch (JSONException e) {
            return new int[0];
        }
    }
    
    static public String[] getStringArray(JSONObject obj, String key) {
        try {
            JSONArray a = obj.getJSONArray(key);
            String[] r = new String[a.length()];
            
            for (int i = 0; i < r.length; i++) {
                r[i] = a.getString(i);
            }
            
            return r;
        } catch (JSONException e) {
            return new String[0];
        }
    }
    
    static public void getStringList(JSONObject obj, String key, List<String> list) {
        try {
            JSONArray a = obj.getJSONArray(key);
            int count = a.length();
            
            for (int i = 0; i < count; i++) {
                list.add(a.getString(i));
            }
        } catch (JSONException e) {
        }
    }
    
    static public void writeStringList(JSONWriter writer, List<String> list) throws JSONException {
        writer.array();
        for (String s : list) {
            writer.value(s);
        }
        writer.endArray();
    }
}
