package org.openrefine.io;

import java.io.File;

public class IOUtils {
    
    /**
     * Delete a directory if it already exists.
     * Workaround for the fact that Spark refuses to save a RDD
     * to a directory if it already exists.
     */
    public static void deleteDirectoryIfExists(File dir) {
        if(dir.exists()) {
            dir.delete();
        }
    }
}
