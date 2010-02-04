package com.metaweb.gridworks.protograph;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class FreebaseTopic implements Serializable, Jsonizable {
	private static final long serialVersionUID = -3427885694129112432L;
	
	final protected String _id;
	final protected String _name;
	
	public FreebaseTopic(String id, String name) {
		_id = id;
		_name = name;
	}
	
	public String getID() {
		return _id;
	}

	public String getName() {
		return _name;
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub

	}

}
