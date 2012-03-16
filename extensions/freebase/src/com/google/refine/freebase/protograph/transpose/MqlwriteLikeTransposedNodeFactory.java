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

package com.google.refine.freebase.protograph.transpose;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.freebase.FreebaseProperty;
import com.google.refine.freebase.protograph.AnonymousNode;
import com.google.refine.freebase.protograph.CellKeyNode;
import com.google.refine.freebase.protograph.CellNode;
import com.google.refine.freebase.protograph.CellTopicNode;
import com.google.refine.freebase.protograph.CellValueNode;
import com.google.refine.freebase.protograph.FreebaseTopicNode;
import com.google.refine.freebase.protograph.Link;
import com.google.refine.freebase.protograph.ValueNode;
import com.google.refine.model.Cell;
import com.google.refine.model.Recon;
import com.google.refine.util.JSONUtilities;

public class MqlwriteLikeTransposedNodeFactory implements TransposedNodeFactory {
    protected Writer writer;
    protected List<JSONObject> rootObjects = new LinkedList<JSONObject>();
    
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String CREATE = "create";
    private static final String VALUE = "value";
    private static final String CONNECT = "connect";
    private static final String LANG = "lang";
    
    public MqlwriteLikeTransposedNodeFactory(Writer writer) {
        this.writer = writer;
    }
    
    protected JSONArray getJSON() {
        return new JSONArray(rootObjects);
    }
    
    @Override
    public void flush() throws IOException {
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);
            
