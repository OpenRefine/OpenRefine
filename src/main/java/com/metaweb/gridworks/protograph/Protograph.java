package com.metaweb.gridworks.protograph;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class Protograph implements Serializable, Jsonizable {
	private static final long serialVersionUID = 706700643851582450L;
	
	final protected List<Node> _rootNodes = new LinkedList<Node>();
	
	public int getRootNodeCount() {
		return _rootNodes.size();
	}
	
	public Node getRootNode(int index) {
		return _rootNodes.get(index);
	}
	
	static public Protograph reconstruct(JSONObject o) throws JSONException {
		Protograph g = new Protograph();
		
		JSONArray rootNodes = o.getJSONArray("rootNodes");
		int count = rootNodes.length();
		
		for (int i = 0; i < count; i++) {
			JSONObject o2 = rootNodes.getJSONObject(i);
			Node node = reconstructNode(o2);
			if (node != null) {
				g._rootNodes.add(node);
			}
		}
		
		return g;
	}
	
	static protected Node reconstructNode(JSONObject o) throws JSONException {
		Node node = null;
		
		String nodeType = o.getString("nodeType");
		if (nodeType.startsWith("cell-as-")) {
			String columnName = o.getString("columnName");
			
			if ("cell-as-topic".equals(nodeType)) {
				if (o.has("type")) {
					node = new CellTopicNode(
						columnName,
						o.getBoolean("createForNoReconMatch"),
						reconstructType(o.getJSONObject("type"))
					);
				}
			} else if ("cell-as-value".equals(nodeType)) {
				node = new CellValueNode(
					columnName,
					o.getString("valueType"),
					o.getString("lang")
				);
			} else if ("cell-as-key".equals(nodeType)) {
				node = new CellKeyNode(
					columnName,
					reconstructTopic(o.getJSONObject("namespace"))
				);
			}
		} else if ("topic".equals(nodeType)) {
			node = new FreebaseTopicNode(reconstructTopic(o.getJSONObject("topic")));
		} else if ("value".equals(nodeType)) {
			node = new ValueNode(
				o.get("value"),
				o.getString("valueType"),
				o.getString("lang")
			);
		} else if ("anonymous".equals(nodeType)) {
			node = new AnonymousNode(reconstructType(o.getJSONObject("type")));
		}
		
		if (node != null) {
			if (node instanceof NodeWithLinks && o.has("links")) {
				NodeWithLinks node2 = (NodeWithLinks) node;
				
				JSONArray links = o.getJSONArray("links");
				int linkCount = links.length();
				
				for (int j = 0; j < linkCount; j++) {
					JSONObject oLink = links.getJSONObject(j);
					
					node2.addLink(new Link(
						reconstructProperty(oLink.getJSONObject("property")),
						reconstructNode(oLink.getJSONObject("target"))
					));
				}
			}
		}
		
		return node;
	}
	
	static protected FreebaseProperty reconstructProperty(JSONObject o) throws JSONException {
		return new FreebaseProperty(
			o.getString("id"),
			o.getString("name")
		);
	}
	
	static protected FreebaseType reconstructType(JSONObject o) throws JSONException {
		return new FreebaseType(
			o.getString("id"),
			o.getString("name")
		);
	}
	
	static protected FreebaseTopic reconstructTopic(JSONObject o) throws JSONException {
		return new FreebaseTopic(
			o.getString("id"),
			o.getString("name")
		);
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("rootNodes"); writer.array();
		
		for (Node node : _rootNodes) {
			node.write(writer, options);
		}
		
		writer.endArray();
		writer.endObject();
	}

}
