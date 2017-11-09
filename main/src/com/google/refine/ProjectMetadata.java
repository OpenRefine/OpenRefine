/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
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
    private Date written = null;
    private String         _name = "";
    private String         _password = "";

    private String         _encoding = "";
    private int            _encodingConfidence;
    
    private String _creator = "";
    private String _contributors = "";
    private String _subject = "";    // Several refine projects may be linked
    private String _description = "";                // free form of comment
    private int _rowNumber;      // at the creation. Essential for cleaning old projects too heavy
    
    // import options is an array for 1-n data sources
    private JSONArray _importOptionMetadata = new JSONArray();
    
    // user metadata
    private JSONArray _userMetadata = new JSONArray();; 
    
    private Map<String, Serializable>   _customMetadata = new HashMap<String, Serializable>();
    private PreferenceStore             _preferenceStore = new PreferenceStore();

    private final static Logger logger = LoggerFactory.getLogger("project_metadata");

    protected ProjectMetadata(Date date) {
        _created = date;
        preparePreferenceStore(_preferenceStore);
    }

    public ProjectMetadata() {
        this(new Date());
        _modified = _created;
    }

    public ProjectMetadata(Date created, Date modified, String name) {
        this(created);
        _modified = modified;
        _name = name;
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {

        writer.object();
        writer.key("name"); writer.value(_name);
        writer.key("created"); writer.value(ParsingUtilities.dateToString(_created));
        writer.key("modified"); writer.value(ParsingUtilities.dateToString(_modified));
        writer.key("creator"); writer.value(_creator);
        writer.key("contributors"); writer.value(_contributors);
        writer.key("subject"); writer.value(_subject);
        writer.key("description"); writer.value(_description);
        writer.key("rowNumber"); writer.value(_rowNumber);
        
        writer.key("customMetadata"); writer.object();
        for (String key : _customMetadata.keySet()) {
            Serializable value = _customMetadata.get(key);
            writer.key(key);
            writer.value(value);
        }
        writer.endObject();
        
        // write JSONArray directly 
        if (_importOptionMetadata.length() > 0 ) {
            writer.key("importOptionMetadata"); 
            writer.value(_importOptionMetadata);
        }
        
        // write user metadata in {name, value, display} form:
        if (_userMetadata.length() > 0) {
            writer.key(PreferenceStore.USER_METADATA_KEY);
            writer.value(_userMetadata);
        }
        
        if (isSaveMode(options)) {
            writer.key("password"); writer.value(_password);

            writer.key("encoding"); writer.value(_encoding);
            writer.key("encodingConfidence"); writer.value(_encodingConfidence);

            writer.key("preferences"); _preferenceStore.write(writer, options);
        }
        
        writer.endObject();
        
        if (isSaveMode(options)) {
            written = new Date();
        }
    }
    
    public void writeWithoutOption(JSONWriter writer)
            throws JSONException {
        write(writer, 
                new Properties());
    }
    
    private boolean isSaveMode(Properties options) {
        return "save".equals(options.getProperty("mode"));
    }

    public boolean isDirty() {
        return written == null || _modified.after(written);
    }
    
    public void write(JSONWriter jsonWriter) throws JSONException  {
        write(jsonWriter, false);
    }
    
    /**
     * @param jsonWriter writer to save metadatea to
     * @param onlyIfDirty true to not write unchanged metadata
     * @throws JSONException
     */
    public void write(JSONWriter jsonWriter, boolean onlyIfDirty) throws JSONException  {
        if (!onlyIfDirty || isDirty()) {
            Properties options = new Properties();
            options.setProperty("mode", "save");

            write(jsonWriter, options);
        }
    }
    
    static public ProjectMetadata loadFromJSON(JSONObject obj) {
        // TODO: Is this correct?  It's using modified date for creation date
        ProjectMetadata pm = new ProjectMetadata(JSONUtilities.getDate(obj, "modified", new Date()));

        pm._modified = JSONUtilities.getDate(obj, "modified", new Date());
        pm._name = JSONUtilities.getString(obj, "name", "<Error recovering project name>");
        pm._password = JSONUtilities.getString(obj, "password", "");

        pm._encoding = JSONUtilities.getString(obj, "encoding", "");
        pm._encodingConfidence = JSONUtilities.getInt(obj, "encodingConfidence", 0);
        
        pm._creator = JSONUtilities.getString(obj, "creator", "");
        pm._contributors = JSONUtilities.getString(obj, "contributors", "");
        pm._subject = JSONUtilities.getString(obj, "subject", "");
        pm._description = JSONUtilities.getString(obj, "description", "");
        pm._rowNumber = JSONUtilities.getInt(obj, "rowNumber", 0);
        
        if (obj.has("preferences") && !obj.isNull("preferences")) {
            try {
                pm._preferenceStore.load(obj.getJSONObject("preferences"));
            } catch (JSONException e) {
                logger.error(ExceptionUtils.getFullStackTrace(e));
            }
        }
        
        if (obj.has("expressions") && !obj.isNull("expressions")) { // backward compatibility
            try {
                ((TopList) pm._preferenceStore.get("scripting.expressions"))
                    .load(obj.getJSONArray("expressions"));
            } catch (JSONException e) {
                logger.error(ExceptionUtils.getFullStackTrace(e));
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
                logger.error(ExceptionUtils.getFullStackTrace(e));
            }
        }
        
        if (obj.has("importOptionMetadata") && !obj.isNull("importOptionMetadata")) {
            try {
                JSONArray jsonArray = obj.getJSONArray("importOptionMetadata");
                pm._importOptionMetadata = jsonArray;
            } catch (JSONException e) {
                logger.error(ExceptionUtils.getFullStackTrace(e));
            }
        }
        
        if (obj.has(PreferenceStore.USER_METADATA_KEY) && !obj.isNull(PreferenceStore.USER_METADATA_KEY)) {
            try {
                JSONArray jsonArray = obj.getJSONArray(PreferenceStore.USER_METADATA_KEY);
                pm._userMetadata = jsonArray;
            } catch (JSONException e) {
                logger.error(ExceptionUtils.getFullStackTrace(e));
            }
        } 
        
        pm.written = new Date(); // Mark it as not needing writing until modified
        
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
        updateModified();
    }

    public String getName() {
        return _name;
    }

    public void setEncoding(String encoding) {
        this._encoding = encoding;
        updateModified();
    }

    public String getEncoding() {
        return _encoding;
    }

    public void setEncodingConfidence(int confidence) {
        this._encodingConfidence = confidence;
        updateModified();
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
        updateModified();
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
        updateModified();
    }
    
    public JSONArray getImportOptionMetadata() {
        return _importOptionMetadata;
    }
    
    public void setImportOptionMetadata(JSONArray jsonArray) {
        _importOptionMetadata = jsonArray;
        updateModified();
    }
    
    public void appendImportOptionMetadata(JSONObject obj) {
        _importOptionMetadata.put(obj);
        updateModified();
    }

    
    public String getCreator() {
        return _creator;
    }

    
    public void setCreator(String creator) {
        this._creator = creator;
        updateModified();
    }

    
    public String getContributors() {
        return _contributors;
    }

    
    public void setContributors(String contributors) {
        this._contributors = contributors;
        updateModified();
    }

    
    public String getSubject() {
        return _subject;
    }

    
    public void setSubject(String subject) {
        this._subject = subject;
        updateModified();
    }

    
    public String getDescription() {
        return _description;
    }

    
    public void setDescription(String description) {
        this._description = description;
        updateModified();
    }

    
    public int getRowNumber() {
        return _rowNumber;
    }

    
    public void setRowNumber(int rowNumber) {
        this._rowNumber = rowNumber;
        updateModified();
    }

    
    public JSONArray getUserMetadata() {
        return _userMetadata;
    }

    
    public void setUserMetadata(JSONArray userMetadata) {
        this._userMetadata = userMetadata;
    }

    private void updateUserMetadata(String metaName, String valueString)  {
        for (int i = 0; i < _userMetadata.length(); i++) {
            try {
                JSONObject obj = _userMetadata.getJSONObject(i);
                if (obj.getString("name").equals(metaName)) {
                    obj.put("value", valueString);
                }
            } catch (JSONException e) {
                logger.error(ExceptionUtils.getFullStackTrace(e));
            }
        }
    }
    
    public void setAnyField(String metaName, String valueString)  {
        Class<? extends ProjectMetadata> metaClass = this.getClass();
        try {
            Field metaField = metaClass.getDeclaredField("_" + metaName);

            metaField.set(this, valueString);
        } catch (NoSuchFieldException e) {
            updateUserMetadata(metaName, valueString);
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
            logger.error(ExceptionUtils.getFullStackTrace(e));
        }
    }
    
}
