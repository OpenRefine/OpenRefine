package com.google.refine.clustering.knn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    
    static {
        _distances.put("levenshtein", new VicinoDistance(new LevenshteinDistance()));
        _distances.put("jaccard", new VicinoDistance(new JaccardDistance()));
        _distances.put("jaro", new VicinoDistance(new JaroDistance()));
        _distances.put("jaro-winkler", new VicinoDistance(new JaroWinklerDistance()));
        _distances.put("jaro-winkler-tfidf", new VicinoDistance(new JaroWinklerTFIDFDistance()));
        _distances.put("gzip", new VicinoDistance(new GZipDistance()));
        _distances.put("bzip2", new VicinoDistance(new BZip2Distance()));
        _distances.put("ppm", new VicinoDistance(new PPMDistance()));
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
    }
    
    /**
     * Lists the available distances, by name.
     */
    public static Set<String> getDistanceNames() {
    	return _distances.keySet();
    }
}
