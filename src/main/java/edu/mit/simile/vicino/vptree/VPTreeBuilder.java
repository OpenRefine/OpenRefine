package edu.mit.simile.vicino.vptree;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.metaweb.gridworks.Gridworks;

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
        Node[] nodes_array = this.nodes.toArray(new Node[this.nodes.size()]);
        VPTree tree = new VPTree();
        tree.setRoot(addNode(nodes_array, 0, nodes_array.length - 1));
        Gridworks.log("Built vptree with " + nodes_array.length + " nodes");
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
    
    public Map<Serializable,List<Serializable>> getClusters(double radius) {
        VPTree tree = buildVPTree();
        VPTreeSeeker seeker = new VPTreeSeeker(distance,tree);
        
        Map<Serializable,List<Serializable>> map = new HashMap<Serializable,List<Serializable>>();
        for (Node n : nodes) {
            Serializable s = n.get();
            List<Serializable> results = seeker.range(s, radius);
            map.put(s, results);
        }
        
        return map;
    }
    
    private TNode addNode(Node nodes[], int begin, int end) {

        int delta = end - begin;
        int middle = begin + delta / 2;

        TNode node = new TNode(nodes[begin + getRandomIndex(delta)].get());

        if (DEBUG) System.out.println("\nnode: " + node.get().toString());

        calculateDistances(node, nodes, begin, end);
        orderDistances(nodes, begin, end);

        if (DEBUG) {
            for (int i = begin; i <= end; i++) {
                System.out.println(" +-- " + nodes[i].getDistance() + " --> " + nodes[i].get());
            }
        }

        if (delta + 1 > 0) {
            if (middle - (begin + 1) >= 1) {
                node.setLeft(addNode(nodes, begin + 1, middle));
                if (DEBUG) System.out.println(" L --> " + node.getLeft().get());
            } else if (middle - (begin + 1) == 0) {
                node.setLeft(new TNode(nodes[middle].get()));
                if (DEBUG) System.out.println(" L --> " + node.getLeft().get());
            }

            if ((end - (middle + 1)) >= 1) {
                node.setRight(addNode(nodes, middle + 1, end));
                if (DEBUG) System.out.println(" R --> " + node.getRight().get());
            } else if (end - (middle + 1) == 0) {
                node.setRight(new TNode(nodes[middle + 1].get()));
                if (DEBUG) System.out.println(" R --> " + node.getRight().get());
            }
        }

        return node;
    }

    private void calculateDistances(TNode pivot, Node nodes[], int begin, int end) {
        for (int i = begin; i <= end; i++) {
            Object x = pivot.get();
            Object y = nodes[i].get();
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
