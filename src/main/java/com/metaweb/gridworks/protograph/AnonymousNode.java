package com.metaweb.gridworks.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class AnonymousNode extends Node {
	private static final long serialVersionUID = -6956243664838720646L;
	
	final public FreebaseType type;
	
	public AnonymousNode(FreebaseType type) {
		this.type = type;
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub

	}

}
