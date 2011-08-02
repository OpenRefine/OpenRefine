/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.freebase.protograph;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.freebase.FreebaseProperty;
import com.google.refine.freebase.FreebaseTopic;
import com.google.refine.freebase.FreebaseType;
import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;

public class Protograph implements OverlayModel {
    final protected List<Node> _rootNodes = new LinkedList<Node>();
    
    public int getRootNodeCount() {
        return _rootNodes.size();
    }
    
    public Node getRootNode(int index) {
        return _rootNodes.get(index);
    }
    
    @Override
    public void onBeforeSave(Project project) {
    }
    
    @Override
    public void onAfterSave(Project project) {
    }
    
    
    @Override
    public void dispose(Project project) {
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
            if ("cell-as-topic".equals(nodeType)) {
                if (o.has("type")) {
                    node = new CellTopicNode(
                        reconstructType(o.getJSONObject("type"))
                    );
                }
            } else if ("cell-as-value".equals(nodeType)) {
                node = new CellValueNode(
                    o.getString("valueType"),
                    o.getString("lang")
                );
            } else if ("cell-as-key".equals(nodeType)) {
                node = new CellKeyNode(
                    reconstructTopic(o.getJSONObject("namespace"))
                );
            }
            
            if (o.has("columnName") && !o.isNull("columnName")) {
                ((CellNode) node).columnNames.add(o.getString("columnName"));
            }
            if (o.has("columnNames") && !o.isNull("columnNames")) {
                JSONArray columnNames = o.getJSONArray("columnNames");
                int count = columnNames.length();
                
                for (int c = 0; c < count; c++) {
                    ((CellNode) node).columnNames.add(columnNames.getString(c));
                }
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
        
        if (node != null && node instanceof NodeWithLinks && o.has("links")) {
            NodeWithLinks node2 = (NodeWithLinks) node;
            
            JSONArray links = o.getJSONArray("links");
            int linkCount = links.length();
            
            for (int j = 0; j < linkCount; j++) {
                JSONObject oLink = links.getJSONObject(j);
                Condition condition = null;
                
                if (oLink.has("condition") && !oLink.isNull("condition")) {
                    JSONObject oCondition = oLink.getJSONObject("condition");
                    if (oCondition.has("columnName") && !oCondition.isNull("columnName")) {
                        condition = new BooleanColumnCondition(oCondition.getString("columnName"));
                    }
                }
                
                node2.addLink(new Link(
                    reconstructProperty(oLink.getJSONObject("property")),
                    oLink.has("target") && !oLink.isNull("target") ? 
                        reconstructNode(oLink.getJSONObject("target")) : null,
                    condition,
                    oLink.has("load") && !oLink.isNull("load") ?
                        oLink.getBoolean("load") : true
                ));
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
    
    @Override
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        writer.key("rootNodes"); writer.array();
        
        for (Node node : _rootNodes) {
            node.write(writer, options);
        }
        
        writer.endArray();
        writer.endObject();
    }
    
    static public Protograph load(Project project, JSONObject obj) throws Exception {
        return reconstruct(obj);
    }
}
