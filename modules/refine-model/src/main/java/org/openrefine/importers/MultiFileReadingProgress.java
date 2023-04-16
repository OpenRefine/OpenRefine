
package org.openrefine.importers;

/**
 * Interface for reporting progress of reading a file, by repeatedly updating how many bytes have been read in the file.
 */
public interface MultiFileReadingProgress {

    /**
     * Called when starting to read a file
     */
    public void startFile(String fileSource);

    /**
     * Called while the file is being read, with the total number of bytes read since the beginning of the file
     */
    public void readingFile(String fileSource, long bytesRead);

    /**
     * Called at the end of reading all files in the importing job. This is to ensure that subsequent reads (for
     * instance when saving the project in the workspace) do not impact the progress, as the progress of that step is
     * handled separately.
     */
    public void endFiles();
}