            jsonWriter.array();
            for (JSONObject obj : rootObjects) {
                jsonWriter.value(obj);
            }
            jsonWriter.endArray();
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        writer.flush();
    }
    
    abstract protected class JsonTransposedNode implements TransposedNode {
        abstract public Object getJSON();
    }
    
    abstract protected class JsonObjectTransposedNode extends JsonTransposedNode {
        abstract public JSONObject getJSONObject();
        
        protected JSONObject obj;
        
        @Override
        public Object getJSON() {
            return getJSONObject();
        }
    }
    
    protected class AnonymousTransposedNode extends JsonObjectTransposedNode {
        JsonObjectTransposedNode parent;
        FreebaseProperty property;
        AnonymousNode node;
        
        protected AnonymousTransposedNode(
            JsonObjectTransposedNode parent,
            FreebaseProperty property,
            AnonymousNode node
        ) {
            this.parent = parent;
            this.property = property;
            this.node = node;
        }

        @Override
        public JSONObject getJSONObject() {
            if (obj == null) {
                obj = new JSONObject();
                try {
                    obj.put(TYPE, this.node.type.id);
                    obj.put(ID, (String) null);
                    obj.put(CREATE, "unconditional");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
                linkTransposedNodeJSON(obj, parent, property);
            }
            
            return obj;
        }
    }
    
    protected class CellTopicTransposedNode extends JsonObjectTransposedNode {
        protected CellTopicNode node;
        protected Cell cell;
        
        public CellTopicTransposedNode(CellTopicNode node, Cell cell) {
            this.node = node;
            this.cell = cell;
        }

        @Override
        public JSONObject getJSONObject() {
            if (obj == null) {
                obj = new JSONObject();
                try {
                    if (cell.recon != null && 
                        cell.recon.judgment == Recon.Judgment.Matched &&
                        cell.recon.match != null) {
                        obj.put(ID, cell.recon.match.id);
                    } else {
                        obj.put(ID, (String) null);
                        obj.put(NAME, cell.value.toString());
                        obj.put(TYPE, node.type.id);
                        obj.put(CREATE, "unless_exists");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return obj;
        }
    }
    
    protected class CellValueTransposedNode extends JsonTransposedNode {
        protected JSONObject obj;
        protected CellValueNode node;
        protected Cell cell;
        
        public CellValueTransposedNode(CellValueNode node, Cell cell) {
            this.node = node;
            this.cell = cell;
        }

        @Override
        public Object getJSON() {
            if (obj == null) {
                obj = new JSONObject();
                try {
                    JSONUtilities.putField(obj, VALUE, cell.value);
                    
                    obj.put(TYPE, node.valueType);
                    if ("/type/text".equals(node.valueType)) {
                        obj.put(LANG, node.lang);
                    }
                    
                    obj.put(CONNECT, "insert");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return obj;
        }
    }
    
    protected class CellKeyTransposedNode extends JsonTransposedNode {
        protected JSONObject obj;
        protected CellKeyNode node;
        protected Cell cell;
        
        public CellKeyTransposedNode(CellKeyNode node, Cell cell) {
            this.node = node;
            this.cell = cell;
        }

        @Override
        public Object getJSON() {
            if (obj == null) {
                obj = new JSONObject();
                try {
                    obj.put(VALUE, cell.value.toString());
                    
                    JSONObject nsObj = new JSONObject();
                    nsObj.put(ID, node.namespace.id);
                    
                    obj.put("namespace", nsObj);
                    obj.put(CONNECT, "insert");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return obj;
        }
    }
    
    protected class TopicTransposedNode extends JsonObjectTransposedNode {
        protected FreebaseTopicNode node;
        
        public TopicTransposedNode(FreebaseTopicNode node) {
            this.node = node;
        }

        @Override
        public JSONObject getJSONObject() {
            if (obj == null) {
                obj = new JSONObject();
                try {
                    obj.put(ID, node.topic.id);
                    // TODO: This won't work at the root of the query, so that needs
                    // to be special cased, but for now one must use a different shaped graph
                    // (ie move the Freebase topic to someplace other than the root
                    obj.put(CONNECT, "insert");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return obj;
        }
    }

    protected class ValueTransposedNode extends JsonTransposedNode {
        protected JSONObject obj;
        protected ValueNode node;
        
        public ValueTransposedNode(ValueNode node) {
            this.node = node;
        }

        @Override
        public Object getJSON() {
            if (obj == null) {
                obj = new JSONObject();
                try {
                    if ("/type/datetime".equals(node.valueType) && node.value instanceof Long) {
                        // Special case integers as year-only dates
                        obj.put(VALUE, node.value.toString());
                    } else {
                        obj.put(VALUE, node.value);                        
                    }
                    obj.put(TYPE, node.valueType);
                    if ("/type/text".equals(node.valueType)) {
                        obj.put(LANG, node.lang);
                    }
                    
                    obj.put(CONNECT, "insert");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return obj;
        }
    }    
    @Override
    public TransposedNode transposeAnonymousNode(
            TransposedNode parentNode,
            Link link, 
            AnonymousNode node, int rowIndex) {
        
        return new AnonymousTransposedNode(
            parentNode instanceof JsonObjectTransposedNode ? (JsonObjectTransposedNode) parentNode : null, 
            link != null ? link.property : null,
            node
        );
    }

    @Override
    public TransposedNode transposeCellNode(
            TransposedNode parentNode,
            Link link, 
            CellNode node, 
            int rowIndex,
            int cellIndex,
            Cell cell) {
        
        JsonTransposedNode tnode = null;
        if (node instanceof CellTopicNode) {
            tnode = new CellTopicTransposedNode((CellTopicNode) node, cell);
        } else if (node instanceof CellValueNode) {
            tnode = new CellValueTransposedNode((CellValueNode) node, cell);
        } else if (node instanceof CellKeyNode) {
            tnode = new CellKeyTransposedNode((CellKeyNode) node, cell);
        }
        
        if (tnode != null) {
            processTransposedNode(tnode, parentNode, link != null ? link.property : null);
        }
        return tnode;
    }

    @Override
    public TransposedNode transposeTopicNode(
            TransposedNode parentNode,
            Link link, 
            FreebaseTopicNode node, int rowIndex) {
        
        JsonTransposedNode tnode = new TopicTransposedNode(node);
        
        processTransposedNode(tnode, parentNode, link != null ? link.property : null);
        
        return tnode;
    }

    @Override
    public TransposedNode transposeValueNode(
            TransposedNode parentNode,
            Link link, 
            ValueNode node, int rowIndex) {
        
        JsonTransposedNode tnode = new ValueTransposedNode(node);
        
        processTransposedNode(tnode, parentNode, link != null ? link.property : null);
        
        return tnode;
    }
    
    protected void processTransposedNode(
        JsonTransposedNode         tnode, 
        TransposedNode             parentNode,
        FreebaseProperty         property 
        ) {
        
        if (!(tnode instanceof AnonymousTransposedNode)) {
            linkTransposedNodeJSON(tnode.getJSON(), parentNode, property);
        }
    }
    
    protected void linkTransposedNodeJSON(
        Object                obj,
        TransposedNode         parentNode,
        FreebaseProperty     property 
        ) {
        
        if (parentNode == null) {
            if (obj instanceof JSONObject) {
                rootObjects.add((JSONObject) obj);
            }
        } else if (parentNode instanceof JsonTransposedNode) {
            JSONObject parentObj = ((JsonObjectTransposedNode) parentNode).getJSONObject();
            
            try {
                JSONArray a = null;
                if (parentObj.has(property.id)) {
                    a = parentObj.getJSONArray(property.id);
                } else {
                    a = new JSONArray();
                    parentObj.put(property.id, a);
                }
                
                a.put(a.length(), obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
