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

package com.google.refine.model.metadata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.preference.TopList;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class ProjectMetadata  extends AbstractMetadata {
    final public static String DEFAULT_FILE_NAME = "metadata.json";
    final public static String TEMP_FILE_NAME = "metadata.temp.json";
    final public static String OLD_FILE_NAME = "metadata.old.json";
    
    private final LocalDateTime     _created;
    private String         _name = "";
    private String         _password = "";

    private String _encoding = "";
    private int _encodingConfidence;
    private int _rowCount;
    // user metadata
    private JSONArray _userMetadata = new JSONArray();; 
    
    // _tags maps to keywords of the data package metadata
    private String[] _tags = new String[0];
    private String _creator = "";
    private String _contributors = "";
    private String _subject = "";    // Several refine projects may be linked
    private String _description = "";                // free form of comment
    
    // import options is an array for 1-n data sources
    private JSONArray _importOptionMetadata = new JSONArray();
    
    
    private Map<String, Serializable>   _customMetadata = new HashMap<String, Serializable>();
    private PreferenceStore             _preferenceStore = new PreferenceStore();
    
    // below 5 fields are from data package metadata:
    private String title = "";
    private String homepage;
    private String image = "";
    private String license = "";
    private String version = "";

    private final static Logger logger = LoggerFactory.getLogger("project_metadata");

    protected ProjectMetadata(LocalDateTime date) {
        setFormatName(MetadataFormat.PROJECT_METADATA);
        _created = date;
        preparePreferenceStore(_preferenceStore);
    }

    public ProjectMetadata() {
        this(LocalDateTime.now());
        _modified = _created;
    }

    public ProjectMetadata(LocalDateTime created, LocalDateTime modified, String name) {
        this(created);
        _modified = modified;
        _name = name;
    }
    
    public void setRowCount(int rowCount) {
        this._rowCount = rowCount;
        updateModified();
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {

        writer.object();
        writer.key("name");
        writer.value(_name);
        writer.key("tags");
        writer.array();
        for (String tag : _tags) {
            writer.value(tag);
        }
        writer.endArray();
        writer.key("created"); writer.value(ParsingUtilities.localDateToString(_created));
        writer.key("modified"); writer.value(ParsingUtilities.localDateToString(_modified));
        writer.key("creator"); writer.value(_creator);
        writer.key("contributors"); writer.value(_contributors);
        writer.key("subject"); writer.value(_subject);
        writer.key("description"); writer.value(_description);
        writer.key("rowCount"); writer.value(_rowCount);
        writer.key("title"); writer.value(title);
        writer.key("homepage"); writer.value(homepage);
        writer.key("image"); writer.value(image);
        writer.key("license"); writer.value(license);
        writer.key("version"); writer.value(version);

        writer.key("customMetadata");
        writer.object();
        
        for (String key : _customMetadata.keySet()) {
            Serializable value = _customMetadata.get(key);
            writer.key(key);
            writer.value(value);
        }
        writer.endObject();

        // write JSONArray directly
        if (_importOptionMetadata.length() > 0) {
            writer.key("importOptionMetadata");
            writer.value(_importOptionMetadata);
        }

        // write user metadata in {name, value, display} form:
        if (_userMetadata.length() > 0) {
            writer.key(PreferenceStore.USER_METADATA_KEY);
            writer.value(_userMetadata);
        }

        if (isSaveMode(options)) {
            writer.key("password");
            writer.value(_password);

            writer.key("encoding");
            writer.value(_encoding);
            writer.key("encodingConfidence");
            writer.value(_encodingConfidence);

            writer.key("preferences");
            _preferenceStore.write(writer, options);
        }

        writer.endObject();

        if (isSaveMode(options)) {
            written = LocalDateTime.now();
        }
    }

    public void writeWithoutOption(JSONWriter writer)
            throws JSONException {
        write(writer, new Properties());
    }

    private boolean isSaveMode(Properties options) {
        return "save".equals(options.getProperty("mode"));
    }

    public void write(JSONWriter jsonWriter)
            throws JSONException {
        write(jsonWriter, false);
    }

     public void loadFromJSON(JSONObject obj) {
        extractModifiedLocalTime(obj);

        this._name = JSONUtilities.getString(obj, "name", "<Error recovering project name>");
        this._password = JSONUtilities.getString(obj, "password", "");

        this._encoding = JSONUtilities.getString(obj, "encoding", "");
        this._encodingConfidence = JSONUtilities.getInt(obj, "encodingConfidence", 0);

        this._creator = JSONUtilities.getString(obj, "creator", "");
        this._contributors = JSONUtilities.getString(obj, "contributors", "");
        this._subject = JSONUtilities.getString(obj, "subject", "");
        this._description = JSONUtilities.getString(obj, "description", "");
        this._rowCount = JSONUtilities.getInt(obj, "rowCount", 0);
        
        this.title = JSONUtilities.getString(obj, "title", "");
        this.homepage = JSONUtilities.getString(obj, "homepage", "");
        this.image = JSONUtilities.getString(obj, "image", "");
        this.license = JSONUtilities.getString(obj, "license", "");
        this.version = JSONUtilities.getString(obj, "version", "");

        this._tags = JSONUtilities.getStringArray(obj, "tags");

        if (obj.has("preferences") && !obj.isNull("preferences")) {
            try {
                this._preferenceStore.load(obj.getJSONObject("preferences"));
            } catch (JSONException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }

        if (obj.has("expressions") && !obj.isNull("expressions")) { // backward compatibility
            try {
                ((TopList) this._preferenceStore.get("scripting.expressions")).load(obj.getJSONArray("expressions"));
            } catch (JSONException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }

        if (obj.has("customMetadata") && !obj.isNull("customMetadata")) {
            try {
                JSONObject obj2 = obj.getJSONObject("customMetadata");

                Iterator<String> keys = obj2.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = obj2.get(key);
                    if (value != null && value instanceof Serializable) {
                        this._customMetadata.put(key, (Serializable) value);
                    }
                }
            } catch (JSONException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }

        if (obj.has("importOptionMetadata") && !obj.isNull("importOptionMetadata")) {
            try {
                JSONArray jsonArray = obj.getJSONArray("importOptionMetadata");
                this._importOptionMetadata = jsonArray;
            } catch (JSONException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }

        if (obj.has(PreferenceStore.USER_METADATA_KEY) && !obj.isNull(PreferenceStore.USER_METADATA_KEY)) {
            try {
                JSONArray jsonArray = obj.getJSONArray(PreferenceStore.USER_METADATA_KEY);
                this._userMetadata = jsonArray;
            } catch (JSONException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        } 
        
        this.written = LocalDateTime.now(); // Mark it as not needing writing until modified
        
    }

    private void extractModifiedLocalTime(JSONObject obj) {
        String modified = JSONUtilities.getString(obj, "modified", LocalDateTime.now().toString());
        
        this._modified = ParsingUtilities.stringToLocalDate(modified);
    }

    static protected void preparePreferenceStore(PreferenceStore ps) {
        ProjectManager.preparePreferenceStore(ps);
        // Any project specific preferences?
    }

    public LocalDateTime getCreated() {
        return _created;
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

    public void setTags(String[] tags) {
        if (tags != null) {
            List<String> tmpTags = new ArrayList<String>(tags.length);
            for (String tag : tags) {
                if (tag != null) {
                    String trimmedTag = tag.trim();

                    if (!trimmedTag.isEmpty()) {
                        tmpTags.add(trimmedTag);
                    }
                }
            }
            this._tags = tmpTags.toArray(new String[tmpTags.size()]);
        } else {
            this._tags = tags;
        }

        updateModified();
    }

    public void appendTags(String[] tags) {
        String[] mergedTags = (String[])ArrayUtils.addAll(this._tags, tags);
        setTags(mergedTags);
    }
    
    public String[] getTags() {
        if (_tags == null) this._tags = new String[0];
        return _tags;
    }

    public void setPassword(String password) {
        this._password = password;
        updateModified();
    }

    public String getPassword() {
        return _password;
    }

    public  LocalDateTime getModified() {
        return _modified;
    }

    public void updateModified() {
        _modified = LocalDateTime.now();
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
    
    public String getEncoding() {
        return _encoding;
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
    
    
    public String getTitle() {
        return title;
    }

    
    public void setTitle(String title) {
        this.title = title;
        updateModified();
    }

    
    public String getHomepage() {
        return homepage;
    }

    
    public void setHomepage(String homepage) {
        this.homepage = homepage;
        updateModified();
    }

    
    public String getImage() {
        return image;
    }

    
    public void setImage(String image) {
        this.image = image;
        updateModified();
    }

    
    public String getLicense() {
        return license;
    }

    
    public void setLicense(String license) {
        this.license = license;
        updateModified();
    }
    
    public String getVersion() {
        return version;
    }

    
    public void setVersion(String version) {
        this.version = version;
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
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }
    
    public void setAnyStringField(String metaName, String valueString)  {
        if (propertyExists(this, metaName)) {
            try {
                if (metaName.equals("tags")) {
                    BeanUtils.setProperty(this, metaName, valueString.split(","));
                } else
                    BeanUtils.setProperty(this, metaName, valueString);
            } catch (IllegalAccessException | InvocationTargetException ite) {
                logger.error(ExceptionUtils.getStackTrace(ite));
            }
        } else {
            updateUserMetadata(metaName, valueString);
        }
    }
    
    @Override
    public void loadFromFile(File metadataFile) {
        InputStream targetStream = null;
        try {
            targetStream = FileUtils.openInputStream(metadataFile);
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
        loadFromStream(targetStream);
    }

    @Override
    public void writeToFile(File metadataFile) {
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(metadataFile));

            JSONWriter jsonWriter = new JSONWriter(writer);
            write(jsonWriter, false);
        } catch (FileNotFoundException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    @Override
    public void loadFromStream(InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject obj = (JSONObject) tokener.nextValue();

            this.loadFromJSON(obj);
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public JSONObject getJSON() {
        StringWriter writer = new StringWriter();
        JSONWriter jsonWriter = new JSONWriter(writer);
        writeWithoutOption(jsonWriter);
        
        return new JSONObject(jsonWriter.toString());
    }

    @Override
    public List<Exception> validate() {
        return null;
    }
    
}
