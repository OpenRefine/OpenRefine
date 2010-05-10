package com.metaweb.gridworks.protograph.transpose;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.protograph.AnonymousNode;
import com.metaweb.gridworks.protograph.CellKeyNode;
import com.metaweb.gridworks.protograph.CellNode;
import com.metaweb.gridworks.protograph.CellTopicNode;
import com.metaweb.gridworks.protograph.CellValueNode;
import com.metaweb.gridworks.protograph.FreebaseProperty;
import com.metaweb.gridworks.protograph.FreebaseTopicNode;
import com.metaweb.gridworks.protograph.ValueNode;

public class MqlreadLikeTransposedNodeFactory implements TransposedNodeFactory {
    protected List<JSONObject> rootObjects = new LinkedList<JSONObject>();
    
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String CREATE = "create";
    private static final String VALUE = "value";
    private static final String CONNECT = "connect";
    private static final String LANG = "lang";
    
    public JSONArray getJSON() {
        return new JSONArray(rootObjects);
    }
    
    abstract protected class JsonTransposedNode implements TransposedNode {
        abstract public Object getJSON();
    }
    
    abstract protected class JsonObjectTransposedNode extends JsonTransposedNode {
        abstract public JSONObject getJSONObject();
        
        protected JSONObject obj;
        
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
                        obj.put(ID, cell.recon.match.topicID);
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

        public Object getJSON() {
            if (obj == null) {
                obj = new JSONObject();
                try {
                    obj.put(VALUE, cell.value.toString());
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

        public Object getJSON() {
            if (obj == null) {
                obj = new JSONObject();
                try {
                    obj.put(VALUE, node.value);
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
    public TransposedNode transposeAnonymousNode(
            TransposedNode parentNode,
            FreebaseProperty property, 
            AnonymousNode node) {
        
        return new AnonymousTransposedNode(
            parentNode instanceof JsonObjectTransposedNode ? (JsonObjectTransposedNode) parentNode : null, 
            property, 
            node
        );
    }

    public TransposedNode transposeCellNode(
            TransposedNode parentNode,
            FreebaseProperty property, 
            CellNode node, 
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
            processTransposedNode(tnode, parentNode, property);
        }
        return tnode;
    }

    public TransposedNode transposeTopicNode(
            TransposedNode parentNode,
            FreebaseProperty property, 
            FreebaseTopicNode node) {
        
        JsonTransposedNode tnode = new TopicTransposedNode(node);
        
        processTransposedNode(tnode, parentNode, property);
        
        return tnode;
    }

    public TransposedNode transposeValueNode(
            TransposedNode parentNode,
            FreebaseProperty property, 
            ValueNode node) {
        
        JsonTransposedNode tnode = new ValueTransposedNode(node);
        
        processTransposedNode(tnode, parentNode, property);
        
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
