package com.metaweb.gridworks;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public interface Jsonizable {
    public void write(JSONWriter writer, Properties options) throws JSONException;
}
