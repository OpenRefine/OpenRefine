package edu.mit.simile.vicino.clustering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.simile.vicino.distances.Distance;
import edu.mit.simile.vicino.vptree.Node;
import edu.mit.simile.vicino.vptree.VPTree;
import edu.mit.simile.vicino.vptree.VPTreeBuilder;
import edu.mit.simile.vicino.vptree.VPTreeSeeker;

public class VPTreeClusterer extends Clusterer {

    VPTreeBuilder _treeBuilder;
    Distance _distance;
    
    public VPTreeClusterer(Distance d) {
        _distance = d;
        _treeBuilder = new VPTreeBuilder(d);
    }
    
    public void populate(String s) {
        _treeBuilder.populate(s);
    }
    
    public List<Set<Serializable>> getClusters(double radius) {
        VPTree tree = _treeBuilder.buildVPTree();
        Set<Node> nodes = _treeBuilder.getNodes();
        
        VPTreeSeeker seeker = new VPTreeSeeker(_distance,tree);
        Map<Serializable,Boolean> flags = new HashMap<Serializable,Boolean>();
        for (Node n : nodes) {
            flags.put(n.get(), true);
        }
        
        Map<Serializable,Set<Serializable>> map = new HashMap<Serializable,Set<Serializable>>();
        for (Node n : nodes) {
            Serializable s = n.get();
            if (flags.get(s)) {
                Set<Serializable> results = seeker.range(s, radius);
                for (Serializable ss : results) {
                    flags.put(ss, false);
                }
                if (results.size() > 1) {
                    map.put(s, results);
                }
            }
        }
        
        List<Set<Serializable>> clusters = new ArrayList<Set<Serializable>>(map.values());
        Collections.sort(clusters, new SizeComparator());
        
        return clusters;
    }
    
    
}
