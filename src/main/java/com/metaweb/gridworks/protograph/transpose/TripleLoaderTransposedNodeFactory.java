package com.metaweb.gridworks.protograph.transpose;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.protograph.AnonymousNode;
import com.metaweb.gridworks.protograph.CellKeyNode;
import com.metaweb.gridworks.protograph.CellNode;
import com.metaweb.gridworks.protograph.CellTopicNode;
import com.metaweb.gridworks.protograph.CellValueNode;
import com.metaweb.gridworks.protograph.FreebaseProperty;
import com.metaweb.gridworks.protograph.FreebaseTopicNode;
import com.metaweb.gridworks.protograph.ValueNode;

public class TripleLoaderTransposedNodeFactory implements TransposedNodeFactory {
    protected boolean start = true;
    protected Writer writer;
    protected WritingTransposedNode lastRootNode;
    protected Map<String, Long> varPool = new HashMap<String, Long>();
    protected Map<Long, String> newTopicVars = new HashMap<Long, String>();
    
    public TripleLoaderTransposedNodeFactory(Writer writer) {
        this.writer = writer;
    }
    
    public void flush() {
        if (lastRootNode != null) {
            lastRootNode.write(null, null);
            lastRootNode = null;
        }
    }
    
    protected void writeLine(String line) {
        try {
            if (start) {
                start = false;
            } else {
                writer.write('\n');
            }
            writer.write(line);
        } catch (IOException e) {
            // ignore
        }
    }
    protected void writeLine(String subject, String predicate, Object object) {
        if (subject != null && object != null) {
            String s = object instanceof String ? 
                    JSONObject.quote((String) object) : object.toString();
                    
            writeLine("{ \"s\" : \"" + subject + "\", \"p\" : \"" + predicate + "\", \"o\" : " + s + " }");
        }
    }
    protected void writeLine(String subject, String predicate, Object object, String lang) {
        if (subject != null && object != null) {
            String s = object instanceof String ? 
                    JSONObject.quote((String) object) : object.toString();
                    
            writeLine("{ \"s\" : \"" + 
                    subject + "\", \"p\" : \"" + 
                    predicate + "\", \"o\" : " + 
                    s + ", \"lang\" : \"" + lang + "\" }");
        }
    }
    
    protected interface WritingTransposedNode extends TransposedNode {
        public String write(String subject, String predicate);
    }
    
    abstract protected class TransposedNodeWithChildren implements WritingTransposedNode {
        public List<FreebaseProperty> properties = new LinkedList<FreebaseProperty>();
        public List<WritingTransposedNode> children = new LinkedList<WritingTransposedNode>();
        
        protected void writeChildren(String subject) {
            for (int i = 0; i < children.size(); i++) {
                WritingTransposedNode child = children.get(i);
                String predicate = properties.get(i).id;
                
                child.write(subject, predicate);
            }
        }
    }
    
    protected class AnonymousTransposedNode extends TransposedNodeWithChildren {
        AnonymousNode node;
        
        protected AnonymousTransposedNode(
            AnonymousNode node
        ) {
            this.node = node;
        }
        
        public String write(String subject, String predicate) {
            if (children.size() == 0 || subject == null) {
                return null;
            }
            
            StringBuffer sb = new StringBuffer();
            sb.append("{ ");
            
            boolean first = true;
            for (int i = 0; i < children.size(); i++) {
                String s = children.get(i).write(null, null);
                if (s != null) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append("\"" + properties.get(i).id + "\": ");
                    sb.append(s instanceof String ? JSONObject.quote(s) : s.toString());
                }
            }
            sb.append(" }");
            
            writeLine(subject, predicate, sb);
            
