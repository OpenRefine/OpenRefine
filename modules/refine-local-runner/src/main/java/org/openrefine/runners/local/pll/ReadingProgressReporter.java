
package org.openrefine.runners.local.pll;

import java.util.concurrent.atomic.AtomicLong;

import org.openrefine.importers.MultiFileReadingProgress;

/**
 * Adapter to report reading progress from multiple threads back into a {@link MultiFileReadingProgress} object.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ReadingProgressReporter {

    private final MultiFileReadingProgress parent;
    private final AtomicLong totalRead;
    private final String fileName;

    public ReadingProgressReporter(MultiFileReadingProgress progress, String fileName) {
        parent = progress;
        totalRead = new AtomicLong(0L);
        this.fileName = fileName;
    }

    /**
     * Reports that we read some more bytes.
     * 
     * @param bytesRead
     *            additional bytes read (not total amount)
     */
    public void increment(long bytesRead) {
        long read = totalRead.addAndGet(bytesRead);
        parent.readingFile(fileName, read);
    }
}
