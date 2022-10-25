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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.CharMatcher;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.JsonViews;
import com.google.refine.util.ParsingUtilities;

public class ProjectMetadata {

    public final static String DEFAULT_FILE_NAME = "metadata.json";
    public final static String TEMP_FILE_NAME = "metadata.temp.json";
    public final static String OLD_FILE_NAME = "metadata.old.json";

    @JsonProperty("created")
    private final LocalDateTime _created;
    @JsonProperty("modified")
    private LocalDateTime _modified;
    @JsonIgnore
    private LocalDateTime written = null;
    @JsonProperty("name")
    private String _name = "";
    @JsonProperty("password")
    @JsonView(JsonViews.SaveMode.class)
    private String _password = "";

    @JsonProperty("encoding")
    @JsonView(JsonViews.SaveMode.class)
    private String _encoding = "";
    @JsonProperty("encodingConfidence")
    @JsonView(JsonViews.SaveMode.class)
    private int _encodingConfidence;

    @JsonProperty("tags")
    private String[] _tags = new String[0];

    @JsonProperty("creator")
    private String _creator = "";
    @JsonProperty("contributors")
    private String _contributors = "";
    @JsonProperty("subject")
    private String _subject = ""; // Several refine projects may be linked
    @JsonProperty("description")
    private String _description = ""; // free form of comment
    @JsonProperty("rowCount")
    private int _rowCount; // at the creation. Essential for cleaning old projects too heavy

    @JsonProperty("title")
    private String _title = "";
    @JsonProperty("version")
    private String _version = "";
    @JsonProperty("license")
    private String license = "";
    @JsonProperty("homepage")
    private String homepage = "";
    @JsonProperty("image")
    private String image = "";

    // import options is an array for 1-n data sources
    @JsonProperty("importOptionMetadata")
    private ArrayNode _importOptionMetadata = ParsingUtilities.mapper.createArrayNode();

    // user metadata
    @JsonIgnore
    private ArrayNode _userMetadata = ParsingUtilities.mapper.createArrayNode();

    @JsonProperty("customMetadata")
    private Map<String, Object> _customMetadata = new HashMap<>();
    @JsonProperty("preferences")
    @JsonView(JsonViews.SaveMode.class)
    private PreferenceStore _preferenceStore = new PreferenceStore();

    private final static Logger logger = LoggerFactory.getLogger("project_metadata");

    protected ProjectMetadata(LocalDateTime date) {
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

    @JsonIgnore
    public boolean isDirty() {
        return written == null || _modified.isAfter(written);
    }

    static protected void preparePreferenceStore(PreferenceStore ps) {
        ProjectManager.preparePreferenceStore(ps);
        // Any project specific preferences?
    }

    @JsonIgnore
    public LocalDateTime getCreated() {
        return _created;
    }

    @JsonIgnore
    public void setName(String name) {
        this._name = name;
        updateModified();
    }

    @JsonIgnore
    public String getName() {
        return _name;
    }

    @JsonIgnore
    public void setEncoding(String encoding) {
        this._encoding = encoding;
        updateModified();
    }

    @JsonIgnore
    public String getEncoding() {
        return _encoding;
    }

    @JsonIgnore
    public void setEncodingConfidence(int confidence) {
        this._encodingConfidence = confidence;
        updateModified();
    }

    @JsonIgnore
    public void setEncodingConfidence(String confidence) {
        if (confidence != null) {
            this.setEncodingConfidence(Integer.parseInt(confidence));
        }
    }

    @JsonIgnore
    public int getEncodingConfidence() {
        return _encodingConfidence;
    }

    @JsonIgnore
    public void setTags(String[] tags) {
        if (tags != null) {
            List<String> tmpTags = new ArrayList<String>(tags.length);
            for (String tag : tags) {
                if (tag != null) {
                    String trimmedTag = CharMatcher.whitespace().trimFrom(tag);

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

    @JsonIgnore
    public String[] getTags() {
        if (_tags == null) this._tags = new String[0];
        return _tags;
    }

    @JsonIgnore
    public void setPassword(String password) {
        this._password = password;
        updateModified();
    }

    @JsonIgnore
    public String getPassword() {
        return _password;
    }

    @JsonIgnore
    public LocalDateTime getModified() {
        return _modified;
    }

    @JsonIgnore
    public void updateModified() {
        _modified = LocalDateTime.now();
    }

    @JsonIgnore
    public PreferenceStore getPreferenceStore() {
        return _preferenceStore;
    }

    @JsonIgnore
    public Serializable getCustomMetadata(String key) {
        return (Serializable) _customMetadata.get(key);
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

    @JsonIgnore
    public String getCreator() {
        return _creator;
    }

    @JsonIgnore
    public void setCreator(String creator) {
        this._creator = creator;
        updateModified();
    }

    @JsonIgnore
    public String getContributors() {
        return _contributors;
    }

    @JsonIgnore
    public void setContributors(String contributors) {
        this._contributors = contributors;
        updateModified();
    }

    @JsonIgnore
    public String getSubject() {
        return _subject;
    }

    @JsonIgnore
    public void setSubject(String subject) {
        this._subject = subject;
        updateModified();
    }

    @JsonIgnore
    public String getDescription() {
        return _description;
    }

    @JsonIgnore
    public void setDescription(String description) {
        this._description = description;
        updateModified();
    }

    @JsonIgnore
    public int getRowCount() {
        return _rowCount;
    }

    @JsonIgnore
    public void setRowCount(int rowCount) {
        this._rowCount = rowCount;
        updateModified();
    }

    @JsonIgnore
    public ArrayNode getUserMetadata() {
        return _userMetadata;
    }

    @JsonProperty("userMetadata")
    @JsonInclude(Include.NON_NULL)
    public ArrayNode getUserMetadataJson() {
        if (_userMetadata != null && _userMetadata.size() > 0) {
            return _userMetadata;
        }
        return null;
    }

    @JsonIgnore
    public void setUserMetadata(ArrayNode userMetadata) {
        this._userMetadata = userMetadata;
    }

    private void updateUserMetadata(String metaName, String valueString) {
        for (int i = 0; i < _userMetadata.size(); i++) {
            ObjectNode obj = (ObjectNode) _userMetadata.get(i);
            if (obj.get("name").asText("").equals(metaName)) {
                obj.put("value", valueString);
            }
        }
    }

    public void setAnyField(String metaName, String valueString) {
        Class<? extends ProjectMetadata> metaClass = this.getClass();
        try {
            Field metaField = metaClass.getDeclaredField("_" + metaName);
            if (metaName.equals("tags")) {
                metaField.set(this, valueString.split(","));
            } else {
                metaField.set(this, valueString);
            }
        } catch (NoSuchFieldException e) {
            updateUserMetadata(metaName, valueString);
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
            logger.error(ExceptionUtils.getFullStackTrace(e));
        }
    }
}