            return null;
        }
    }
    
    protected class CellTopicTransposedNode extends TransposedNodeWithChildren {
        protected CellTopicNode node;
        protected Cell cell;
        
        public CellTopicTransposedNode(CellTopicNode node, Cell cell) {
            this.node = node;
            this.cell = cell;
        }
        
        public String write(String subject, String predicate) {
            String id = null;
            if (cell.recon != null &&
                cell.recon.judgment == Recon.Judgment.Matched &&
                cell.recon.match != null) {
                
                id = cell.recon.match.topicID;
            } else if (node.createForNoReconMatch || 
                    (cell.recon != null && cell.recon.judgment == Judgment.New)) {
                if (cell.recon != null && newTopicVars.containsKey(cell.recon.id)) {
                    id = newTopicVars.get(cell.recon.id);
                } else {
                    long var = 0;
                    if (varPool.containsKey(node.columnName)) {
                        var = varPool.get(node.columnName);
                    }
                    varPool.put(node.columnName, var + 1);
                    
                    id = "$" + node.columnName.replaceAll("\\W+", "_") + "_" + var;
                    
                    writeLine(id, "type", node.type.id);
                    writeLine(id, "name", cell.value);
                    
                    if (cell.recon != null) {
                        newTopicVars.put(cell.recon.id, id);
                    }
                }
            } else {
                return null;
            }
            
            if (subject != null) {
                writeLine(subject, predicate, id);
            }
            
            writeChildren(id);
            
            return id;
        }
    }
    
    protected class CellValueTransposedNode implements WritingTransposedNode {
        protected JSONObject obj;
        protected CellValueNode node;
        protected Cell cell;
        
        public CellValueTransposedNode(CellValueNode node, Cell cell) {
            this.node = node;
            this.cell = cell;
        }
        
        public String write(String subject, String predicate) {
            if (subject != null) {
                if ("/type/text".equals(node.lang)) {
                    writeLine(subject, predicate, cell.value, node.lang);
                } else {
                    writeLine(subject, predicate, cell.value);
                }
            }
            
            return cell.value.toString();
        }
    }
    
    protected class CellKeyTransposedNode implements WritingTransposedNode {
        protected CellKeyNode node;
        protected Cell cell;
        
        public CellKeyTransposedNode(CellKeyNode node, Cell cell) {
            this.node = node;
            this.cell = cell;
        }
        
        public String write(String subject, String predicate) {
            writeLine(subject, "key", node.namespace.id + "/" + cell.value);
            
            return null;
        }
    }
    
    protected class TopicTransposedNode extends TransposedNodeWithChildren {
        protected FreebaseTopicNode node;
        
        public TopicTransposedNode(FreebaseTopicNode node) {
            this.node = node;
        }

        public String write(String subject, String predicate) {
            writeLine(subject, predicate, node.topic.id);
            writeChildren(node.topic.id);
            
            return node.topic.id;
        }
    }

    protected class ValueTransposedNode implements WritingTransposedNode {
        protected ValueNode node;
        
        public ValueTransposedNode(ValueNode node) {
            this.node = node;
        }

        public String write(String subject, String predicate) {
            if ("/type/text".equals(node.lang)) {
                writeLine(subject, predicate, node.value, node.lang);
            } else {
                writeLine(subject, predicate, node.value);
            }
            
            return node.value.toString();
        }
    }
    
    public TransposedNode transposeAnonymousNode(
            TransposedNode parentNode,
            FreebaseProperty property, 
            AnonymousNode node) {
        
        WritingTransposedNode tnode = new AnonymousTransposedNode(node);
        
        processTransposedNode(tnode, parentNode, property);
        
        return tnode;
    }

    public TransposedNode transposeCellNode(
            TransposedNode parentNode,
            FreebaseProperty property, 
            CellNode node, 
            Cell cell) {
        
        WritingTransposedNode tnode = null;
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
        
        WritingTransposedNode tnode = new TopicTransposedNode(node);
        
        processTransposedNode(tnode, parentNode, property);
        
        return tnode;
    }

    public TransposedNode transposeValueNode(
            TransposedNode parentNode,
            FreebaseProperty property, 
            ValueNode node) {
        
        WritingTransposedNode tnode = new ValueTransposedNode(node);
        
        processTransposedNode(tnode, parentNode, property);
        
        return tnode;
    }
    
    protected void processTransposedNode(
        WritingTransposedNode  tnode, 
        TransposedNode         parentNode,
        FreebaseProperty       property 
    ) {
        if (parentNode != null) {
            if (parentNode instanceof TransposedNodeWithChildren) {
                TransposedNodeWithChildren parentNode2 = (TransposedNodeWithChildren) parentNode;
                parentNode2.children.add(tnode);
                parentNode2.properties.add(property);
            }
        } else {
            addRootNode(tnode);
        }
    }
    
    protected void addRootNode(WritingTransposedNode tnode) {
        if (lastRootNode != null) {
            lastRootNode.write(null, null);
        }
        lastRootNode = tnode;
    }
}
