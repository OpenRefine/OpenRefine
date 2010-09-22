package com.google.refine.preference;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections.list.UnmodifiableList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;


public class TopList implements Jsonizable {
    private static final long serialVersionUID = 2666669643063493350L;
    
    final protected int          _top;
    final protected List<String> _list = new ArrayList<String>();

    public TopList(int top) {
        _top = top;
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getList() {
        return (List<String>) UnmodifiableList.decorate(_list);
    }
    
    public void add(String element) {
        _list.remove(element);
        _list.add(0, element);
        while (_list.size() > _top) {
            _list.remove(_list.size() - 1);
        }
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
}
