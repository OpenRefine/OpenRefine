package com.metaweb.gridworks.protograph.transpose;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    protected Set<Long> serializedRecons = new HashSet<Long>();
    
    public TripleLoaderTransposedNodeFactory(Writer writer) {
        this.writer = writer;
    }
    
    public void flush() {
        if (lastRootNode != null) {
            lastRootNode.write(null, null, null);
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
    
    protected void writeRecon(StringBuffer sb, Cell cell) {
        Recon recon = cell.recon;
        if (serializedRecons.contains(recon.id)) {
            sb.append(Long.toString(recon.id));
        } else {
            serializedRecons.add(recon.id);
            
            String s = cell.value instanceof String ? (String) cell.value : cell.value.toString();
                    
            sb.append("{ ");
            sb.append("\"id\" : "); sb.append(Long.toString(recon.id));
            sb.append(", \"history_entry\" : "); sb.append(Long.toString(recon.judgmentHistoryEntry));
            sb.append(", \"text\" : "); sb.append(JSONObject.quote(s));
            sb.append(", \"service\" : "); sb.append(JSONObject.quote(recon.service));
            sb.append(", \"action\" : "); sb.append(JSONObject.quote(recon.judgmentAction));
            sb.append(", \"batch\" : "); sb.append(Integer.toString(recon.judgmentBatchSize));
            sb.append(", \"matchRank\" : "); sb.append(Integer.toString(recon.matchRank));
            sb.append(" }");
        }
    }
    
    protected void writeLine(String subject, String predicate, Object object, Cell subjectCell, Cell objectCell) {
        if (subject != null && object != null) {
            String s = object instanceof String ? 
                    JSONObject.quote((String) object) : object.toString();
                    
            StringBuffer sb = new StringBuffer();
            sb.append("{ \"s\" : \""); sb.append(subject); sb.append('"');
            sb.append(", \"p\" : \""); sb.append(predicate); sb.append('"');
            sb.append(", \"o\" : "); sb.append(s);
            if (subjectCell != null || objectCell != null) {
                sb.append(", \"meta\" : { ");
                
                if (subjectCell != null) {
                    sb.append("\"srecon\" : ");
                    writeRecon(sb, subjectCell);
                }
                if (objectCell != null) {
                    if (subjectCell != null) {
                        sb.append(", ");
                    }
                    sb.append("\"orecon\" : ");
                    writeRecon(sb, objectCell);
                }
                
                sb.append(" }");
            }
            sb.append(" }");
                    
            writeLine(sb.toString());
        }
    }
    
    protected void writeLine(String subject, String predicate, Object object, String lang, Cell subjectCell) {
        if (subject != null && object != null) {
            String s = object instanceof String ? 
                    JSONObject.quote((String) object) : object.toString();
                    
            StringBuffer sb = new StringBuffer();
            sb.append("{ \"s\" : \""); sb.append(subject); sb.append('"');
            sb.append(", \"p\" : \""); sb.append(predicate); sb.append('"');
            sb.append(", \"o\" : "); sb.append(s);
            sb.append(", \"lang\" : "); sb.append(lang);
                    
            if (subjectCell != null) {
                sb.append(", \"meta\" : { ");
                sb.append("\"srecon\" : ");
                writeRecon(sb, subjectCell);
                sb.append(" }");
            }
            sb.append(" }");
                    
            writeLine(sb.toString());
        }
    }
    
    protected interface WritingTransposedNode extends TransposedNode {
        public Object write(String subject, String predicate, Cell subjectCell);
    }
    
    abstract protected class TransposedNodeWithChildren implements WritingTransposedNode {
        public List<FreebaseProperty> properties = new LinkedList<FreebaseProperty>();
        public List<WritingTransposedNode> children = new LinkedList<WritingTransposedNode>();
        
        protected void writeChildren(String subject, Cell subjectCell) {
            for (int i = 0; i < children.size(); i++) {
                WritingTransposedNode child = children.get(i);
                String predicate = properties.get(i).id;
                
                child.write(subject, predicate, subjectCell);
            }
        }
    }
    
    protected class AnonymousTransposedNode extends TransposedNodeWithChildren {
        
        //protected AnonymousTransposedNode(AnonymousNode node) { }
        
        public Object write(String subject, String predicate, Cell subjectCell) {
            if (children.size() == 0 || subject == null) {
                return null;
            }
            
            StringBuffer sb = new StringBuffer();
            sb.append("{ ");
            
            boolean first = true;
            for (int i = 0; i < children.size(); i++) {
                Object c = children.get(i).write(null, null, null);
                if (c != null) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append("\"" + properties.get(i).id + "\": ");
                    sb.append(c instanceof String ? JSONObject.quote((String) c) : c.toString());
                }
            }
            sb.append(" }");
            
            writeLine(subject, predicate, sb, subjectCell, null);
            
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
        
        public Object write(String subject, String predicate, Cell subjectCell) {
            String id = null;
            Cell objectCell = null;
            
            if (cell.recon != null &&
                cell.recon.judgment == Recon.Judgment.Matched &&
                cell.recon.match != null) {
                
                objectCell = cell;
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
                    
                    writeLine(id, "type", node.type.id, (Cell) null, (Cell) null);
                    writeLine(id, "name", cell.value, (Cell) null, (Cell) null);
                    
                    if (cell.recon != null) {
                        newTopicVars.put(cell.recon.id, id);
                    }
                }
            } else {
                return null;
            }
            
            if (subject != null) {
                writeLine(subject, predicate, id, subjectCell, objectCell);
            }
            
            writeChildren(id, objectCell);
            
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
        
        public Object write(String subject, String predicate, Cell subjectCell) {
            if (subject != null) {
                if ("/type/text".equals(node.lang)) {
                    writeLine(subject, predicate, cell.value, node.lang, subjectCell);
                } else {
                    writeLine(subject, predicate, cell.value, subjectCell, null);
                }
            }
            
            return cell.value;
        }
    }
    
    protected class CellKeyTransposedNode implements WritingTransposedNode {
        protected CellKeyNode node;
        protected Cell cell;
        
        public CellKeyTransposedNode(CellKeyNode node, Cell cell) {
            this.node = node;
            this.cell = cell;
        }
        
        public Object write(String subject, String predicate, Cell subjectCell) {
            writeLine(subject, "key", node.namespace.id + "/" + cell.value, subjectCell, null);
            
            return null;
        }
    }
    
    protected class TopicTransposedNode extends TransposedNodeWithChildren {
        protected FreebaseTopicNode node;
        
        public TopicTransposedNode(FreebaseTopicNode node) {
            this.node = node;
        }

        public Object write(String subject, String predicate, Cell subjectCell) {
            writeLine(subject, predicate, node.topic.id, subjectCell, null);
            writeChildren(node.topic.id, null);
            
            return node.topic.id;
        }
    }

    protected class ValueTransposedNode implements WritingTransposedNode {
        protected ValueNode node;
        
        public ValueTransposedNode(ValueNode node) {
            this.node = node;
        }

        public Object write(String subject, String predicate, Cell subjectCell) {
            if ("/type/text".equals(node.lang)) {
                writeLine(subject, predicate, node.value, node.lang, subjectCell);
            } else {
                writeLine(subject, predicate, node.value, subjectCell, null);
            }
            
            return node.value;
        }
    }
    
    public TransposedNode transposeAnonymousNode(
            TransposedNode parentNode,
            FreebaseProperty property, 
            AnonymousNode node) {
        
        WritingTransposedNode tnode = new AnonymousTransposedNode();
        
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
            lastRootNode.write(null, null, null);
        }
        lastRootNode = tnode;
    }
}
