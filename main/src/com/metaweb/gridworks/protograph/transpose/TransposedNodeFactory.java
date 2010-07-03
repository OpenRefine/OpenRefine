package com.metaweb.gridworks.protograph.transpose;

import java.io.IOException;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.protograph.AnonymousNode;
import com.metaweb.gridworks.protograph.CellNode;
import com.metaweb.gridworks.protograph.FreebaseTopicNode;
import com.metaweb.gridworks.protograph.Link;
import com.metaweb.gridworks.protograph.ValueNode;

public interface TransposedNodeFactory {
    public TransposedNode transposeAnonymousNode(
        TransposedNode parentNode, 
        Link link, 
        AnonymousNode node, int rowIndex
    );
    
    public TransposedNode transposeCellNode(
        TransposedNode parentNode, 
        Link link, 
        CellNode node, 
        int rowIndex,
        Cell cell
    );
    
    public TransposedNode transposeValueNode(
        TransposedNode parentNode, 
        Link link, 
        ValueNode node, 
        int rowIndex
    );
    
    public TransposedNode transposeTopicNode(
        TransposedNode parentNode, 
        Link link, 
        FreebaseTopicNode node, 
        int rowIndex
    );
    
    public void flush() throws IOException;
}
