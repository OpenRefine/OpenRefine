package edu.mit.simile.vicino.vptree;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import edu.mit.simile.vicino.distances.Distance;

/**
 * @author Paolo Ciccarese
 * @author Stefano Mazzocchi
 */
public class VPTreeBuilder {

    private static final boolean DEBUG = false;

    private Random generator = new Random(System.currentTimeMillis());

    private final Distance distance;

    private Set<Node> nodes = new HashSet<Node>();
    
    /**
     * Defines a VPTree Builder for a specific distance.
     * 
     * @param distance The class implementing the distance.
     */
    public VPTreeBuilder(Distance distance) {
        this.distance = distance;
    }

    public Set<Node> getNodes() {
        return this.nodes;
    }
    
    public void populate(Serializable s) {
        nodes.add(new Node(s));
    }

    public VPTree buildVPTree() {
        if (DEBUG) {
            for (Node n : this.nodes) {
                System.out.println(n.get().toString());
            }
            System.out.println();
        }
        Node[] nodes_array = this.nodes.toArray(new Node[this.nodes.size()]);
        VPTree tree = new VPTree();
        if (nodes_array.length > 0) {
            tree.setRoot(makeNode(nodes_array, 0, nodes_array.length-1));
        }
        return tree;
    }

    public VPTree buildVPTree(Collection<? extends Serializable> values) {
        reset();
        for (Serializable s : values) {
            populate(s);
        }
        return buildVPTree();
    }
    
    public void reset() {
        this.nodes.clear();
    }
    
    private TNode makeNode(Node nodes[], int begin, int end) {

        int delta = end - begin;

        if (DEBUG) System.out.println("\ndelta: " + delta);
        
        if (delta == 0) {
            TNode vpNode = new TNode(nodes[begin].get());
            vpNode.setMedian(0);
            return vpNode;
        } else if(delta < 0) {
            return null;
        }
                
        Node randomNode = nodes[begin + getRandomIndex(delta)];
        TNode vpNode = new TNode(randomNode.get());
    
        if (DEBUG) System.out.println("\nvp-node: " + vpNode.get().toString());
        
        calculateDistances (vpNode , nodes, begin, end);
        orderDistances (nodes, begin, end);
        fixVantagPoint (randomNode , nodes, begin, end);
        
        if (DEBUG) {
            for (int i = begin; i <= end; i++) {
                System.out.println(" +-- " + nodes[i].getDistance() + " --> " + nodes[i].get());
            }
        }
        
        float median = (float) median(nodes, begin, end);
        vpNode.setMedian(median);
        
        int i = 0;
        for (i = begin + 1; i < end; i++) {
            if (nodes[i].getDistance() >= median) {
                vpNode.setLeft(makeNode(nodes, begin+1, i-1));
                break;
            }
        }
        vpNode.setRight(makeNode(nodes, i, end));
        
        return vpNode;
    }
    
    public double median(Node nodes[], int begin, int end) {
        int middle = (end-begin) / 2;  // subscript of middle element
        
        if ((end-begin) % 2 == 0) {
            return nodes[begin+middle].getDistance();
        } else {
           return (nodes[begin+middle].getDistance() + nodes[begin+middle+1].getDistance()) / 2.0d;
        }
    }
    
    private void calculateDistances(TNode pivot, Node nodes[], int begin, int end) {
        for (int i = begin; i <= end; i++) {
            Serializable x = pivot.get();
            Serializable y = nodes[i].get();
            double d = (x == y) ? 0.0d : distance.d(x.toString(), y.toString());
            nodes[i].setDistance(d);
        }
    }
    
    private void fixVantagPoint (Node pivot, Node nodes[], int begin, int end) {
        for (int i = begin; i < end; i++) {
            if (nodes[i] == pivot) {
                if (i > begin) {
                    Node tmp = nodes[begin];
                    nodes[begin] = pivot;
                    nodes[i] = tmp;
                    break;
                } 
            }
        }
    }
    
    private void orderDistances(Node nodes[], int begin, int end) {
        NodeSorter.sort(nodes, begin, end);
    }

    private int getRandomIndex(int max) {
        return generator.nextInt(max);
    }
}
