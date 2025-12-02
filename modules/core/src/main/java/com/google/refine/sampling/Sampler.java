
package com.google.refine.sampling;

import java.util.List;

public interface Sampler {

    <T> List<T> sample(List<T> list, int samplingFactor);
}
