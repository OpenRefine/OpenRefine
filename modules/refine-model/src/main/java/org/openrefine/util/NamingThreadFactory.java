
package org.openrefine.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.*;

/**
 * A utility to name the threads in a {@link ThreadPoolExecutor}.
 */
public class NamingThreadFactory implements ThreadFactory {

    private final String namePrefix;
    private final AtomicInteger nextId = new AtomicInteger(0);

    public NamingThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        String name = namePrefix + "-thread-" + nextId.getAndIncrement();
        return new Thread(r, name);
    }

}
