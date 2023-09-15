
package org.openrefine.model.local;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.LocalFileSystem;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

public class PLLTestsBase {

    static PLLContext context = null;

    @BeforeTest
    public void setUpPool() throws IOException {
        if (context == null) {
            context = new PLLContext(
                    MoreExecutors.listeningDecorator(
                            Executors.newCachedThreadPool()),
                    LocalFileSystem.get(new Configuration()));
        }
    }

    protected <T> PLL<T> parallelize(int numPartitions, List<T> elements) {
        return new InMemoryPLL<T>(context, elements, numPartitions);
    }

    @AfterTest
    public void tearDownPool() throws IOException {
        if (context != null) {
            context.shutdown();
            context = null;
        }
    }
}
