package com.google.refine.clustering;

import java.io.Serializable;
import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusteredEntry {
    @JsonProperty("v")
    protected final Serializable value;
    @JsonProperty("c")
    protected final int count;
    
    public ClusteredEntry(
            Serializable value,
            int count) {
        this.value = value;
        this.count = count;
    }
    
    public static Comparator<ClusteredEntry> comparator = new Comparator<ClusteredEntry>() {
        @Override
        public int compare(ClusteredEntry o1, ClusteredEntry o2) {
            return o2.count - o1.count;
        }
    };
}
