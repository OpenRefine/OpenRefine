package com.metaweb.gridworks.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class FreebaseTopicNode extends Node {
	private static final long serialVersionUID = 8418548867745587387L;

	final public FreebaseTopic topic;
	
	public FreebaseTopicNode(FreebaseTopic topic) {
		this.topic = topic;
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("nodeType"); writer.value("topic");
		writer.key("topic"); topic.write(writer, options);
		writer.endObject();
	}
}
