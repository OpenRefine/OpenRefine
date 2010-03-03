package com.metaweb.gridworks.protograph.transpose;

import java.util.LinkedList;
import java.util.List;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.protograph.AnonymousNode;
import com.metaweb.gridworks.protograph.CellNode;
import com.metaweb.gridworks.protograph.CellTopicNode;
import com.metaweb.gridworks.protograph.FreebaseProperty;
import com.metaweb.gridworks.protograph.FreebaseTopicNode;
import com.metaweb.gridworks.protograph.Link;
import com.metaweb.gridworks.protograph.Node;
import com.metaweb.gridworks.protograph.NodeWithLinks;
import com.metaweb.gridworks.protograph.Protograph;
import com.metaweb.gridworks.protograph.ValueNode;

public class Transposer {
    static public void transpose(
        Project                 project,
        Protograph                 protograph,
        Node                     rootNode,
        TransposedNodeFactory     nodeFactory
    ) {
        Context rootContext = new Context(rootNode, null, null, 20);
        
        for (Row row : project.rows) {
            descend(project, protograph, nodeFactory, row, rootNode, rootContext);
            if (rootContext.limit > 0 && rootContext.count > rootContext.limit) {
                break;
            }
        }
    }
    
    static protected void descend(
        Project project,
        Protograph protograph, 
        TransposedNodeFactory nodeFactory,
        Row row,
        Node node,
        Context context
    ) {
        TransposedNode tnode = null;
        
        TransposedNode parentNode = context.parent == null ? null : context.parent.transposedNode;
        FreebaseProperty property = context.parent == null ? null : context.link.property;
        
        if (node instanceof CellNode) {
            CellNode node2 = (CellNode) node;
            Column column = project.columnModel.getColumnByName(node2.columnName);
            Cell cell = row.getCell(column.getCellIndex());
            if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                if (node2 instanceof CellTopicNode) {
                    if (!((CellTopicNode) node2).createForNoReconMatch && 
                        (cell.recon == null || cell.recon.judgment == Judgment.None)) {
                        return;
                    }
                }
                
                context.count++;
                if (context.limit > 0 && context.count > context.limit) {
                    return;
                }
                
                tnode = nodeFactory.transposeCellNode(
                    parentNode,
                    property,
                    node2, 
                    cell
                );
            }
        } else {
            if (node instanceof AnonymousNode) {
                tnode = nodeFactory.transposeAnonymousNode(
                    parentNode,
                    property,
                    (AnonymousNode) node
                );
            } else if (node instanceof FreebaseTopicNode) {
                tnode = nodeFactory.transposeTopicNode(
                    parentNode,
                    property,
                    (FreebaseTopicNode) node
                );
            } else if (node instanceof ValueNode) {
                tnode = nodeFactory.transposeValueNode(
                    parentNode,
                    property,
                    (ValueNode) node
                );
            }
        }
        
        if (tnode != null) {
            context.transposedNode = tnode;
            context.nullifySubContextNodes();
        } /*
             else, previous rows might have set the context transposed node already,
             and we simply inherit that transposed node.
        */
        
        if (node instanceof NodeWithLinks && context.transposedNode != null) {
            NodeWithLinks node2 = (NodeWithLinks) node;
            
            int linkCount = node2.getLinkCount();
            
            for (int i = 0; i < linkCount; i++) {
                descend(
                    project, 
                    protograph, 
                    nodeFactory, 
                    row, 
                    node2.getLink(i).getTarget(), 
                    context.subContexts.get(i)
                );
            }
        }
    }
    
    static class Context {
        TransposedNode     transposedNode;
        List<Context>     subContexts;
        Context         parent;
        Link            link;
        int                count;
        int                limit;
        
        Context(Node node, Context parent, Link link, int limit) {
            this.parent = parent;
            this.link = link;
            this.limit = limit;
            
            if (node instanceof NodeWithLinks) {
                NodeWithLinks node2 = (NodeWithLinks) node;
                
                int subContextCount = node2.getLinkCount();
                
                subContexts = new LinkedList<Context>();
                for (int i = 0; i < subContextCount; i++) {
                    Link link2 = node2.getLink(i);
                    subContexts.add(
                        new Context(link2.getTarget(), this, link2, -1));
                }
            }
        }
        
        public void nullifySubContextNodes() {
            if (subContexts != null) {
                for (Context context : subContexts) {
                    context.transposedNode = null;
                    context.nullifySubContextNodes();
                }
            }
        }
    }
}
