package edu.mit.simile.vicino.vptree;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import edu.mit.simile.vicino.Distance;

/**
 * @author Paolo Ciccarese
 * @author Stefano Mazzocchi
 */
public class VPTreeBuilder {

    private static final boolean DEBUG = false;

    private Random generator = new Random(System.currentTimeMillis());

    private VPTree tree;
    private final Distance distance;

    /**
     * Defines a VPTree Builder for a specific distance.
     * 
     * @param distance The class implementing the distance.
     */
    public VPTreeBuilder(Distance distance) {
        this.distance = distance;
    }

    public VPTree buildVPTree(Collection<? extends Serializable> col) {
        Node nodes[] = new Node[col.size()];
        Iterator<? extends Serializable> i = col.iterator();
        int counter = 0;
        while (i.hasNext()) {
            Serializable s = (Serializable) i.next();
            nodes[counter++] = new Node(s);
        }

        tree = new VPTree();
        tree.setRoot(addNode(nodes, 0, nodes.length - 1));
        return tree;
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
            float d = (x == y) ? 0.0f : distance.d(x.toString(), y.toString());
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
