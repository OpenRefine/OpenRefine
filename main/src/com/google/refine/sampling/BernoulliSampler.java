
package com.google.refine.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Bernoulli Sampling selects a subset of items from a list or sequence such that each item has an equal probability of
 * being chosen, independently of the others.
 */
public class BernoulliSampler {

    public <T> List<T> sample(List<T> list, int percentage) {
        // validate input
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Sampling factor (percentage) must be between 0 and 100");
        }

        // cases in which a pass over data is not needed
        if (percentage == 100) {
            return new ArrayList<>(list); // short-circuit: return original list
        }
        if (percentage == 0 || list.isEmpty()) {
            return new ArrayList<>(); // short-circuit: empty list
        }

        // sample
        List<T> sample = new ArrayList<>();
        Random random = new Random();
        for (T element : list) {
            if (random.nextInt(100) < percentage) {
                sample.add(element);
            }
        }

        return sample;
    }
}
