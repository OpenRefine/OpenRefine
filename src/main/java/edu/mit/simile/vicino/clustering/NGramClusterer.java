package edu.mit.simile.vicino.clustering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    
    public class BlockEvaluator implements Callable<Map<Serializable,Set<Serializable>>> {

        int start;
        int stop;
        double radius;
        
        List<Set<String>> blocks;
        Map<Serializable,Set<Serializable>> cluster_map;
        
        public BlockEvaluator(List<Set<String>> blocks, double radius, int start, int stop) {
            this.blocks = blocks;
            this.start = start;
            this.stop = stop;
            this.radius = radius;
        }
        
        public Map<Serializable,Set<Serializable>> call() {
            Map<Serializable,Set<Serializable>> cluster_map = new HashMap<Serializable,Set<Serializable>>();

            for (int i = start; i < stop; i++) {
                Set<String> set = blocks.get(i);
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
            
            return cluster_map;
        }
    }

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private static final boolean MULTITHREADED = true;
    
    public List<Set<Serializable>> getClusters(double radius) {
        if (MULTITHREADED) {
            return getClustersMultiThread(radius);
        } else {
            return getClustersSingleThread(radius);
        }
    }
    
    public List<Set<Serializable>> getClustersMultiThread(double radius) {

        int cores = Runtime.getRuntime().availableProcessors();
        int size = blocks.size();
        int range = size / cores + 1;

        List<Map<Serializable,Set<Serializable>>> cluster_maps = new ArrayList<Map<Serializable,Set<Serializable>>>(cores);
        
        List<BlockEvaluator> evaluators = new ArrayList<BlockEvaluator>(cores);
        for (int i = 0; i < cores; i++) {
            int range_start = range * i;
            int range_end = range * (i + 1);
            if (range_end > size) range_end = size;
            evaluators.add(new BlockEvaluator(new ArrayList<Set<String>>(blocks.values()),radius,range_start,range_end));
        }
        
        try {
            List<Future<Map<Serializable,Set<Serializable>>>> futures = executor.invokeAll(evaluators);
            for (Future<Map<Serializable,Set<Serializable>>> future : futures) {  
                cluster_maps.add(future.get());
            }  
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        Set<Set<Serializable>> clusters = new HashSet<Set<Serializable>>();
        
        for (Map<Serializable,Set<Serializable>> cluster_map : cluster_maps) {
            for (Entry<Serializable,Set<Serializable>> e : cluster_map.entrySet()) {
                Set<Serializable> v = e.getValue();
                if (v.size() > 1) {
                    clusters.add(v);
                }
            }
        }
        
        List<Set<Serializable>> sorted_clusters = new ArrayList<Set<Serializable>>(clusters);
        
        Collections.sort(sorted_clusters, new SizeComparator());
        
        return sorted_clusters;
    }
    
    public List<Set<Serializable>> getClustersSingleThread(double radius) {
    
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
