package com.google.gridworks.protograph.transpose;

import java.io.IOException;

import com.google.gridworks.model.Cell;
import com.google.gridworks.protograph.AnonymousNode;
import com.google.gridworks.protograph.CellNode;
import com.google.gridworks.protograph.FreebaseTopicNode;
import com.google.gridworks.protograph.Link;
import com.google.gridworks.protograph.ValueNode;

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
        int cellIndex,
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
