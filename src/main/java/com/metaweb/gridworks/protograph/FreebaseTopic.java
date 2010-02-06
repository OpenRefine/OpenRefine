package com.metaweb.gridworks.protograph;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class FreebaseTopic implements Serializable, Jsonizable {
	private static final long serialVersionUID = -3427885694129112432L;
	
	final public String id;
	final public String name;
	
	public FreebaseTopic(String id, String name) {
		this.id = id;
		this.name = name;
	}
	
	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub

	}

}
