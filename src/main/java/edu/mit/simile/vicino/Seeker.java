package edu.mit.simile.vicino;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import edu.mit.simile.vicino.distances.Distance;
import edu.mit.simile.vicino.vptree.VPTree;
import edu.mit.simile.vicino.vptree.VPTreeBuilder;
import edu.mit.simile.vicino.vptree.VPTreeSeeker;

public class Seeker extends Operator {

    public static void main(String[] args) throws Exception {
        Distance d = getDistance(args[0]);

        log("Working with distance: " + d);
        List<String> strings = getStrings(args[1]);
        log("Obtained " + strings.size() + " from " + args[1]);

        log("Building VPTree...");
        VPTreeBuilder builder = new VPTreeBuilder(d);
        VPTree tree = builder.buildVPTree(strings);
        log("..done");

        VPTreeSeeker seeker = new VPTreeSeeker(d, tree);

        log("type a string|range then hit return:");
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String line = null;
        while ((line = input.readLine()) != null) {
            int index = line.indexOf('|');
            String query = line.substring(0, index);
            float range = Float.parseFloat(line.substring(index + 1));
            long start = System.currentTimeMillis();
            List<? extends Serializable> results = seeker.range(query, range);
            long stop = System.currentTimeMillis();
            Iterator<? extends Serializable> j = results.iterator();
            if (j.hasNext()) {
                while (j.hasNext()) {
                    String r = (String) j.next();
                    log("   " + r);
                }
                log(" [done in " + (stop - start) + "ms]");
            } else {
                log(" [no results found in " + (stop - start) + "ms]");
            }
        }
    }
}
