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
    private static final boolean OPTIMIZED = false;
    private static final int sample_size = 10;

    private Random generator = new Random(System.currentTimeMillis());

    private final Distance distance;

    private Set<Node> nodes = new HashSet<Node>();

    /**
     * Defines a VPTree Builder for a specific distance.
     * 
     * @param distance
     *            The class implementing the distance.
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
            tree.setRoot(makeNode(nodes_array, 0, nodes_array.length - 1));
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
        } else if (delta < 0) {
            return null;
        }

        Node randomNode = getVantagePoint(nodes, begin, end);
        TNode vpNode = new TNode(randomNode.get());
        
        if (DEBUG) System.out.println("\nvp-node: " + vpNode.get().toString());

        calculateDistances(vpNode, nodes, begin, end);
        orderDistances(nodes, begin, end);
        fixVantagPoint(randomNode, nodes, begin, end);

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
                vpNode.setLeft(makeNode(nodes, begin + 1, i - 1));
                break;
            }
        }
        vpNode.setRight(makeNode(nodes, i, end));

        return vpNode;
    }

    private Node getVantagePoint(Node nodes[], int begin, int end) {
        if (OPTIMIZED) {
            Node buffer[] = new Node[sample_size];
            for (int i = 0; i < sample_size; i++) {
                buffer[i] = getRandomNode(nodes,begin,end); 
            }
    
            double bestSpread = 0;
            Node bestNode = buffer[0];
            for (int i = 0; i < sample_size; i++) {
                calculateDistances(new TNode(buffer[i]), buffer, 0, buffer.length - 1);
                orderDistances(nodes, begin, end);
                double median = (double) median(nodes, begin, end);
                double spread = deviation(buffer, median);
                System.out.println(" " + spread);
                if (spread > bestSpread) {
                    bestSpread = spread;
                    bestNode = buffer[i];
                }
            }
    
            System.out.println("best: " + bestSpread);
            return bestNode;
        } else {
            return getRandomNode(nodes,begin,end);            
        }
    }

    private Node getRandomNode(Node nodes[], int begin, int end) {
        return nodes[begin + generator.nextInt(end - begin)];        
    }
    
    private double deviation(Node buffer[], double median) {
        double sum = 0;
        for (int i = 0; i < buffer.length; i++) {
            sum += Math.pow(buffer[i].getDistance() - median, 2);
        }
        return sum / buffer.length;
    }

    public double median(Node nodes[], int begin, int end) {
        int delta = end - begin;
        int middle = delta / 2; 

        if (delta % 2 == 0) {
            return nodes[begin + middle].getDistance();
        } else {
            return (nodes[begin + middle].getDistance() + nodes[begin + middle + 1].getDistance()) / 2.0d;
        }
    }

    private void calculateDistances(TNode pivot, Node nodes[], int begin, int end) {
        Serializable x = pivot.get();
        for (int i = begin; i <= end; i++) {
            Serializable y = nodes[i].get();
            double d = (x == y || x.equals(y)) ? 0.0d : distance.d(x.toString(), y.toString());
            nodes[i].setDistance(d);
        }
    }

    private void fixVantagPoint(Node pivot, Node nodes[], int begin, int end) {
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
}
