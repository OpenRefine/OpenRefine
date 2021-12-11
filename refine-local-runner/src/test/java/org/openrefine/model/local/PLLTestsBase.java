
package org.openrefine.model.local;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import org.openrefine.io.OrderedLocalFileSystem;

public class PLLTestsBase {

    PLLContext context = null;

    @BeforeClass
    public void setUpPool() throws IOException {
        if (context == null) {
            Configuration conf = new Configuration();
            // these values are purposely very low for testing purposes,
            // so that we can check the partitioning strategy without using large files
            conf.set("fs.file.impl", OrderedLocalFileSystem.class.getName());
            conf.setLong("mapreduce.input.fileinputformat.split.minsize", 128L);
            conf.setLong("mapreduce.input.fileinputformat.split.maxsize", 1024L);
            FileSystem fileSystem = LocalFileSystem.get(conf);

            context = new PLLContext(
                    MoreExecutors.listeningDecorator(
                            Executors.newCachedThreadPool()),
                    fileSystem,
                    4);
        }
    }

    protected <T> PLL<T> parallelize(int numPartitions, List<T> elements) {
        return new InMemoryPLL<T>(context, elements, numPartitions);
    }

    @AfterClass
    public void tearDownPool() throws IOException {
        if (context != null) {
            context.shutdown();
            context = null;
        }
    }
}
