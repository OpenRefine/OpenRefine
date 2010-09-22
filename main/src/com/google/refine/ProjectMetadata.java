package com.google.refine;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.preference.PreferenceStore;
import com.google.refine.preference.TopList;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class ProjectMetadata implements Jsonizable {
    private final Date     _created;
    private Date           _modified;
    private String         _name;
    private String         _password;

    private String         _encoding;
    private int            _encodingConfidence;
    
    private Map<String, Serializable>   _customMetadata = new HashMap<String, Serializable>();
    private PreferenceStore             _preferenceStore = new PreferenceStore();

    final Logger logger = LoggerFactory.getLogger("project_metadata");

    protected ProjectMetadata(Date date) {
        _created = date;
        preparePreferenceStore(_preferenceStore);
    }

    public ProjectMetadata() {
        _created = new Date();
        _modified = _created;
        preparePreferenceStore(_preferenceStore);
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {

        writer.object();
        writer.key("name"); writer.value(_name);
        writer.key("created"); writer.value(ParsingUtilities.dateToString(_created));
        writer.key("modified"); writer.value(ParsingUtilities.dateToString(_modified));

        if ("save".equals(options.getProperty("mode"))) {
            writer.key("password"); writer.value(_password);

            writer.key("encoding"); writer.value(_encoding);
            writer.key("encodingConfidence"); writer.value(_encodingConfidence);
            
            writer.key("customMetadata"); writer.object();
            for (String key : _customMetadata.keySet()) {
                Serializable value = _customMetadata.get(key);
                writer.key(key);
                writer.value(value);
            }
            writer.endObject();
            
            writer.key("preferences"); _preferenceStore.write(writer, options);
        }
        writer.endObject();
    }
    
    public void write(JSONWriter jsonWriter) throws Exception {
        Properties options = new Properties();
        options.setProperty("mode", "save");

        write(jsonWriter, options);
    }
    
    static public ProjectMetadata loadFromJSON(JSONObject obj) {
        ProjectMetadata pm = new ProjectMetadata(JSONUtilities.getDate(obj, "modified", new Date()));

        pm._modified = JSONUtilities.getDate(obj, "modified", new Date());
        pm._name = JSONUtilities.getString(obj, "name", "<Error recovering project name>");
        pm._password = JSONUtilities.getString(obj, "password", "");

        pm._encoding = JSONUtilities.getString(obj, "encoding", "");
        pm._encodingConfidence = JSONUtilities.getInt(obj, "encodingConfidence", 0);

        if (obj.has("preferences") && !obj.isNull("preferences")) {
            try {
                pm._preferenceStore.load(obj.getJSONObject("preferences"));
            } catch (JSONException e) {
                // ignore
            }
        }
        
        if (obj.has("expressions") && !obj.isNull("expressions")) { // backward compatibility
            try {
                ((TopList) pm._preferenceStore.get("scripting.expressions"))
                    .load(obj.getJSONArray("expressions"));
            } catch (JSONException e) {
                // ignore
            }
        }
        
        if (obj.has("customMetadata") && !obj.isNull("customMetadata")) {
            try {
                JSONObject obj2 = obj.getJSONObject("customMetadata");
                
                @SuppressWarnings("unchecked")
                Iterator<String> keys = obj2.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = obj2.get(key);
                    if (value != null && value instanceof Serializable) {
                        pm._customMetadata.put(key, (Serializable) value);
                    }
                }
            } catch (JSONException e) {
                // ignore
            }
        }
        
        return pm;
    }
    
    static protected void preparePreferenceStore(PreferenceStore ps) {
        ProjectManager.preparePreferenceStore(ps);
        // Any project specific preferences?
    }

    public Date getCreated() {
        return _created;
    }

    public void setName(String name) {
        this._name = name;
    }

    public String getName() {
        return _name;
    }

    public void setEncoding(String encoding) {
        this._encoding = encoding;
    }

    public String getEncoding() {
        return _encoding;
    }

    public void setEncodingConfidence(int confidence) {
        this._encodingConfidence = confidence;
    }

    public void setEncodingConfidence(String confidence) {
        if (confidence != null) {
            this.setEncodingConfidence(Integer.parseInt(confidence));
        }
    }

    public int getEncodingConfidence() {
        return _encodingConfidence;
    }

    public void setPassword(String password) {
        this._password = password;
    }

    public String getPassword() {
        return _password;
    }

    public Date getModified() {
        return _modified;
    }

    public void updateModified() {
        _modified = new Date();
    }

    public PreferenceStore getPreferenceStore() {
        return _preferenceStore;
    }
    
    public Serializable getCustomMetadata(String key) {
        return _customMetadata.get(key);
    }
    
    public void setCustomMetadata(String key, Serializable value) {
        if (value == null) {
            _customMetadata.remove(key);
        } else {
            _customMetadata.put(key, value);
        }
    }
}
