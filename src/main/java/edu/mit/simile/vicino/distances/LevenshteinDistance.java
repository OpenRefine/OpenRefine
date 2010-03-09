package edu.mit.simile.vicino.distances;

import com.wcohen.ss.Levenstein;
import com.wcohen.ss.api.StringDistance;

import edu.mit.simile.vicino.Distance;

public class LevenshteinDistance implements Distance {

    StringDistance distance;

    public LevenshteinDistance() {
        this.distance = new Levenstein();
    }

    public double d(String x, String y) {
        return Math.abs(this.distance.score(x, y));
    }

}
