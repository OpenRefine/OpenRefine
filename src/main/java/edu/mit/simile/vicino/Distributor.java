package edu.mit.simile.vicino;

import java.util.List;

import edu.mit.simile.vicino.Distance;

public class Distributor extends Operator {

    private static final int COLUMNS = 70;
    private static final char CHAR = '=';

    public static void main(String[] args) throws Exception {
        
        Distance d = getDistance(args[0]);

        List<String> strings = getStrings(args[1]);

        int buckets = Integer.parseInt(args[2]);

        long start = System.currentTimeMillis();
        int[] values = new int[buckets];

        int size = strings.size();
        for (int i = 0; i < size; i++) {
            String x = (String) strings.get((int) (Math.random() * size));
            String y = (String) strings.get((int) (Math.random() * size));
            int dist = (int) (buckets * d.d(x, y));
            values[dist]++;
            System.out.print(".");
        }
        System.out.println();

        long stop = System.currentTimeMillis();
        float m = ((float) (stop - start)) / (float) size;

        int maxValue = 0;
        for (int i = 0; i < buckets; i++) {
            if (values[i] > maxValue) {
                maxValue = values[i];
            }
        }

        System.out
                .println("+-------------------------------------------------------------------");
        for (int i = 0; i < buckets; i++) {
            System.out.println("|" + bar(COLUMNS * values[i] / maxValue));
        }
        System.out
                .println("+-------------------------------------------------------------------");

        System.out.println("\n Each distance calculation took: " + m + " millis");
    }

    static private String bar(int value) {
        StringBuffer b = new StringBuffer(value);
        for (int i = 0; i < value; i++) {
            b.append(CHAR);
        }
        return b.toString();
    }
}
