package edu.mit.simile.vicino.distances;

import com.wcohen.ss.JaroWinkler;
import com.wcohen.ss.api.StringDistance;

public class JaroWinklerDistance extends MetricDistance {

    StringDistance distance;

    public JaroWinklerDistance() {
        this.distance = new JaroWinkler();
    }

    protected double d2(String x, String y) {
        return this.distance.score(x, y);
    }

}
