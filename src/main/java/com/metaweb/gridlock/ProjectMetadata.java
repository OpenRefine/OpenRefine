package com.metaweb.gridlock;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class ProjectMetadata implements Serializable, Jsonizable {
	private static final long serialVersionUID = 7959027046468240844L;
	
	public String 	name;
	public String 	password;
	public Date 	created = new Date();
	public Date 	modified = new Date();
	
	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
		
		writer.object();
		writer.key("name"); writer.value(name);
		writer.key("created"); writer.value(sdf.format(created));
		writer.key("modified"); writer.value(sdf.format(modified));
		writer.endObject();
	}
}
