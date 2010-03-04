package edu.mit.simile.vicino.distances;

import com.wcohen.ss.Jaccard;
import com.wcohen.ss.api.StringDistance;

public class JaccardDistance extends MetricDistance {

    StringDistance distance;

    public JaccardDistance() {
        this.distance = new Jaccard();
    }

    protected float d2(String x, String y) {
        return Math.abs((float) this.distance.score(x, y) - 1.0f);
    }

}
