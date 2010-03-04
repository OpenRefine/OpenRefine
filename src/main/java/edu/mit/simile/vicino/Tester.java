package edu.mit.simile.vicino;

import java.util.List;

public class Tester extends Operator {

    public static void main(String[] args) throws Exception {
        Distance d = getDistance(args[0]);

        List<String> strings = getStrings(args[1]);

        long start = System.currentTimeMillis();

        int size = strings.size();
        for (int i = 0; i < size; i++) {
            String x = (String) strings.get((int) (Math.random() * size));
            String y = (String) strings.get((int) (Math.random() * size));
            String z = (String) strings.get((int) (Math.random() * size));
            boolean metric = metric(x, y, z, d);
            if (metric) {
                System.out.println("metric");
            } else {
                System.out.println("***** NOT METRIC *****");
            }
        }

        long stop = System.currentTimeMillis();
        float m = ((float) (stop - start)) / (float) size;

        System.out.println("\n Each metric evaluation took: " + m + " millis");
    }

    static boolean metric(String x, String y, String z, Distance d) {
        float dxx = d.d(x, x);
        boolean identity = (dxx == 0.0f);
        float dxy = d.d(x, y);
        float dyx = d.d(y, x);
        boolean simmetrical = (dxy == dyx);
        float dxz = d.d(x, z);
        float dyz = d.d(y, z);
        boolean triangular = (dxz <= dxy + dyz);
        return (identity && simmetrical && triangular);
    }

    static Distance getDistance(String distance) throws Exception {
        return (Distance) Class.forName(
                "edu.mit.simile.vicino.distances." + distance + "Distance")
                .newInstance();
    }
}
