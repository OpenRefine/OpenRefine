package edu.mit.simile.vicino;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import edu.mit.simile.vicino.clustering.Clusterer;
import edu.mit.simile.vicino.clustering.NGramClusterer;
import edu.mit.simile.vicino.clustering.VPTreeClusterer;
import edu.mit.simile.vicino.distances.Distance;

public class Cluster extends Operator {

    public static void main(String[] args) throws Exception {
        (new Cluster()).init(args);
    }
    
    public void init(String[] args) throws Exception {
        Distance distance = getDistance(args[0]);
        List<String> strings = getStrings(args[1]);
        double radius = Double.parseDouble(args[2]);
        int blocking_size = Integer.parseInt(args[3]);

        long vptree_start = System.currentTimeMillis();
        Clusterer vptree_clusterer = new VPTreeClusterer(distance);
        for (String s: strings) {
            vptree_clusterer.populate(s);
        }
        List<Set<Serializable>> vptree_clusters = vptree_clusterer.getClusters(radius);
        long vptree_elapsed = System.currentTimeMillis() - vptree_start;
        int vptree_distances = distance.getCount();
        distance.resetCounter();
        
        long ngram_start = System.currentTimeMillis();
        Clusterer ngram_clusterer = new NGramClusterer(distance,blocking_size);
        for (String s: strings) {
            ngram_clusterer.populate(s);
        }
        List<Set<Serializable>> ngram_clusters = ngram_clusterer.getClusters(radius);
        long ngram_elapsed = System.currentTimeMillis() - ngram_start;
        int ngram_distances = distance.getCount();
        distance.resetCounter();
        
        log("VPTree found " + vptree_clusters.size() + " in " + vptree_elapsed + " ms with " + vptree_distances + " distances\n");
        for (Set<Serializable> s : vptree_clusters) {
            for (Serializable ss : s) {
                log(" " + ss);
            }
            log("");
        }

        log("NGram  found " + ngram_clusters.size() + " in " + ngram_elapsed + " ms with " + ngram_distances + " distances\n");
        for (Set<Serializable> s : ngram_clusters) {
            for (Serializable ss : s) {
                log(" " + ss);
            }
            log("");
        }
        
    }
}
