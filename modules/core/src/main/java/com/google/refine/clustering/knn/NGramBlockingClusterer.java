package com.google.refine.clustering.knn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replacement for Vicino's NGramClusterer. Groups strings by shared n-grams (blocking), then computes pairwise
 * distances within each block and transitively merges strings within the given radius.
 */
public class NGramBlockingClusterer {

    private static final Logger logger = LoggerFactory.getLogger("NGramBlockingClusterer");

    private final SimilarityDistance _distance;
    private final int _ngramSize;
    private final Map<String, Set<String>> _blocks = new HashMap<>();

    public NGramBlockingClusterer(SimilarityDistance distance, int ngramSize) {
        _distance = distance;
        _ngramSize = ngramSize;
    }

    /**
     * Add a string to the blocking index. It will be tokenized into character n-grams and added to each corresponding
     * block.
     */
    public void populate(String s) {
        List<String> ngrams = tokenize(s, _ngramSize);
        for (String ngram : ngrams) {
            _blocks.computeIfAbsent(ngram, k -> new HashSet<>()).add(s);
        }
    }

    /**
     * Compute clusters. Strings within the same block whose pairwise distance is &le; radius are transitively merged.
     *
     * @param radius
     *            maximum distance for two strings to be considered similar
     * @param cancelChecker
     *            returns true if computation should be cancelled; may be null
     * @param progressCallback
     *            receives progress 0..100 for the getClusters phase; may be null
     * @return list of clusters (each cluster is a set of similar strings)
     */
    public List<Set<Serializable>> getClusters(double radius, Supplier<Boolean> cancelChecker,
            java.util.function.IntConsumer progressCallback) {
        // Union-Find structure
        Map<String, String> parent = new HashMap<>();

        List<Set<String>> blockValues = new ArrayList<>(_blocks.values());
        int totalBlocks = blockValues.size();
        int processedBlocks = 0;

        for (Set<String> block : blockValues) {
            if (cancelChecker != null && cancelChecker.get()) {
                return new ArrayList<>();
            }

            String[] items = block.toArray(new String[0]);
            for (int i = 0; i < items.length; i++) {
                for (int j = i + 1; j < items.length; j++) {
                    if (cancelChecker != null && cancelChecker.get()) {
                        return new ArrayList<>();
                    }
                    double d = _distance.compute(items[i], items[j]);
                    if (d <= radius) {
                        union(parent, items[i], items[j]);
                    }
                }
            }

            processedBlocks++;
            if (progressCallback != null) {
                progressCallback.accept((int) ((processedBlocks * 100L) / totalBlocks));
            }
        }

        // Collect clusters by root
        Map<String, Set<Serializable>> clusterMap = new HashMap<>();
        for (String s : parent.keySet()) {
            String root = find(parent, s);
            clusterMap.computeIfAbsent(root, k -> new HashSet<>()).add(s);
        }
        // Also add strings that were never unioned (singletons in parent map are already roots)
        // We need to ensure every string that was populated appears
        Set<String> allStrings = new HashSet<>();
        for (Set<String> block : _blocks.values()) {
            allStrings.addAll(block);
        }
        for (String s : allStrings) {
            if (!parent.containsKey(s)) {
                // Singleton — not in any union, skip (only return clusters with >1 element)
                continue;
            }
            String root = find(parent, s);
            clusterMap.computeIfAbsent(root, k -> new HashSet<>()).add(s);
        }

        return new ArrayList<>(clusterMap.values());
    }

    /**
     * Convenience overload without cancellation or progress.
     */
    public List<Set<Serializable>> getClusters(double radius) {
        return getClusters(radius, null, null);
    }

    private static String find(Map<String, String> parent, String s) {
        // Path compression
        String root = s;
        while (parent.containsKey(root) && !parent.get(root).equals(root)) {
            root = parent.get(root);
        }
        // Compress path
        String current = s;
        while (!current.equals(root)) {
            String next = parent.get(current);
            parent.put(current, root);
            current = next;
        }
        return root;
    }

    private static void union(Map<String, String> parent, String a, String b) {
        String rootA = find(parent, a);
        String rootB = find(parent, b);
        if (!rootA.equals(rootB)) {
            parent.put(rootA, rootB);
        }
        // Ensure both are in the map
        parent.putIfAbsent(a, a);
        parent.putIfAbsent(b, b);
    }

    static List<String> tokenize(String s, int ngramSize) {
        List<String> ngrams = new ArrayList<>();
        // Normalize: lowercase and collapse whitespace (match Vicino behavior)
        s = s.toLowerCase().replaceAll("\\s+", " ").trim();
        if (s.length() < ngramSize) {
            ngrams.add(s);
            return ngrams;
        }
        for (int i = 0; i <= s.length() - ngramSize; i++) {
            ngrams.add(s.substring(i, i + ngramSize));
        }
        return ngrams;
    }
}
