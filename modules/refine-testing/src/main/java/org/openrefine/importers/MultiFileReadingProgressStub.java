
package org.openrefine.importers;

public class MultiFileReadingProgressStub implements MultiFileReadingProgress {

    public long bytesRead = -1;
    public String fileSource = null;

    @Override
    public void startFile(String fileSource) {
        this.fileSource = fileSource;
        this.bytesRead = 0;
    }

    @Override
    public void readingFile(String fileSource, long bytesRead) {
        this.fileSource = fileSource;
        this.bytesRead = bytesRead;
    }

    @Override
    public void endFiles() {

    }

}
