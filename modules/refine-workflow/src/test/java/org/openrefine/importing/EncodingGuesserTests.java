
package org.openrefine.importing;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import org.openrefine.importing.ImportingJob.ImportingJobConfig;
import org.openrefine.importing.ImportingJob.RetrievalRecord;

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
            ImportingJobConfig config = job.getJsonConfig();
            RetrievalRecord retrievalRecord = new RetrievalRecord();
            String fileName = String.format("%s.txt", encoding);
            ImportingFileRecord importingFileRecord = new ImportingFileRecord(fileName, fileName,
                    0, null, null, null, null, null, null, null, null);
            retrievalRecord.files = Collections.singletonList(importingFileRecord);
            config.retrievalRecord = retrievalRecord;

            EncodingGuesser.guess(job);

            List<ImportingFileRecord> fileRecords = retrievalRecord.files;
            assertNotNull(fileRecords);
            assertEquals(fileRecords.size(), 1);
            ImportingFileRecord record = fileRecords.get(0);
            assertEquals(record.getEncoding().toLowerCase(), encoding);
        }
    }
}
