
package com.google.refine.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Reservoir sampling selects k items uniformly at random from a list or sequence.
 */
public class ReservoirSampler {

    public <T> List<T> sample(List<T> list, int reservoirSize) {
        // validate input
        if (reservoirSize < 0) {
            throw new IllegalArgumentException("Sampling factor (reservoir size) can not be smaller than zero");
        }

        // cases in which a pass over data is not needed
        if (reservoirSize >= list.size()) {
            return new ArrayList<>(list); // short-circuit: return original list
        }
        if (reservoirSize == 0) {
            return new ArrayList<>(); // short-circuit: empty list
        }

        // sample
        List<T> sample = list.stream().limit(reservoirSize).collect(Collectors.toList()); // init reservoir
        Random random = new Random();
        for (int i = reservoirSize; i < list.size(); i++) {
            int j = random.nextInt(i + 1);
            if (j < reservoirSize) {
                sample.set(j, list.get(i));
            }
        }

        return sample;
    }
}
