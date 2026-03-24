package com.google.refine.clustering.knn;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

public class NGramBlockingClustererTests {

    @Test
    public void testBasicClustering() {
        SimilarityDistance levenshtein = (a, b) -> {
            // Simple Levenshtein for testing
            int[][] dp = new int[a.length() + 1][b.length() + 1];
            for (int i = 0; i <= a.length(); i++)
                dp[i][0] = i;
            for (int j = 0; j <= b.length(); j++)
                dp[0][j] = j;
            for (int i = 1; i <= a.length(); i++)
                for (int j = 1; j <= b.length(); j++)
                    dp[i][j] = Math.min(dp[i - 1][j] + 1,
                            Math.min(dp[i][j - 1] + 1,
                                    dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1)));
            return dp[a.length()][b.length()];
        };

        NGramBlockingClusterer clusterer = new NGramBlockingClusterer(levenshtein, 2);
        clusterer.populate("new york");
        clusterer.populate("new yrok");
        clusterer.populate("boston");

        List<Set<Serializable>> clusters = clusterer.getClusters(2.0);

        // "new york" and "new yrok" should cluster (distance 2 with transposition)
        boolean foundCluster = false;
        for (Set<Serializable> cluster : clusters) {
            if (cluster.contains("new york") && cluster.contains("new yrok")) {
                foundCluster = true;
                assertEquals(cluster.size(), 2);
            }
        }
        assertTrue(foundCluster, "Expected cluster containing 'new york' and 'new yrok'");
    }

    @Test
    public void testNoLonelyClusters() {
        SimilarityDistance alwaysFar = (a, b) -> 100.0;

        NGramBlockingClusterer clusterer = new NGramBlockingClusterer(alwaysFar, 2);
        clusterer.populate("foo");
        clusterer.populate("bar");

        List<Set<Serializable>> clusters = clusterer.getClusters(1.0);
        // With distance always 100, nothing clusters within radius 1
        for (Set<Serializable> cluster : clusters) {
            assertTrue(cluster.size() <= 1, "Expected no multi-element clusters");
        }
    }

    @Test
    public void testCancellation() {
        SimilarityDistance slow = (a, b) -> 1.0;

        NGramBlockingClusterer clusterer = new NGramBlockingClusterer(slow, 2);
        for (int i = 0; i < 100; i++) {
            clusterer.populate("string" + i + "abcdef");
        }

        AtomicBoolean cancelled = new AtomicBoolean(false);
        cancelled.set(true); // Cancel immediately

        List<Set<Serializable>> clusters = clusterer.getClusters(1.0, cancelled::get, null);
        assertTrue(clusters.isEmpty(), "Expected empty result on cancellation");
    }

    @Test
    public void testProgressCallback() {
        SimilarityDistance dist = (a, b) -> Math.abs(a.length() - b.length());

        NGramBlockingClusterer clusterer = new NGramBlockingClusterer(dist, 2);
        clusterer.populate("ab");
        clusterer.populate("abc");
        clusterer.populate("abcd");

        AtomicInteger lastProgress = new AtomicInteger(0);
        clusterer.getClusters(1.0, null, lastProgress::set);

        assertTrue(lastProgress.get() > 0, "Progress callback should have been invoked");
    }

    @Test
    public void testTokenize() {
        List<String> ngrams = NGramBlockingClusterer.tokenize("Hello", 2);
        assertEquals(ngrams.size(), 4); // "he", "el", "ll", "lo"
        assertEquals(ngrams.get(0), "he");
        assertEquals(ngrams.get(3), "lo");
    }

    @Test
    public void testTokenizeShortString() {
        List<String> ngrams = NGramBlockingClusterer.tokenize("a", 2);
        assertEquals(ngrams.size(), 1);
        assertEquals(ngrams.get(0), "a");
    }
}
