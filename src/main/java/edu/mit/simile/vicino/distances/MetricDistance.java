package edu.mit.simile.vicino.distances;

import edu.mit.simile.vicino.Distance;

public abstract class MetricDistance implements Distance {

    /*
     * public float d(String x,String y) { 
     *  float dxy = d2(x,y); 
     *  float dx = d2(x,""); 
     *  float dy = d2(y,""); 
     *  float result = dxy / (dx + dy); 
     *  return result; 
     * }
     */

    public double d(String x, String y) {
        return d2(x, y);
    }

    abstract double d2(String x, String y);

}
