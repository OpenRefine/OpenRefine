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

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.ParsingUtilities;

public class ProjectMetadata {
    public final static String DEFAULT_FILE_NAME = "metadata.json";
    public final static String TEMP_FILE_NAME = "metadata.temp.json";
    public final static String OLD_FILE_NAME = "metadata.old.json";
    
    private final LocalDateTime     _created;
    private LocalDateTime           _modified;
    private LocalDateTime written = null;
    private String         _name = "";
    private String         _password = "";

    private String _encoding = "";
    private int _encodingConfidence;

    private String[] _tags = new String[0];

    private String _creator = "";
    private String _contributors = "";
    private String _subject = ""; // Several refine projects may be linked
    private String _description = ""; // free form of comment
    private int _rowCount; // at the creation. Essential for cleaning old projects too heavy

    // import options is an array for 1-n data sources
    private JSONArray _importOptionMetadata = new JSONArray();

    // user metadata
    private JSONArray _userMetadata = new JSONArray();
    
    private Map<String, Serializable>   _customMetadata = new HashMap<String, Serializable>();
    private PreferenceStore             _preferenceStore = new PreferenceStore();

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

    private boolean isSaveMode(Properties options) {
        return "save".equals(options.getProperty("mode"));
    }

    public boolean isDirty() {
        return written == null || _modified.isAfter(written);
    }

    static protected void preparePreferenceStore(PreferenceStore ps) {
        ProjectManager.preparePreferenceStore(ps);
        // Any project specific preferences?
    }

    public LocalDateTime getCreated() {
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

    public void appendImportOptionMetadata(ObjectNode options) {
        _importOptionMetadata.put(options);
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

    public int getRowCount() {
        return _rowCount;
    }

    public void setRowCount(int rowCount) {
        this._rowCount = rowCount;
        updateModified();
    }

    public JSONArray getUserMetadata() {
        return _userMetadata;
    }

    public void setUserMetadata(JSONArray userMetadata) {
        this._userMetadata = userMetadata;
    }

    private void updateUserMetadata(String metaName, String valueString) {
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

	public static ProjectMetadata loadFromStream(InputStream f) throws IOException {
		return ParsingUtilities.mapper.readValue(f, ProjectMetadata.class);
	}

}
