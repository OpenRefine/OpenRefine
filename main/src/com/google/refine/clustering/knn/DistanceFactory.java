/*******************************************************************************
 * Copyright (C) 2018, Antonin Delpeuch
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.clustering.knn;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Registry of distances for kNN clustering.
 * 
 * @author Antonin Delpeuch
 */
public class DistanceFactory {

    static final protected Map<String, SimilarityDistance> _distances = new HashMap<>();
    // We cannot derive this from the hashmap as the order matters
    private static List<String> _distanceNames = new LinkedList<>();

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
