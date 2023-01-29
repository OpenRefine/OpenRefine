
package org.openrefine.runners.spark.io;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class IOUtils {

    /**
     * Delete a directory if it already exists. Workaround for the fact that Spark refuses to save a RDD to a directory
     * if it already exists.
     * 
     * @throws IOException
     */
    public static void deleteDirectoryIfExists(File dir) throws IOException {
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
    }
}
