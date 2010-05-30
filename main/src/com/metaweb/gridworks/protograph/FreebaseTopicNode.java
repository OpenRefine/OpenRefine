package com.metaweb.gridworks.protograph;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class FreebaseTopicNode implements Node, NodeWithLinks {
    final public FreebaseTopic topic;
    final public List<Link> links = new LinkedList<Link>();
    
    public FreebaseTopicNode(FreebaseTopic topic) {
        this.topic = topic;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("nodeType"); writer.value("topic");
        writer.key("topic"); topic.write(writer, options);
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
    
    public Link getLink(int index) {
        return links.get(index);
    }

    public int getLinkCount() {
        return links.size();
    }
}
