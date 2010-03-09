package edu.mit.simile.vicino.distances;


public abstract class MetricDistance extends Distance {

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
        double result = d2(x, y);
        counter += 1;
        return result;
    }
    
    abstract double d2(String x, String y);

}
