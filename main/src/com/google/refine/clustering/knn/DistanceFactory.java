package com.google.refine.clustering.knn;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.mit.simile.vicino.distances.BZip2Distance;
import edu.mit.simile.vicino.distances.GZipDistance;
import edu.mit.simile.vicino.distances.JaccardDistance;
import edu.mit.simile.vicino.distances.JaroDistance;
import edu.mit.simile.vicino.distances.JaroWinklerDistance;
import edu.mit.simile.vicino.distances.JaroWinklerTFIDFDistance;
import edu.mit.simile.vicino.distances.LevenshteinDistance;
import edu.mit.simile.vicino.distances.PPMDistance;

/**
 * Registry of distances for kNN clustering.
 * 
 * @author Antonin Delpeuch
 */
public class DistanceFactory {
    
    static final protected Map<String, SimilarityDistance> _distances = new HashMap<>();
    // We cannot derive this from the hashmap as the order matters
	private static List<String> _distanceNames = new LinkedList<>();
    
    static {
        put("levenshtein", new VicinoDistance(new LevenshteinDistance()));
        put("jaccard", new VicinoDistance(new JaccardDistance()));
        put("jaro", new VicinoDistance(new JaroDistance()));
        put("jaro-winkler", new VicinoDistance(new JaroWinklerDistance()));
        put("jaro-winkler-tfidf", new VicinoDistance(new JaroWinklerTFIDFDistance()));
        put("gzip", new VicinoDistance(new GZipDistance()));
        put("bzip2", new VicinoDistance(new BZip2Distance()));
        put("ppm", new VicinoDistance(new PPMDistance()));
    }
    
    /**
     * Returns the distance registered under this name, or null if it does not exist.
     */
    public static SimilarityDistance get(String name) {
    	return _distances.get(name);
    }
    
    /**
     * Registers a new distance under a name.
     */
    public static void put(String name, SimilarityDistance distance) {
    	_distances.put(name, distance);
    	_distanceNames.add(name);
    }
    
    /**
     * Lists the available distances, by name.
     */
    public static List<String> getDistanceNames() {
    	return Collections.unmodifiableList(_distanceNames);
    }
}
