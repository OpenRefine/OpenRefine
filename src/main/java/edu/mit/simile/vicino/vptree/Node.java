package edu.mit.simile.vicino.vptree;

import java.io.Serializable;

/**
 * This class represent a couple (Object, distance) value of that Object from
 * the Vp in each step of the algorithm.
 * 
 * @author Paolo Ciccarese
 */
public class Node implements Serializable {

    private static final long serialVersionUID = -2077473220894258550L;

    private final Serializable obj;
    private double distance;

    public Node(Serializable obj, int i) {
        this.obj = obj;
        this.distance = i;
    }

    public Node(Serializable obj) {
        this.obj = obj;
    }

    public Serializable get() {
        return this.obj;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }

    public String toString() {
        return obj.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Node) {
            return ((Node) o).get().equals(this.obj);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return this.obj.hashCode();
    }
}
