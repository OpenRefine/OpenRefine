package org.openrefine.wikidata.utils;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;

public class FirstLinesExtractor {
    /**
     * Returns the first n lines of a given string
     * @param content
     *      the content, where lines are separated by '\n'
     * @param nbLines
     *      the number of lines to extract
     * @return
     *      the first lines of the string
     * @throws IOException 
     */
    public static String extractFirstLines(String content, int nbLines) throws IOException {
        StringWriter stringWriter = new StringWriter();
        LineNumberReader reader = new LineNumberReader(new StringReader(content));
        
        // Only keep the first 50 lines
        reader.setLineNumber(0);
        String line = reader.readLine();
        for(int i = 1; i != nbLines && line != null; i++) {
            stringWriter.write(line+"\n");
            line = reader.readLine();
        }
        if (reader.getLineNumber() == nbLines) {
            stringWriter.write("...");
        }
        return stringWriter.toString();
    }
}
