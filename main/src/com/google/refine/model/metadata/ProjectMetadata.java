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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectManager;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.preference.TopList;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.JsonViews;
import com.google.refine.util.ParsingUtilities;

public class ProjectMetadata  extends AbstractMetadata {
    final public static String DEFAULT_FILE_NAME = "metadata.json";
    final public static String TEMP_FILE_NAME = "metadata.temp.json";
    final public static String OLD_FILE_NAME = "metadata.old.json";
    
    @JsonProperty("created")
    private LocalDateTime     _created;
    @JsonProperty("name")
    private String         _name = "";
    @JsonProperty("password")
    @JsonView(JsonViews.SaveMode.class)
    private String         _password = "";

    @JsonProperty("encoding")
    @JsonView(JsonViews.SaveMode.class)
    private String _encoding = "";
    @JsonProperty("encodingConfidence")
    @JsonView(JsonViews.SaveMode.class)
    private int _encodingConfidence;
    @JsonProperty("rowCount")
    private int _rowCount;
    // user metadata
    @JsonIgnore
    private ArrayNode _userMetadata = ParsingUtilities.mapper.createArrayNode();
    
    // _tags maps to keywords of the data package metadata
    @JsonProperty("tags")
    private String[] _tags = new String[0];
    @JsonProperty("creator")
    private String _creator = "";
    @JsonProperty("contributors")
    private String _contributors = "";
    @JsonProperty("subject")
    private String _subject = "";    // Several refine projects may be linked
    @JsonProperty("description")
    private String _description = "";                // free form of comment
    
    // import options is an array for 1-n data sources
    @JsonIgnore
    private ArrayNode _importOptionMetadata = ParsingUtilities.mapper.createArrayNode();
    
    @JsonProperty("customMetadata")
    private Map<String, Serializable>   _customMetadata = new HashMap<String, Serializable>();
    @JsonProperty("preferences")
    @JsonView(JsonViews.SaveMode.class)
    private PreferenceStore             _preferenceStore = new PreferenceStore();
    
    // below 5 fields are from data package metadata:
    @JsonProperty("title")
    private String title = "";
    @JsonProperty("homepage")
    private String homepage;
    @JsonProperty("image")
    private String image = "";
    @JsonProperty("license")
    private String license = "";
    @JsonProperty("version")
    private String version = "";
    
    @JsonProperty(PreferenceStore.USER_METADATA_KEY)
    @JsonInclude(Include.NON_NULL)
    public ArrayNode getJsonUserMetadata() {
        if (_userMetadata.size() > 0) {
            return _userMetadata;
        }
        return null;
    }
    
    @JsonProperty(PreferenceStore.USER_METADATA_KEY)
    protected void setUserMetadataJson(ArrayNode json) {
    	_userMetadata = json;
    }
    
    @JsonProperty("importOptionMetadata")
    @JsonInclude(Include.NON_NULL)
    public ArrayNode getJsonImportOptionMetadata() {
        if (_importOptionMetadata.size() > 0) {
            return _importOptionMetadata;
        }
        return null;
    }
    
    @JsonProperty("importOptionMetadata")
    public void setImportOptionMetadataJson(ArrayNode options) {
    	_importOptionMetadata = options;
    	// this field should always be present so we can update the last updated time here
    	this.written = LocalDateTime.now();
    }
    
    // backwards compatibility
    @JsonProperty("expressions")
    protected void setExpressions(TopList expressions) {
    	this._preferenceStore.put("scripting.expressions", expressions);
    }

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
    
    @JsonProperty("saveModeWritten")
    @JsonView(JsonViews.SaveMode.class)
    @JsonInclude(Include.NON_NULL)
    public String setSaveModeWritten() {
        written = LocalDateTime.now();
        return null;
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
    
    @JsonIgnore
    public ArrayNode getImportOptionMetadata() {
        return _importOptionMetadata;
    }
    
    @JsonIgnore
    public void setImportOptionMetadata(ArrayNode jsonArray) {
        _importOptionMetadata = jsonArray;
        updateModified();
    }
    
    public void appendImportOptionMetadata(ObjectNode options) {
        _importOptionMetadata.add(options);
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
    
    public ArrayNode getUserMetadata() {
        return _userMetadata;
    }
    
    public void setUserMetadata(ArrayNode userMetadata) {
        this._userMetadata = userMetadata;
    }
    
    private void updateUserMetadata(String metaName, String valueString)  {
        for (int i = 0; i < _userMetadata.size(); i++) {
            try {
                JsonNode obj = _userMetadata.get(i);
                if (obj.get("name").asText("").equals(metaName)) {
                    ((ObjectNode) obj).put("value", valueString);
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
    public void loadFromStream(InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject obj = (JSONObject) tokener.nextValue();

            this.loadFromJSON(IOUtils.toString(inputStream));
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public List<Exception> validate() {
        return null;
    }
    
}
