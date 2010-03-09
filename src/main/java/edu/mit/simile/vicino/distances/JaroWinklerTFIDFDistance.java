package edu.mit.simile.vicino.distances;

import com.wcohen.ss.JaroWinklerTFIDF;
import com.wcohen.ss.api.StringDistance;

public class JaroWinklerTFIDFDistance extends MetricDistance {

    StringDistance distance;

    public JaroWinklerTFIDFDistance() {
        this.distance = new JaroWinklerTFIDF();
    }

    protected double d2(String x, String y) {
        return this.distance.score(x, y);
    }

}
