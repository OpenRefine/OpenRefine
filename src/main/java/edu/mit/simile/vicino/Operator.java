package edu.mit.simile.vicino;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.mit.simile.vicino.distances.Distance;

public class Operator {

    static void log(String msg) {
        System.out.println(msg);
    }

    static Distance getDistance(String distance) throws Exception {
        return (Distance) Class.forName("edu.mit.simile.vicino.distances." + distance + "Distance").newInstance();
    }

    static List<String> getStrings(String fileName) throws IOException {
        ArrayList<String> strings = new ArrayList<String>();

        File file = new File(fileName);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                BufferedReader input = new BufferedReader(new FileReader(files[i]));
                StringBuffer b = new StringBuffer();
                String line;
                while ((line = input.readLine()) != null) {
                    b.append(line.trim());
                }
                input.close();
                strings.add(b.toString());
            }
        } else {
            BufferedReader input = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = input.readLine()) != null) {
                strings.add(line.trim());
            }
            input.close();
        }

        return strings;
    }
}
