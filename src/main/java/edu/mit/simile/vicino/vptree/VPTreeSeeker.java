package edu.mit.simile.vicino.vptree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.mit.simile.vicino.distances.Distance;

/**
 * @author Paolo Ciccarese
 */
public class VPTreeSeeker {

    VPTree tree;
    Distance distance;

    public VPTreeSeeker(Distance distance, VPTree tree) {
        this.distance = distance;
        this.tree = tree;
    }

    public List<Serializable> range(Serializable query, double range) {
        return rangeTraversal(query, range, tree.getRoot(), new ArrayList<Serializable>());
    }

    private List<Serializable> rangeTraversal(Serializable query, double range, TNode tNode, List<Serializable> results) {

        if (tNode != null) {
            double distance = this.distance.d(query.toString(), tNode.get().toString());

            if (distance < range) {
                results.add(tNode.get());
            }

            if ((distance + range) < tNode.getMedian()) {
                rangeTraversal(query, range, tNode.getLeft(), results);
            } else if ((distance - range) > tNode.getMedian()) {
                rangeTraversal(query, range, tNode.getRight(), results);
            } else {
                rangeTraversal(query, range, tNode.getLeft(), results);
                rangeTraversal(query, range, tNode.getRight(), results);
            }
        }

        return results;
    }

}
