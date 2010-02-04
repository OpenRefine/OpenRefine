package com.metaweb.gridworks.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class TopicNode extends Node {
	private static final long serialVersionUID = 8418548867745587387L;

	final protected FreebaseTopic _topic;
	
	public TopicNode(FreebaseTopic topic) {
		_topic = topic;
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub
		
	}
}
