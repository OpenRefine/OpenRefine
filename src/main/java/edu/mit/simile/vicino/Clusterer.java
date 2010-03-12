package edu.mit.simile.vicino;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.metaweb.gridworks.clustering.knn.NGramTokenizer;
import com.wcohen.ss.api.Token;
import com.wcohen.ss.tokens.SimpleTokenizer;

import edu.mit.simile.vicino.distances.Distance;
import edu.mit.simile.vicino.vptree.VPTreeBuilder;

public class Clusterer extends Operator {

    public class SizeComparator implements Comparator<Set<Serializable>> {
        public int compare(Set<Serializable> o1, Set<Serializable> o2) {
            return o2.size() - o1.size();
        }
    }

    public static void main(String[] args) throws Exception {
        (new Clusterer()).init(args);
    }
    
    public void init(String[] args) throws Exception {
        Distance distance = getDistance(args[0]);
        List<String> strings = getStrings(args[1]);
        double radius = Double.parseDouble(args[2]);
        int blocking_size = Integer.parseInt(args[3]);

        vptree(strings, radius, distance);
        ngram_blocking(strings, radius, distance, blocking_size);
    }
    
    public void vptree(List<String> strings, double radius, Distance distance) {
        long start = System.currentTimeMillis();
        
        VPTreeBuilder treeBuilder = new VPTreeBuilder(distance);
        for (String s : strings) {
            treeBuilder.populate(s);
        }
        Map<Serializable,Set<Serializable>> cluster_map = treeBuilder.getClusters(radius);
        List<Set<Serializable>> clusters = new ArrayList<Set<Serializable>>(cluster_map.values());
        Collections.sort(clusters, new SizeComparator());
        
        System.out.println("Calculated " + distance.getCount() + " distances.");
        
        distance.resetCounter();
        
        int found = 0;
        
        for (Set<Serializable> m : clusters) {
            if (m.size() > 1) {
                found++;
                for (Serializable s : m) {
                    System.out.println(s);
                }
                System.out.println();
            }
        }
        
        long stop = System.currentTimeMillis();
        
        System.out.println("Found " + found + " clusters in " + (stop - start) + " ms");
    }
    
    public void ngram_blocking(List<String> strings, double radius, Distance distance, int blockSize) {
        long start = System.currentTimeMillis();
        
        System.out.println("block size: " + blockSize);
        
        NGramTokenizer tokenizer = new NGramTokenizer(blockSize,blockSize,false,SimpleTokenizer.DEFAULT_TOKENIZER);

        Map<String,Set<String>> blocks = new HashMap<String,Set<String>>();
        
        for (String s : strings) {
            Token[] tokens = tokenizer.tokenize(s);
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

        int block_count = 0;
                    
        Map<Serializable,Set<Serializable>> cluster_map = new HashMap<Serializable,Set<Serializable>>();
        
        for (Set<String> list : blocks.values()) {
            if (list.size() < 2) continue;
            block_count++;
            for (String a : list) {
                for (String b : list) {
                    if (a == b) continue;
                    if (cluster_map.containsKey(a) && cluster_map.get(a).contains(b)) continue;
                    if (cluster_map.containsKey(b) && cluster_map.get(b).contains(a)) continue;
                    double d = distance.d(a,b);
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
        
        System.out.println("Calculated " + distance.getCount() + " distances in " + block_count + " blocks.");
        
        distance.resetCounter();

        List<Set<Serializable>> clusters = new ArrayList<Set<Serializable>>(cluster_map.values());
        Collections.sort(clusters, new SizeComparator());
        
        int found = 0;
        
        for (Set<Serializable> m : clusters) {
            if (m.size() > 1) {
                found++;
                for (Serializable s : m) {
                    System.out.println(s);
                }
                System.out.println();
            }
        }
        
        long stop = System.currentTimeMillis();
        
        System.out.println("Found " + found + " clusters in " + (stop - start) + " ms");
    }
}
