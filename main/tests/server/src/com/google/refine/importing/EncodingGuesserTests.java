
package com.google.refine.importing;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import java.io.File;
import java.io.IOException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class EncodingGuesserTests {

    // Guessing isn't as reliable for single-byte encodings, so we focus on a few multi-byte
    // non-UTF8 encodings which are still in use (but <1% prevalence on web)
    static String[] ENCODINGS = {
            "big5",
            "euc-jp",
            "euc-kr",
            "shift_jis",
    };

    private static File getTestDir() {
        String dir = ClassLoader.getSystemResource(ENCODINGS[0] + ".html").getPath();
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
            ImportingJob job = new ImportingJobStub();
            ObjectNode config = job.getOrCreateDefaultConfig();
            ObjectNode filesObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                    String.format("{ \"files\": [ {\"location\": \"%s.txt\"}]}", encoding));
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
}
