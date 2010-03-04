package edu.mit.simile.vicino.distances;

import edu.mit.simile.vicino.Distance;

public abstract class PseudoMetricDistance implements Distance {

    public float d(String x, String y) {
        float cxx = d2(x, x);
        float cyy = d2(y, y);
        float cxy = d2(x, y);
        float cyx = d2(y, x);
        float result1 = (cxy + cyx) / (cxx + cyy) - 1.0f;
        return result1;
    }

    protected abstract float d2(String x, String y);
}
