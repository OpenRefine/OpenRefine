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

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.model.RecordModel.RowDependency;
import com.metaweb.gridworks.protograph.AnonymousNode;
import com.metaweb.gridworks.protograph.CellKeyNode;
import com.metaweb.gridworks.protograph.CellNode;
import com.metaweb.gridworks.protograph.CellTopicNode;
import com.metaweb.gridworks.protograph.CellValueNode;
import com.metaweb.gridworks.protograph.FreebaseProperty;
import com.metaweb.gridworks.protograph.FreebaseTopicNode;
import com.metaweb.gridworks.protograph.ValueNode;

public class TripleLoaderTransposedNodeFactory implements TransposedNodeFactory {
    protected Project project;
    
    protected boolean start = true;
    protected Writer writer;
    protected WritingTransposedNode lastRootNode;
    protected Map<String, Long> varPool = new HashMap<String, Long>();
    protected Map<Long, String> newTopicVars = new HashMap<Long, String>();
    protected Set<Long> serializedRecons = new HashSet<Long>();
    
    public TripleLoaderTransposedNodeFactory(Project project, Writer writer) {
        this.project = project;
        this.writer = writer;
    }
    
    @Override
    public void flush() throws IOException {
        if (lastRootNode != null) {
            lastRootNode.write(null, null, project, -1, -1, null);
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
    
    protected void writeRecon(StringBuffer sb, Project project, int rowIndex, int cellIndex, Cell cell) {
        Recon recon = cell.recon;
        Column column = project.columnModel.getColumnByCellIndex(cellIndex);
        
        sb.append("{ ");
        sb.append("\"id\" : "); sb.append(Long.toString(recon.id));
        
        if (!serializedRecons.contains(recon.id)) {
            serializedRecons.add(recon.id);
            
            String s = cell.value instanceof String ? (String) cell.value : cell.value.toString();
                    
            sb.append(", \"history_entry\" : "); sb.append(Long.toString(recon.judgmentHistoryEntry));
            sb.append(", \"text\" : "); sb.append(JSONObject.quote(s));
            sb.append(", \"column\" : "); sb.append(JSONObject.quote(column.getName()));
            sb.append(", \"service\" : "); sb.append(JSONObject.quote(recon.service));
            sb.append(", \"action\" : "); sb.append(JSONObject.quote(recon.judgmentAction));
            sb.append(", \"batch\" : "); sb.append(Integer.toString(recon.judgmentBatchSize));
            sb.append(", \"matchRank\" : "); sb.append(Integer.toString(recon.matchRank));
        }
        /*
        sb.append(", \"row\" : [");
        {
            boolean first = true;
            Row row = project.rows.get(rowIndex);
            RowDependency rowDependency = project.recordModel.getRowDependency(rowIndex);
            List<Integer> contextRows = rowDependency.contextRows;
            
            int maxColumns = project.columnModel.columns.size();
            for (int c = 0; c < maxColumns; c++) {
                Column column2 = project.columnModel.columns.get(c);
                int cellIndex2 = column2.getCellIndex();
                
                if (cellIndex2 != cellIndex) {
                    Object value = row.getCellValue(cellIndex2);
                    if (!ExpressionUtils.isNonBlankData(value) && contextRows != null) {
                        for (int i = contextRows.size() - 1; i >= 0; i--) {
                            int rowIndex2 = contextRows.get(i);
                            Row row2 = project.rows.get(rowIndex2);
                            
                            value = row2.getCellValue(cellIndex2);
                            if (ExpressionUtils.isNonBlankData(value)) {
                                break;
                            }
                        }
                    }
                    
                    if (ExpressionUtils.isNonBlankData(value)) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(",");
                        }
                        
                        String s2 = value instanceof String ? (String) value : value.toString();
                        
                        sb.append("{\"c\":"); sb.append(JSONObject.quote(column2.getName()));
                        sb.append(",\"v\":"); sb.append(JSONObject.quote(s2));
                        sb.append("}");
                    }
                }
            }
        }
        sb.append("]");
        */
        
        sb.append(" }");
    }
    
    protected void writeLine(
            String subject, String predicate, Object object, 
            Project project, 
            int subjectRowIndex, int subjectCellIndex, Cell subjectCell, 
            int objectRowIndex, int objectCellIndex, Cell objectCell
        ) {
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
                    writeRecon(sb, project, subjectRowIndex, subjectCellIndex, subjectCell);
                }
                if (objectCell != null) {
                    if (subjectCell != null) {
                        sb.append(", ");
                    }
                    sb.append("\"orecon\" : ");
                    writeRecon(sb, project, objectRowIndex, objectCellIndex, objectCell);
                }
                
                sb.append(" }");
            }
            sb.append(" }");
                    
            writeLine(sb.toString());
        }
    }
    
    protected void writeLine(
        String subject, String predicate, Object object, String lang, 
        Project project, int subjectRowIndex, int subjectCellIndex, Cell subjectCell
    ) {
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
                writeRecon(sb, project, subjectRowIndex, subjectCellIndex, subjectCell);
                sb.append(" }");
            }
            sb.append(" }");
                    
            writeLine(sb.toString());
        }
    }
    
    protected interface WritingTransposedNode extends TransposedNode {
        public Object write(String subject, String predicate, Project project, int subjectRowIndex, int subjectCellIndex, Cell subjectCell);
    }
    
    abstract protected class TransposedNodeWithChildren implements WritingTransposedNode {
        public List<FreebaseProperty> properties = new LinkedList<FreebaseProperty>();
        public List<WritingTransposedNode> children = new LinkedList<WritingTransposedNode>();
        
        protected void writeChildren(String subject, Project project, int subjectRowIndex, int subjectCellIndex, Cell subjectCell) {
            for (int i = 0; i < children.size(); i++) {
                WritingTransposedNode child = children.get(i);
                String predicate = properties.get(i).id;
                
                child.write(subject, predicate, project, subjectRowIndex, subjectCellIndex, subjectCell);
            }
        }
    }
    
    protected class AnonymousTransposedNode extends TransposedNodeWithChildren {
        
        //protected AnonymousTransposedNode(AnonymousNode node) { }
        
        public Object write(String subject, String predicate, Project project, int subjectRowIndex, int subjectCellIndex, Cell subjectCell) {
            if (children.size() == 0 || subject == null) {
                return null;
            }
            
            StringBuffer sb = new StringBuffer();
            sb.append("{ ");
            
            boolean first = true;
            for (int i = 0; i < children.size(); i++) {
                Object c = children.get(i).write(null, null, project, subjectRowIndex, subjectCellIndex, null);
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
            
            writeLine(subject, predicate, sb, project, subjectRowIndex, subjectCellIndex, subjectCell, -1, -1, null);
            
            return null;
        }
    }
    
    protected class CellTopicTransposedNode extends TransposedNodeWithChildren {
        protected CellTopicNode node;
        protected int rowIndex;
        protected int cellIndex;
        protected Cell cell;
        
        public CellTopicTransposedNode(CellTopicNode node, int rowIndex, int cellIndex, Cell cell) {
            this.node = node;
            this.rowIndex = rowIndex;
            this.cellIndex = cellIndex;
            this.cell = cell;
        }
        
        public Object write(String subject, String predicate, Project project, int subjectRowIndex, int subjectCellIndex, Cell subjectCell) {
            String id = null;
            int objectRowIndex = -1;
            int objectCellIndex = -1;
            Cell objectCell = null;
            
            if (cell.recon != null &&
                cell.recon.judgment == Recon.Judgment.Matched &&
                cell.recon.match != null) {
                
                objectRowIndex = rowIndex;
                objectCellIndex = cellIndex;
                objectCell = cell;
                id = cell.recon.match.id;
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
                    
                    writeLine(id, "type", node.type.id, project, -1, -1, (Cell) null, -1, -1, (Cell) null);
                    writeLine(id, "name", cell.value, project, -1, -1, (Cell) null, -1, -1, (Cell) null);
                    
                    if (cell.recon != null) {
                        newTopicVars.put(cell.recon.id, id);
                    }
                }
            } else {
                return null;
            }
            
            if (subject != null) {
                writeLine(subject, predicate, id, project, 
                        subjectRowIndex, subjectCellIndex, subjectCell, 
                        objectRowIndex, objectCellIndex, objectCell);
            }
            
            writeChildren(id, project, objectRowIndex, objectCellIndex, objectCell);
            
            return id;
        }
    }
    
    protected class CellValueTransposedNode implements WritingTransposedNode {
        protected JSONObject obj;
        protected CellValueNode node;
        protected int rowIndex;
        protected int cellIndex;
        protected Cell cell;
        
        public CellValueTransposedNode(CellValueNode node, int rowIndex, int cellIndex, Cell cell) {
            this.node = node;
            this.rowIndex = rowIndex;
            this.cellIndex = cellIndex;
            this.cell = cell;
        }
        
        public Object write(String subject, String predicate, Project project, int subjectRowIndex, int subjectCellIndex, Cell subjectCell) {
            if (subject != null) {
                if ("/type/text".equals(node.lang)) {
                    writeLine(subject, predicate, cell.value, node.lang, project, 
                            subjectRowIndex, subjectCellIndex, subjectCell);
                } else {
                    writeLine(subject, predicate, cell.value, project, 
                            subjectRowIndex, subjectCellIndex, subjectCell, 
                            -1, -1, null);
                }
            }
            
            return cell.value;
        }
    }
    
    protected class CellKeyTransposedNode implements WritingTransposedNode {
        protected CellKeyNode node;
        protected int rowIndex;
        protected int cellIndex;
        protected Cell cell;
        
        public CellKeyTransposedNode(CellKeyNode node, int rowIndex, int cellIndex, Cell cell) {
            this.node = node;
            this.rowIndex = rowIndex;
            this.cellIndex = cellIndex;
            this.cell = cell;
        }
        
        public Object write(String subject, String predicate, Project project, int subjectRowIndex, int subjectCellIndex, Cell subjectCell) {
            writeLine(subject, "key", node.namespace.id + "/" + cell.value, project, 
                subjectRowIndex, subjectCellIndex, subjectCell, 
                -1, -1, null);
            
            return null;
        }
    }
    
    protected class TopicTransposedNode extends TransposedNodeWithChildren {
        protected FreebaseTopicNode node;
        
        public TopicTransposedNode(FreebaseTopicNode node) {
            this.node = node;
        }

        public Object write(String subject, String predicate, Project project, int subjectRowIndex, int subjectCellIndex, Cell subjectCell) {
            writeLine(subject, predicate, node.topic.id, project, 
                subjectRowIndex, subjectCellIndex, subjectCell, 
                -1, -1, null);
            
            writeChildren(node.topic.id, project, -1, -1, null);
            
            return node.topic.id;
        }
    }

    protected class ValueTransposedNode implements WritingTransposedNode {
        protected ValueNode node;
        
        public ValueTransposedNode(ValueNode node) {
            this.node = node;
        }

        public Object write(String subject, String predicate, Project project, int subjectRowIndex, int subjectCellIndex, Cell subjectCell) {
            if ("/type/text".equals(node.lang)) {
                writeLine(subject, predicate, node.value, node.lang, project,
                    subjectRowIndex, subjectCellIndex, subjectCell);
            } else {
                writeLine(subject, predicate, node.value, project, 
                    subjectRowIndex, subjectCellIndex, subjectCell, 
                    -1, -1, null);
            }
            
            return node.value;
        }
    }
    
    public TransposedNode transposeAnonymousNode(
            TransposedNode parentNode,
            FreebaseProperty property, 
            AnonymousNode node, int rowIndex) {
        
        WritingTransposedNode tnode = new AnonymousTransposedNode();
        
        processTransposedNode(tnode, parentNode, property);
        
        return tnode;
    }

    public TransposedNode transposeCellNode(
            TransposedNode parentNode,
            FreebaseProperty property, 
            CellNode node, 
            int rowIndex, 
            Cell cell) {
        
        Column column = project.columnModel.getColumnByName(node.columnName);
        int cellIndex = column != null ? column.getCellIndex() : -1;
        
        WritingTransposedNode tnode = null;
        if (node instanceof CellTopicNode) {
            tnode = new CellTopicTransposedNode((CellTopicNode) node, rowIndex, cellIndex, cell);
        } else if (node instanceof CellValueNode) {
            tnode = new CellValueTransposedNode((CellValueNode) node, rowIndex, cellIndex, cell);
        } else if (node instanceof CellKeyNode) {
            tnode = new CellKeyTransposedNode((CellKeyNode) node, rowIndex, cellIndex, cell);
        }
        
        if (tnode != null) {
            processTransposedNode(tnode, parentNode, property);
        }
        return tnode;
    }

    public TransposedNode transposeTopicNode(
            TransposedNode parentNode,
            FreebaseProperty property, 
            FreebaseTopicNode node, int rowIndex) {
        
        WritingTransposedNode tnode = new TopicTransposedNode(node);
        
        processTransposedNode(tnode, parentNode, property);
        
        return tnode;
    }

    public TransposedNode transposeValueNode(
            TransposedNode parentNode,
            FreebaseProperty property, 
            ValueNode node, int rowIndex) {
        
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
            lastRootNode.write(null, null, project, -1, -1, null);
        }
        lastRootNode = tnode;
    }
}
