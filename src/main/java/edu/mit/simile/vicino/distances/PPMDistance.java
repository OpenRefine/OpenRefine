package edu.mit.simile.vicino.distances;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.colloquial.arithcode.ArithCodeOutputStream;
import com.colloquial.arithcode.PPMModel;

public class PPMDistance extends PseudoMetricDistance {

    public double d2(String x, String y) {
        String str = x + y;
        double result = 0.0f;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(str.length());
            ArithCodeOutputStream os = new ArithCodeOutputStream(baos,new PPMModel(8));
            os.write(str.getBytes());
            os.close();
            baos.close();
            result = baos.toByteArray().length;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}
