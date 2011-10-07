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

package com.google.refine.preference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections.list.UnmodifiableList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;

public class TopList implements Jsonizable, Iterable<String> {
    
    final protected int          _top;
    final protected List<String> _list = new ArrayList<String>();

    public TopList(int top) {
        _top = top;
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getList() {
        return UnmodifiableList.decorate(_list);
    }
    
    public void add(String element) {
        _list.remove(element);
        _list.add(0, element);
        while (_list.size() > _top) {
            _list.remove(_list.size() - 1);
        }
    }
    
    public void remove(String element)
    {
        _list.remove(element);
    }

    @Override
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        writer.key("class"); writer.value(this.getClass().getName());
        
        writer.key("top"); writer.value(_top);
        writer.key("list");
        writer.array();
        for (String element : _list) {
            writer.value(element);
        }
        writer.endArray();
        writer.endObject();
    }
    
    static public TopList load(JSONObject obj) throws JSONException {
        int top = obj.has("top") && !obj.isNull("top") ? obj.getInt("top") : 10;
        TopList tl = new TopList(top);
        
        if (obj.has("list") && !obj.isNull("list")) {
            JSONArray a = obj.getJSONArray("list");
            
            tl.load(a);
        }
        return tl;
    }
    
    public void load(JSONArray a) throws JSONException {
        int length = a.length();
        for (int i = 0; i < length && _list.size() < _top; i++) {
            _list.add(a.getString(i));
        }
    }
    
    @Override  
    public Iterator<String> iterator() {  
        return _list.iterator();  
    } 
}
