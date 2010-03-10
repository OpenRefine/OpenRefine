package edu.mit.simile.vicino.distances;

public abstract class Distance {

    int counter = 0;
    
    public int getCount() {
        return counter;
    }
    
    public void resetCounter() {
        counter = 0;
    }
    
    public abstract double d(String x, String y);

}
