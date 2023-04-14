
package org.openrefine.runners.local.pll;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class PLLTestsBase {

    final static private Logger logger = LoggerFactory.getLogger(PLLTestsBase.class);

    PLLContext context = null;

    @BeforeClass
    public void setUpPool() throws IOException {
        if (context == null) {
            // the split sizes are purposely very low for testing purposes,
            // so that we can check the partitioning strategy without using large files
            context = new PLLContext(
                    MoreExecutors.listeningDecorator(
                            Executors.newCachedThreadPool()),
                    4, 128L, 1024L, 4L, 10L);
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
