
package org.openrefine.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

/**
 * A file system which enforces that file names are alphabetically sorted when listing the contents of a directory.
 * <p>
 * This is important to preserve the ordering of RDDs serialized to disk, as partitions need to be read in the correct
 * order. This ought to be in Spark itself, but sadly it has not made it there yet.
 * <p>
 * <a href="https://issues.apache.org/jira/browse/SPARK-5300">See the corresponding Spark issue.</a>
 * 
 * @author Antonin Delpeuch
 *
 */
public class OrderedLocalFileSystem extends LocalFileSystem {

    @Override
    public RemoteIterator<LocatedFileStatus> listLocatedStatus(Path path) throws IOException {
        RemoteIterator<LocatedFileStatus> files = super.listLocatedStatus(path);
        List<LocatedFileStatus> filesList = new ArrayList<>();
        while (files.hasNext()) {
            filesList.add(files.next());
        }
        filesList.sort(new Comparator<LocatedFileStatus>() {

            @Override
            public int compare(LocatedFileStatus arg0, LocatedFileStatus arg1) {
                return arg0.getPath().compareTo(arg1.getPath());
            }
        });

        return new RemoteIterator<LocatedFileStatus>() {

            int i = 0;

            @Override
            public boolean hasNext() throws IOException {
                return i < filesList.size();
            }

            @Override
            public LocatedFileStatus next() throws IOException {
                return filesList.get(i++);
            }
        };
    }

}
