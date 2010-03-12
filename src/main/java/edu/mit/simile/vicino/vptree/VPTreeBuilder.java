package edu.mit.simile.vicino.vptree;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    
    public Map<Serializable,Set<Serializable>> getClusters(double radius) {
        VPTree tree = buildVPTree();
        
        if (DEBUG) {
            System.out.println();
            printNode(tree.getRoot(),0);
            System.out.println();
        }
        
        VPTreeSeeker seeker = new VPTreeSeeker(distance,tree);
        Map<Serializable,Boolean> flags = new HashMap<Serializable,Boolean>();
        for (Node n : nodes) {
            flags.put(n.get(), true);
        }
        
        Map<Serializable,Set<Serializable>> map = new HashMap<Serializable,Set<Serializable>>();
        for (Node n : nodes) {
            Serializable s = n.get();
            if (flags.get(s)) {
                Set<Serializable> results = seeker.range(s, radius);
                results.add(s);
                for (Serializable ss : results) {
                    flags.put(ss, false);
                }
                map.put(s, results);
            }
        }
        
        return map;
    }

    private void printNode(TNode node, int level) {
        if (node != null) {
            if (DEBUG) System.out.println(indent(level++) + node.get() + " [" + node.getMedian() + "]");
            printNode(node.getLeft(),level);
            printNode(node.getRight(),level);
        }
    }
    
    private String indent(int i) {
        StringBuffer b = new StringBuffer();
        for (int j = 0; j < i; j++) {
            b.append(' ');
        }
        return b.toString();
    }
    
    private TNode makeNode(Node nodes[], int begin, int end) {

        int delta = end - begin;
        int middle = begin + (delta / 2);

        if (DEBUG) System.out.println("\ndelta: " + delta);
        
        TNode vpNode = new TNode(nodes[begin + getRandomIndex(delta)].get());

        if (DEBUG) System.out.println("\nvp-node: " + vpNode.get().toString());

        calculateDistances(vpNode, nodes, begin, end);
        orderDistances(nodes, begin, end);

        if (DEBUG) {
            System.out.println("delta: " + delta);
            System.out.println("middle: " + middle);
            for (int i = begin; i <= end; i++) {
                System.out.println(" +-- " + nodes[i].getDistance() + " --> " + nodes[i].get());
            }
        }
        
        TNode node = new TNode(nodes[middle].get());
        node.setMedian(nodes[middle].getDistance());
               
        if (DEBUG) System.out.println("\n-node: " + node.get().toString());
               
        if ((middle-1)-begin > 0) {
            node.setLeft(makeNode(nodes, begin, middle-1));
        } else if ((middle-1)-begin == 0) {
           TNode nodeLeft = new TNode(nodes[begin].get());
           nodeLeft.setMedian(nodes[begin].getDistance());
           node.setLeft(nodeLeft);
        }
        
        if (end-(middle+1) > 0) {
            node.setRight(makeNode(nodes, middle+1, end));
        } else if (end-(middle+1) == 0) {
            TNode nodeRight = new TNode(nodes[end].get());
            nodeRight.setMedian(nodes[end].getDistance());
            node.setRight(new TNode(nodes[end].get()));
        }

        return node;
    }

    private void calculateDistances(TNode pivot, Node nodes[], int begin, int end) {
        for (int i = begin; i <= end; i++) {
            Serializable x = pivot.get();
            Serializable y = nodes[i].get();
            double d = (x == y) ? 0.0d : distance.d(x.toString(), y.toString());
            nodes[i].setDistance(d);
        }
    }
    
    private void orderDistances(Node nodes[], int begin, int end) {
        NodeSorter.sort(nodes, begin, end);
    }

    private int getRandomIndex(int max) {
        return generator.nextInt(max);
    }
}
