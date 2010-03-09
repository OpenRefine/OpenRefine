package edu.mit.simile.vicino.distances;

import com.wcohen.ss.Levenstein;
import com.wcohen.ss.api.StringDistance;

public class LevenshteinDistance extends MetricDistance {

    StringDistance distance;

    public LevenshteinDistance() {
        this.distance = new Levenstein();
    }

    public double d2(String x, String y) {
        return Math.abs(this.distance.score(x, y));
    }

}
