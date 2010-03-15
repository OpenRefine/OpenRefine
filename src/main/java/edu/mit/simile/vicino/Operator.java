package edu.mit.simile.vicino;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
        List<String> strings = new ArrayList<String>();

        File file = new File(fileName);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                getStrings(f, strings);
            }
        } else {
            getStrings(file, strings);
        }

        return strings;
    }
    
    static void getStrings(File file, List<String> strings) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        String line;
        while ((line = input.readLine()) != null) {
            strings.add(line.trim().intern());
        }
        input.close();
    }
}
