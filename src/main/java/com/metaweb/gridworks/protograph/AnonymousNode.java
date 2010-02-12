package com.metaweb.gridworks.protograph;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class AnonymousNode implements Node, NodeWithLinks {
	private static final long serialVersionUID = -6956243664838720646L;
	
	final public FreebaseType type;
	final public List<Link> 	links = new LinkedList<Link>();
	
	public AnonymousNode(FreebaseType type) {
		this.type = type;
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("nodeType"); writer.value("anonymous");
		writer.key("type"); type.write(writer, options);
		if (links != null) {
			writer.key("links"); writer.array();
			for (Link link : links) {
				link.write(writer, options);
			}
			writer.endArray();
		}
		writer.endObject();
	}

	public void addLink(Link link) {
		links.add(link);
	}
}
