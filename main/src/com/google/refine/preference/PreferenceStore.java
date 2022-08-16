/*

Copyright 2010,2012 Google Inc.
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

package com.google.refine.preference;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.util.ParsingUtilities;

public class PreferenceStore {

    public static final String USER_METADATA_KEY = "userMetadata";

    private boolean dirty = false;
    protected Map<String, Object> _prefs = new HashMap<>();

    public void put(String key, Object value) {
        if (value == null) {
            _prefs.remove(key);
        } else {
            _prefs.put(key, value);
        }
        dirty = true;
    }

    public Object get(String key) {
        return _prefs.get(key);
    }

    @JsonIgnore
    public Set<String> getKeys() {
        return _prefs.keySet();
    }

    /**
     * @return true if the preference store has unsaved changes
     */
    @JsonIgnore
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Mark the object as clean every time it is serialized. This behaviour is not very clean - it is inherited from the
     * previous deserialization code.
     * 
     * @return
     */
    @JsonProperty("makeClean")
    @JsonInclude(Include.NON_NULL)
    public Integer markAsClean() {
        dirty = false;
        return null;
    }

    @JsonProperty("entries")
    public void setEntries(JsonNode entries) {
        Iterator<String> i = entries.fieldNames();
        while (i.hasNext()) {
            String key = i.next();
            if (entries.get(key) != null) {
                JsonNode o = entries.get(key);
                Object loaded = loadObject(o);
                if (loaded == null) {
                    if ("scripting.starred-expressions".contentEquals(key)) {
                        // HACK to work around preferences corruption
                        loaded = new TopList(10);
                    }
                }
                _prefs.put(key, loaded);
            }
        }
        dirty = false; // internal puts don't count
    }

    @JsonProperty("entries")
    public Map<String, Object> getEntries() {
        return _prefs;
    }

    static public Object loadObject(JsonNode o) {
        try {
            if (o instanceof ObjectNode) {
                ObjectNode obj2 = (ObjectNode) o;
                return ParsingUtilities.mapper.treeToValue(obj2, PreferenceValue.class);
            } else if (o instanceof ArrayNode) {
                return o;
            } else {
                // basic datatypes (int, double, boolean, string)
                return ParsingUtilities.mapper.treeToValue(o, Object.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
