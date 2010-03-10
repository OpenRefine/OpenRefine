package edu.mit.simile.vicino.distances;


public abstract class PseudoMetricDistance extends Distance {

    public double d(String x, String y) {
        double cxx = d2(x, x);
        double cyy = d2(y, y);
        double cxy = d2(x, y);
        double cyx = d2(y, x);
        counter += 4;
        return 10.0d * ((cxy + cyx) / (cxx + cyy) - 1.0d);
    }
    
    protected abstract double d2(String x, String y);
}
