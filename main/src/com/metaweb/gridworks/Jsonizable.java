package com.metaweb.gridworks;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

/**
 * Interface for streaming out JSON, either into HTTP responses or 
 * serialization files.
 * 
 * @author dfhuynh
 */
public interface Jsonizable {
    public void write(JSONWriter writer, Properties options) throws JSONException;
}
