package edu.mit.simile.vicino.vptree;

import java.io.Serializable;

/**
 * The VPTree class.
 * 
 * @author Paolo Ciccarese
 */
public class VPTree implements Serializable {

    private static final long serialVersionUID = 1291056732155841123L;

    private TNode root;

    /**
     * Sets the root of the VPTree.
     * 
     * @param root The VPTree root.
     */
    public void setRoot(TNode root) {
        this.root = root;
    }

    /**
     * Get the root of the VPTree.
     * 
     * @return The VPTree root.
     */
    public TNode getRoot() {
        return root;
    }
}
