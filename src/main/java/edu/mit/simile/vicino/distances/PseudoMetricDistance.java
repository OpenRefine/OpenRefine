package edu.mit.simile.vicino.distances;

import edu.mit.simile.vicino.Distance;

public abstract class PseudoMetricDistance implements Distance {

    public double d(String x, String y) {
        double cxx = d2(x, x);
        double cyy = d2(y, y);
        double cxy = d2(x, y);
        double cyx = d2(y, x);
        double result1 = (cxy + cyx) / (cxx + cyy) - 1.0d;
        return result1;
    }

    protected abstract double d2(String x, String y);
}
