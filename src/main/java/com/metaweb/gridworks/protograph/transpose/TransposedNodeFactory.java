package com.metaweb.gridworks.protograph.transpose;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.protograph.AnonymousNode;
import com.metaweb.gridworks.protograph.CellNode;
import com.metaweb.gridworks.protograph.FreebaseProperty;
import com.metaweb.gridworks.protograph.FreebaseTopicNode;
import com.metaweb.gridworks.protograph.ValueNode;

public interface TransposedNodeFactory {
    public TransposedNode transposeAnonymousNode(
        TransposedNode parentNode, 
        FreebaseProperty property, 
        AnonymousNode node
    );
    
    public TransposedNode transposeCellNode(
        TransposedNode parentNode, 
        FreebaseProperty property, 
        CellNode node, 
        Cell cell
    );
    
    public TransposedNode transposeValueNode(
        TransposedNode parentNode, 
        FreebaseProperty property, 
        ValueNode node
    );
    
    public TransposedNode transposeTopicNode(
        TransposedNode parentNode, 
        FreebaseProperty property, 
        FreebaseTopicNode node
    );
}
