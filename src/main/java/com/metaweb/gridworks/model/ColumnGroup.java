package com.metaweb.gridworks.model;

import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.util.ParsingUtilities;

public class ColumnGroup implements Jsonizable {
    final public int    startColumnIndex;
    final public int    columnSpan;
    final public int    keyColumnIndex; // could be -1 if there is no key cell 
    
    transient public ColumnGroup        parentGroup;
    transient public List<ColumnGroup>  subgroups;
    
    public ColumnGroup(int startColumnIndex, int columnSpan, int keyColumnIndex) {
        this.startColumnIndex = startColumnIndex;
        this.columnSpan = columnSpan;
        this.keyColumnIndex = keyColumnIndex;
        internalInitialize();
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        
        writer.key("startColumnIndex"); writer.value(startColumnIndex);
        writer.key("columnSpan"); writer.value(columnSpan);
        writer.key("keyColumnIndex"); writer.value(keyColumnIndex);
        
        if (!"save".equals(options.get("mode")) && (subgroups != null) && (subgroups.size() > 0)) {
            writer.key("subgroups"); writer.array();
            for (ColumnGroup g : subgroups) {
                g.write(writer, options);
            }
            writer.endArray();
        }
        writer.endObject();
    }
    
    public boolean contains(ColumnGroup g) {
        return (g.startColumnIndex >= startColumnIndex &&
            g.startColumnIndex < startColumnIndex + columnSpan);
    }
    
    public void save(Writer writer) {
        JSONWriter jsonWriter = new JSONWriter(writer);
        try {
            write(jsonWriter, new Properties());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    static public ColumnGroup load(String s) throws Exception {
        JSONObject obj = ParsingUtilities.evaluateJsonStringToObject(s);
        
        return new ColumnGroup(
            obj.getInt("startColumnIndex"),
            obj.getInt("columnSpan"),
            obj.getInt("keyColumnIndex")
        );
    }
    
    protected void internalInitialize() {
        subgroups = new LinkedList<ColumnGroup>();
    }
}
