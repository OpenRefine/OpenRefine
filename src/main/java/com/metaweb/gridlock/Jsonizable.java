package com.metaweb.gridlock;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public interface Jsonizable {
	public void write(JSONWriter writer, Properties options) throws JSONException;
}
