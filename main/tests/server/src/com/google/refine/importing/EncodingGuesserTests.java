
package com.google.refine.importing;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.annotations.Test;

import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class EncodingGuesserTests {

    // Guessing isn't as reliable for single-byte encodings, so we focus on a few multibyte
    // non-UTF8 encodings which are still in use (but <1% prevalence on web)
    static String[] ENCODINGS = {
            "big5",
            "euc-jp",
            "euc-kr",
            "shift_jis",
    };

    private static File getTestDir() {
        String dir = ClassLoader.getSystemResource(ENCODINGS[0] + ".txt").getPath();
        dir = dir.substring(0, dir.lastIndexOf('/'));
        return new File(dir);
    }

    public class ImportingJobStub extends ImportingJob {

        public ImportingJobStub() {
            super(1, getTestDir());
        }

        @Override
        public File getRawDataDir() {
            return this.dir;
        }
    }

    @Test
    public void testEncodingGuesser() throws IOException {

        for (String encoding : ENCODINGS) {
            checkEncoding(encoding + ".txt", encoding);
        }

        checkEncoding("example-latin1.tsv", "windows-1252"); // close enough - these overlap a lot
        checkEncoding("example-utf8.tsv", "utf-8");
        checkEncoding("example-utf16le-bom.tsv", "utf-16le");
        checkEncoding("example-utf16be-bom.tsv", "utf-16be");
        checkEncoding("csv-with-bom.csv", "utf-8-bom");
    }

    private void checkEncoding(String filename, String encoding) throws IOException {
        ImportingJob job = new ImportingJobStub();
        ObjectNode config = job.getOrCreateDefaultConfig();
        ObjectNode filesObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                String.format("{ \"files\": [ {\"location\": \"%s\"}]}", filename));
        JSONUtilities.safePut(config, "retrievalRecord", filesObj);

        EncodingGuesser.guess(job);

        ObjectNode retrievalRecord = job.getRetrievalRecord();
        assertNotNull(retrievalRecord);
        ArrayNode fileRecords = JSONUtilities.getArray(retrievalRecord, "files");
        assertNotNull(fileRecords);
        assertEquals(fileRecords.size(), 1);
        ObjectNode record = JSONUtilities.getObjectElement(fileRecords, 0);
        assertEquals(JSONUtilities.getString(record, "encoding", null).toLowerCase(), encoding);
    }
}
