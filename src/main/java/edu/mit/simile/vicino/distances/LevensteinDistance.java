package edu.mit.simile.vicino.distances;

import com.wcohen.ss.Levenstein;
import com.wcohen.ss.api.StringDistance;

import edu.mit.simile.vicino.Distance;

public class LevensteinDistance implements Distance {

    StringDistance distance;

    public LevensteinDistance() {
        this.distance = new Levenstein();
    }

    public float d(String x, String y) {
        float d = Math.abs((float) this.distance.score(x, y));
        // System.out.println(this.distance.explainScore(x,y));
        return d / (x.length() + y.length());
    }

}
