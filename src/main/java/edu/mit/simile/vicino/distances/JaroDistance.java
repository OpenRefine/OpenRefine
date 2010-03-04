package edu.mit.simile.vicino.distances;

import com.wcohen.ss.Jaro;
import com.wcohen.ss.api.StringDistance;

public class JaroDistance extends MetricDistance {

    StringDistance distance;

    public JaroDistance() {
        this.distance = new Jaro();
    }

    protected float d2(String x, String y) {
        return Math.abs((float) this.distance.score(x, y) - 1.0f);
    }

}
