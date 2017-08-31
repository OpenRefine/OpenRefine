package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;


public abstract class BiJsonizable implements Jsonizable {
    
    // public static String jsonType;
    public static String jsonType = null;
    public static final String jsonTypeKey = "type";
    
    public abstract void writeFields(JSONWriter writer, Properties options)
            throws JSONException;
    
    @Override
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        writer.key(jsonTypeKey);
        writer.value(getJsonType());
        writeFields(writer, options);
        writer.endObject();
    }
    
    public abstract String getJsonType();
}
