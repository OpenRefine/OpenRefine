
package com.google.refine.importing;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import com.google.refine.util.JSONUtilities;

/**
 * This class tries tofind the correct encoding based on the
 * http://site.icu-project.org/ and the icu4j library
 * http://site.icu-project.org/home/why-use-icu4j.
 * 
 * @author <a href="mailto:kontakt@stundzig.de">Steffen Stundzig</a>
 */
public final class EncodingGuesser {

    public final static void guess(final ImportingJob job)
            throws FileNotFoundException, IOException {
        if (job.getRetrievalRecord() != null) {
            JSONArray fileRecords = JSONUtilities.getArray(job.getRetrievalRecord(), "files");
            if (fileRecords != null) {
                for (int i = 0; i < fileRecords.length(); i++) {
                    JSONObject record = JSONUtilities.getObjectElement(fileRecords, i);
                    String encoding = ImportingUtilities.getEncoding(record);
                    if (StringUtils.isBlank(encoding)) {
                        String location = JSONUtilities.getString(record, "location", null);
                        if (location != null) {
                            CharsetDetector detector = new CharsetDetector();
                            detector.setText(new BufferedInputStream(
                                    new FileInputStream(new File(job.getRawDataDir(), location))));
                            CharsetMatch match = detector.detect();
                            if (match != null && match.getConfidence() > 10) {
                                JSONUtilities.safePut(record, "encoding", match.getName());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * uses the first found encoding in the file records as initial encoding and
     * put them into the options
     * 
     * @param fileRecords
     * @param options
     */
    public final static void guessInitialEncoding(final List<JSONObject> fileRecords, final JSONObject options) {
        if (fileRecords != null) {
            for (JSONObject record : fileRecords) {
                String encoding = JSONUtilities.getString(record, "encoding", null);
                if (!StringUtils.isBlank(encoding)) {
                    JSONUtilities.safePut(options, "encoding", encoding);
                    break;
                }
            }
        }
    }
}
