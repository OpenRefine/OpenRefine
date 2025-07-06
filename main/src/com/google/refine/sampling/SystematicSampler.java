
package com.google.refine.sampling;

import java.util.ArrayList;
import java.util.List;

/**
 * Systematic sampling selects every k-th item from a list or sequence.
 */
public class SystematicSampler {

    public <T> List<T> sample(List<T> list, int stepSize) {
        // validate input
        if (stepSize <= 0) {
            throw new IllegalArgumentException("Sampling factor (step size) can not be smaller than or equal to zero");
        }

        // sample
        List<T> sample = new ArrayList<>();
        for (int i = 0; i < list.size(); i += stepSize) {
            sample.add(list.get(i));
        }

        return sample;
    }
}
