
package org.openrefine.importing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;
import org.mozilla.universalchardet.UnicodeBOMInputStream;
import org.mozilla.universalchardet.UniversalDetector;

import org.openrefine.importing.ImportingJob.RetrievalRecord;
import org.openrefine.util.JSONUtilities;

/**
 * This class tries to find the correct encoding based on https://github.com/albfernandez/juniversalchardet which is a
 * Java port of Mozilla's universalchardet library
 * https://hg.mozilla.org/mozilla-central/file/tip/extensions/universalchardet/
 *
 * @author <a href="mailto:kontakt@stundzig.de">Steffen Stundzig</a>
 */
public final class EncodingGuesser {

    public static final String UTF_8_BOM = "UTF-8-BOM";

    public final static void guess(final ImportingJob job)
            throws IOException {
        RetrievalRecord retrievalRecord = job.getRetrievalRecord();
        if (retrievalRecord != null) {
            List<ImportingFileRecord> fileRecords = retrievalRecord.files;
            if (fileRecords != null) {
                // TODO: If different files have different encodings, we're only able to present a single
                // encoding to the user currently. Should we check for conflicts? Warn the user?
                for (int i = 0; i < fileRecords.size(); i++) {
                    ImportingFileRecord record = fileRecords.get(i);
                    String encoding = record.getEncoding();
                    if (StringUtils.isBlank(encoding)) {
                        String location = record.getLocation();
                        if (location != null) {
                            try (UnicodeBOMInputStream is = new UnicodeBOMInputStream(
                                    new FileInputStream(new File(job.getRawDataDir(), location)))) {
                                String detected = UniversalDetector.detectCharset(is);
                                UnicodeBOMInputStream.BOM bom = is.getBOM();
                                if (UnicodeBOMInputStream.BOM.UTF_8.equals(bom)) {
                                    detected = UTF_8_BOM;
                                }
                                if (detected != null) {
                                    record.setEncoding(detected);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * uses the first found encoding in the file records as initial encoding and put them into the options
     *
     * @param fileRecords
     * @param options
     */
    public final static void guessInitialEncoding(final List<ImportingFileRecord> fileRecords, final ObjectNode options) {
        if (fileRecords != null) {
            for (ImportingFileRecord record : fileRecords) {
                String encoding = record.getEncoding();
                if (!StringUtils.isBlank(encoding)) {
                    JSONUtilities.safePut(options, "encoding", encoding);
                    break;
                }
            }
        }
    }
}
