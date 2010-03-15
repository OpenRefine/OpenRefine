package edu.mit.simile.vicino.clustering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.wcohen.ss.api.Token;

import edu.mit.simile.vicino.NGramTokenizer;
import edu.mit.simile.vicino.distances.Distance;

public class NGramClusterer extends Clusterer {
    
    NGramTokenizer _tokenizer;
    Distance _distance;
    
    Map<String,Set<String>> blocks = new HashMap<String,Set<String>>();
    
    public NGramClusterer(Distance d, int blockSize) {
        _tokenizer = new NGramTokenizer(blockSize);
        _distance = d;
    }
    
    public void populate(String s) {
        Token[] tokens = _tokenizer.tokenize(s);
        for (Token t : tokens) {
            String ss = t.getValue();
            Set<String> l = null;
            if (!blocks.containsKey(ss)) {
                l = new TreeSet<String>(); 
                blocks.put(ss, l);
            } else {
                l = blocks.get(ss);
            }
            l.add(s);
        }
    }
    
    public List<Set<Serializable>> getClusters(double radius) {
    
        Map<Serializable,Set<Serializable>> cluster_map = new HashMap<Serializable,Set<Serializable>>();
        
        for (Set<String> set : blocks.values()) {
            if (set.size() < 2) continue;
            for (String a : set) {
                for (String b : set) {
                    if (a == b) continue;
                    if (cluster_map.containsKey(a) && cluster_map.get(a).contains(b)) continue;
                    if (cluster_map.containsKey(b) && cluster_map.get(b).contains(a)) continue;
                    double d = _distance.d(a,b);
                    if (d <= radius || radius < 0) {
                        Set<Serializable> l = null; 
                        if (!cluster_map.containsKey(a)) {
                            l = new TreeSet<Serializable>();
                            l.add(a);
                            cluster_map.put(a, l);
                        } else {
                            l = cluster_map.get(a);
                        }
                        l.add(b);
                    }
                }
            }
        }
        
        List<Set<Serializable>> clusters = new ArrayList<Set<Serializable>>();
        for (Entry<Serializable,Set<Serializable>> e : cluster_map.entrySet()) {
            Set<Serializable> v = e.getValue();
            if (v.size() > 1) {
                clusters.add(v);
            }
        }
        Collections.sort(clusters, new SizeComparator());
        
        return clusters;
    }
    
}
