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

import java.util.LinkedList;
import java.util.List;

import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.freebase.protograph.AnonymousNode;
import com.google.refine.freebase.protograph.CellNode;
import com.google.refine.freebase.protograph.CellTopicNode;
import com.google.refine.freebase.protograph.FreebaseTopicNode;
import com.google.refine.freebase.protograph.Link;
import com.google.refine.freebase.protograph.Node;
import com.google.refine.freebase.protograph.NodeWithLinks;
import com.google.refine.freebase.protograph.Protograph;
import com.google.refine.freebase.protograph.ValueNode;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.Row;

public class Transposer {
    static public void transpose(
        Project                 project,
        FilteredRows            filteredRows,
        Protograph              protograph,
        Node                    rootNode,
        TransposedNodeFactory   nodeFactory
    ) {
        transpose(project, filteredRows, protograph, rootNode, nodeFactory, 20);
    }
    
    static public void transpose(
        Project                 project,
        FilteredRows            filteredRows,
        Protograph              protograph,
        Node                    rootNode,
        TransposedNodeFactory   nodeFactory,
        int                     limit
    ) {
        Context rootContext = new Context(rootNode, null, null, limit);
        
        filteredRows.accept(project, new RowVisitor() {
            Context                 rootContext;
            Protograph              protograph;
            Node                    rootNode;
            TransposedNodeFactory   nodeFactory;
            
            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                if (rootContext.limit <= 0 || rootContext.count < rootContext.limit) {
                    descend(project, protograph, nodeFactory, rowIndex, row, rootNode, rootContext);
                }
                
                if (rootContext.limit > 0 && rootContext.count > rootContext.limit) {
                    return true;
                }
                return false;
            }
            
            @Override
            public void start(Project project) {
            }
            
            @Override
            public void end(Project project) {
            }
            
            public RowVisitor init(
                Context                 rootContext,
                Protograph              protograph,
                Node                    rootNode,
                TransposedNodeFactory   nodeFactory
            ) {
                this.rootContext = rootContext;
                this.protograph = protograph;
                this.rootNode = rootNode;
                this.nodeFactory = nodeFactory;
                
                return this;
            }
        }.init(rootContext, protograph, rootNode, nodeFactory));
    }
    
    static protected void descend(
        Project project,
        Protograph protograph, 
        TransposedNodeFactory nodeFactory,
        int rowIndex, 
        Row row,
        Node node,
        Context context
    ) {
        List<TransposedNode> tnodes = new LinkedList<TransposedNode>();
        
        Link link = context.parent == null ? null : context.link;
        
        if (node instanceof CellNode) {
            if (!descendCellNode(project, nodeFactory, rowIndex, row, node, context, tnodes, link)) {
                return;
            }
        } else if (node instanceof AnonymousNode) {
            descendAnonymousNode(nodeFactory, rowIndex, node, context, tnodes, link);
        } else if (node instanceof FreebaseTopicNode) {
            descendFreebaseTopicNode(nodeFactory, rowIndex, node, context, tnodes, link);
        } else if (node instanceof ValueNode) {
            descendValueNode(nodeFactory, rowIndex, node, context, tnodes, link);
        }
        
        if (tnodes.size() > 0) {
            context.transposedNodes.clear();
            context.transposedNodes.addAll(tnodes);
        }
        
        if (node instanceof NodeWithLinks) {
            NodeWithLinks node2 = (NodeWithLinks) node;
            int linkCount = node2.getLinkCount();
            
            for (int i = 0; i < linkCount; i++) {
                Link link2 = node2.getLink(i);
                if (link2.condition == null || link2.condition.test(project, rowIndex, row)) {
                    descend(
                        project, 
                        protograph, 
                        nodeFactory,
                        rowIndex,
                        row, 
                        link2.getTarget(), 
                        context.subContexts.get(i)
                    );
                }
            }
        }
    }

    private static boolean descendCellNode(Project project, TransposedNodeFactory nodeFactory, int rowIndex, Row row,
            Node node, Context context, List<TransposedNode> tnodes, Link link) {
        CellNode node2 = (CellNode) node;
        for (String columnName : node2.columnNames) {
            Column column = project.columnModel.getColumnByName(columnName);
            if (column != null) {
                int cellIndex = column.getCellIndex();
                
                Cell cell = row.getCell(cellIndex);
                if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                    if (node2 instanceof CellTopicNode &&
                        (cell.recon == null || cell.recon.judgment == Judgment.None)) {
                            return false;
                    }
                    
                    context.count++;
                    if (context.limit > 0 && context.count > context.limit) {
                        return false;
                    }
                    
                    if (context.parent == null) {
                        tnodes.add(nodeFactory.transposeCellNode(
                            null,
                            link,
                            node2, 
                            rowIndex,
                            cellIndex,
                            cell
                        ));
                    } else {
                        for (TransposedNode parentNode : context.parent.transposedNodes) {
                            tnodes.add(nodeFactory.transposeCellNode(
                                parentNode,
                                link,
                                node2, 
                                rowIndex,
                                cellIndex,
                                cell
                            ));
                        }
                    }
                }
            }
        }
        return true;
    }

    private static void descendAnonymousNode(TransposedNodeFactory nodeFactory, int rowIndex, Node node,
            Context context, List<TransposedNode> tnodes, Link link) {
        if (context.parent == null) {
            tnodes.add(nodeFactory.transposeAnonymousNode(
                null,
                link,
                (AnonymousNode) node,
                rowIndex
            ));
        } else {
            for (TransposedNode parentNode : context.parent.transposedNodes) {
                tnodes.add(nodeFactory.transposeAnonymousNode(
                    parentNode,
                    link,
                    (AnonymousNode) node,
                    rowIndex
                ));
            }
        }
    }

    private static void descendFreebaseTopicNode(TransposedNodeFactory nodeFactory, int rowIndex, Node node,
            Context context, List<TransposedNode> tnodes, Link link) {
        if (context.parent == null) {
            tnodes.add(nodeFactory.transposeTopicNode(
                null,
                link,
                (FreebaseTopicNode) node,
                rowIndex
            ));
        } else {
            for (TransposedNode parentNode : context.parent.transposedNodes) {
                tnodes.add(nodeFactory.transposeTopicNode(
                    parentNode,
                    link,
                    (FreebaseTopicNode) node,
                    rowIndex
                ));
            }
        }
    }

    private static void descendValueNode(TransposedNodeFactory nodeFactory, int rowIndex, Node node, Context context,
            List<TransposedNode> tnodes, Link link) {
        if (context.parent == null) {
            tnodes.add(nodeFactory.transposeValueNode(
                null,
                link,
                (ValueNode) node,
                rowIndex
            ));
        } else {
            for (TransposedNode parentNode : context.parent.transposedNodes) {
                tnodes.add(nodeFactory.transposeValueNode(
                    parentNode,
                    link,
                    (ValueNode) node,
                    rowIndex
                ));
            }
        }
    }
    
    static class Context {
        List<TransposedNode>    transposedNodes = new LinkedList<TransposedNode>();
        List<Context>           subContexts;
        Context                 parent;
        Link                    link;
        int                     count;
        int                     limit;
        
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
                    context.transposedNodes.clear();
                    context.nullifySubContextNodes();
                }
            }
        }
    }
}
