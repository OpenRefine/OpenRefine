package edu.mit.simile.vicino;

import edu.mit.simile.vicino.distances.Distance;

public class Meter extends Operator {

    public static void main(String[] args) throws Exception {
        Distance d = getDistance(args[0]);
        System.out.println(args[1] + " <- " + d.d(args[1], args[2]) + " -> " + args[2]);
    }

}
