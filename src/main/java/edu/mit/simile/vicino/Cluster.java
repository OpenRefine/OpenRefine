package edu.mit.simile.vicino;

import java.io.Serializable;
import java.util.HashSet;
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
        log("NGram  found " + ngram_clusters.size() + " in " + ngram_elapsed + " ms with " + ngram_distances + " distances\n");
        
        if (vptree_clusters.size() > ngram_clusters.size()) {
            log("VPTree clusterer found these clusters the other method couldn't: ");
            diff(vptree_clusters,ngram_clusters);
        } else if (ngram_clusters.size() > vptree_clusters.size()) {
            log("NGram clusterer found these clusters the other method couldn't: ");
            diff(ngram_clusters,vptree_clusters);
        }
    }
    
    private void diff(List<Set<Serializable>> more, List<Set<Serializable>> base) {
        Set<Set<Serializable>> holder = new HashSet<Set<Serializable>>(base.size());
        
        for (Set<Serializable> s : base) {
            holder.add(s);
        }
        
        for (Set<Serializable> s : more) {
            if (!holder.contains(s)) {
                for (Serializable ss : s) {
                    log(ss.toString());
                }
                log("");
            }
        }
    }
}
