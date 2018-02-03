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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.RefineServlet;

public class PreferenceStore implements Jsonizable {
    public static final String USER_METADATA_KEY = "userMetadata";
    
    // use to populate "creator" filed in metadata. https://github.com/OpenRefine/OpenRefine/issues/1393
    public static final String USER_NAME = "username";
    
    private boolean dirty = false;
    protected Map<String, Object> _prefs = new HashMap<String, Object>();
    
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
    
    public Set<String> getKeys() {
        return _prefs.keySet();
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
        dirty = false;
    }
    
    /**
     * @return true if the preference store has unsaved changes
     */
    public boolean isDirty() {
        return dirty;
    }
    
    @SuppressWarnings("unchecked")
    public void load(JSONObject obj) throws JSONException {
        if (obj.has("entries") && !obj.isNull("entries")) {
            JSONObject entries = obj.getJSONObject("entries");
            
            Iterator<String> i = entries.keys();
            while (i.hasNext()) {
                String key = i.next();
                if (!entries.isNull(key)) {
                    Object o = entries.get(key);
                    _prefs.put(key, loadObject(o));
                }
            }
            dirty = false; // internal puts don't count
        }
    }
    
    static public Object loadObject(Object o) {
        if (o instanceof JSONObject) {
            try {
                JSONObject obj2 = (JSONObject) o;
                String className = obj2.getString("class");
                Class<?> klass = RefineServlet.getClass(className);
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
