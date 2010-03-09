package edu.mit.simile.vicino.distances;

import com.wcohen.ss.Jaccard;
import com.wcohen.ss.api.StringDistance;

public class JaccardDistance extends MetricDistance {

    StringDistance distance;

    public JaccardDistance() {
        this.distance = new Jaccard();
    }

    protected double d2(String x, String y) {
        return this.distance.score(x, y);
    }

}
