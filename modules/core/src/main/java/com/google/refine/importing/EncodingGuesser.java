
package com.google.refine.importing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.universalchardet.UnicodeBOMInputStream;
import org.mozilla.universalchardet.UniversalDetector;

import com.google.refine.util.JSONUtilities;

/**
 * This class tries to find the correct encoding based on https://github.com/albfernandez/juniversalchardet which is a
 * Java port of Mozilla's universalchardet library
 * https://hg.mozilla.org/mozilla-central/file/tip/extensions/universalchardet/
 *
 * @author <a href="mailto:kontakt@stundzig.de">Steffen Stundzig</a>
 */
public final class EncodingGuesser {

    public static final String UTF_8_BOM = "UTF-8-BOM"; // Fake encoding for weird Microsoft UFT-8 with BOM

    // Overloaded method without sortCriteria and sortOrder
    public static void guess(final ImportingJob job) throws IOException {
        guess(job, null, null);
    }

    public static void guess(final ImportingJob job, String sortCriteria, String sortOrder)
            throws IOException {
        ObjectNode retrievalRecord = job.getRetrievalRecord();
        if (retrievalRecord != null) {
            ArrayNode fileRecords = JSONUtilities.getArray(retrievalRecord, "files");
            if (fileRecords != null) {
                // TODO: If different files have different encodings, we're only able to present a single
                // encoding to the user currently. Should we check for conflicts? Warn the user?

                List<ObjectNode> fileList = new ArrayList<>();
                for (int i = 0; i < fileRecords.size(); i++) {
                    fileList.add((ObjectNode) fileRecords.get(i));
                }
                if (sortCriteria != null && sortOrder != null) {
                    fileList.sort((o1, o2) -> {
                        int comparison = 0;
                        switch (sortCriteria) {
                            case "fileName":
                                comparison = o1.get("fileName").asText().compareTo(o2.get("fileName").asText());
                                break;
                            case "fileSize":
                                comparison = Long.compare(o1.get("size").asLong(), o2.get("size").asLong());
                                break;
                        }
                        return "desc".equals(sortOrder) ? -comparison : comparison;
                    });
                }

                // Clear the original ArrayNode and populate it with sorted elements
                fileRecords.removeAll();
                for (ObjectNode fileObject : fileList) {
                    fileRecords.add(fileObject);
                }

                guessFilesEncodings(job, fileRecords);
            }
        }
    }

    private static void guessFilesEncodings(ImportingJob job, ArrayNode fileRecords) throws IOException {
        for (int i = 0; i < fileRecords.size(); i++) {
            ObjectNode record = JSONUtilities.getObjectElement(fileRecords, i);
            String encoding = ImportingUtilities.getEncoding(record);
            if (StringUtils.isBlank(encoding)) {
                String location = JSONUtilities.getString(record, "location", null);
                if (location != null) {
                    String detected = guessEncoding(job.getRawDataDir(), location);
                    if (detected != null) {
                        JSONUtilities.safePut(record, "encoding", detected);
                    }
                }
            }
        }
    }

    public static String guessEncoding(File dir, String location) throws IOException {
        try (UnicodeBOMInputStream is = new UnicodeBOMInputStream(
                new FileInputStream(new File(dir, location)), false)) {
            String detected = UniversalDetector.detectCharset(is);
            if (UnicodeBOMInputStream.BOM.UTF_8.equals(is.getBOM())) {
                detected = UTF_8_BOM;
            }
            return detected;
        }
    }

    /**
     * uses the first found encoding in the file records as initial encoding and put them into the options
     * 
     * @param fileRecords
     * @param options
     */
    public static void guessInitialEncoding(final List<ObjectNode> fileRecords, final ObjectNode options) {
        if (fileRecords != null) {
            for (ObjectNode record : fileRecords) {
                String encoding = JSONUtilities.getString(record, "encoding", null);
                if (!StringUtils.isBlank(encoding)) {
                    JSONUtilities.safePut(options, "encoding", encoding);
                    break;
                }
            }
        }
    }
}
