package com.metaweb.gridworks.preference;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class PreferenceStore implements Jsonizable {
    protected Map<String, Object> _prefs = new HashMap<String, Object>();
    
    public void put(String key, Object value) {
        _prefs.put(key, value);
    }
    
    public Object get(String key) {
        return _prefs.get(key);
    }
    
    @Override
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        
        writer.key("entries");
            writer.object();
            for (String k : _prefs.keySet()) {
                writer.key(k);
                
                Object o = _prefs.get(k);
                if (o instanceof Jsonizable) {
                    ((Jsonizable) o).write(writer, options);
                } else {
                    writer.value(o);
                }
            }
            writer.endObject();
        
        writer.endObject();
    }
    
    @SuppressWarnings("unchecked")
    public void load(JSONObject obj) throws JSONException {
        if (obj.has("entries") && !obj.isNull("entries")) {
            JSONObject entries = obj.getJSONObject("entries");
            
            Iterator<String> i = entries.keys();
            while (i.hasNext()) {
                String key = i.next();
                Object o = entries.get(key);
                _prefs.put(key, loadObject(o));
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    static public Object loadObject(Object o) {
        if (o instanceof JSONObject) {
            try {
                JSONObject obj2 = (JSONObject) o;
                String className = obj2.getString("class");
                Class klass = Class.forName(className);
                Method method = klass.getMethod("load", JSONObject.class);
                
                return method.invoke(null, obj2);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return o;
        }
    }
}
