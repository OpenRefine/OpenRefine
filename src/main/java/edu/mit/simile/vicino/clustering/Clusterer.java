package edu.mit.simile.vicino.clustering;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public abstract class Clusterer {

    public class SizeComparator implements Comparator<Set<Serializable>> {
        public int compare(Set<Serializable> o1, Set<Serializable> o2) {
            return o2.size() - o1.size();
        }
    }
    
    public abstract void populate(String s);

    public abstract List<Set<Serializable>> getClusters(double radius);
    
}
