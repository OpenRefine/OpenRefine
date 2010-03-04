package edu.mit.simile.vicino.distances;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class GZipDistance extends PseudoMetricDistance {

    public float d2(String x, String y) {
        String str = x + y;
        float result = 0.0f;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(str.length());
            GZIPOutputStream os = new GZIPOutputStream(baos);
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
