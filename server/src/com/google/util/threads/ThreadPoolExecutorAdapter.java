//========================================================================
//$$Id: ThreadPoolExecutorAdapter.java,v 1.3 2007/11/02 12:39:41 ludovic_orban Exp $$
//
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package com.google.util.threads;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.ThreadPool;

/**
 * Jetty {@link ThreadPool} that bridges requests to a {@link ThreadPoolExecutor}.
 */
public class ThreadPoolExecutorAdapter implements ThreadPool, LifeCycle {

    private ThreadPoolExecutor executor;

    public ThreadPoolExecutorAdapter(ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Runnable job) {
        try {
            executor.execute(job);
        } catch (RejectedExecutionException e) {
            Log.getLog().warn(e);
        }
    }

    @Override
    public int getIdleThreads() {
        return executor.getPoolSize() - executor.getActiveCount();
    }

    @Override
    public int getThreads() {
        return executor.getPoolSize();
    }

    @Override
    public boolean isLowOnThreads() {
        return executor.getActiveCount() >= executor.getMaximumPoolSize();
    }

    @Override
    public void join() throws InterruptedException {
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isFailed() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return !executor.isTerminated() && !executor.isTerminating();
    }

    @Override
    public boolean isStarted() {
        return !executor.isTerminated() && !executor.isTerminating();
    }

    @Override
    public boolean isStarting() {
        return false;
    }

    @Override
    public boolean isStopped() {
        return executor.isTerminated();
    }

    @Override
    public boolean isStopping() {
        return executor.isTerminating();
    }

    @Override
    public void start() throws Exception {
        if (executor.isTerminated() || executor.isTerminating()
                || executor.isShutdown()) {
            throw new IllegalStateException("Cannot restart");
        }
    }

    @Override
    public void stop() throws Exception {
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }

    @Override
    public void addLifeCycleListener(Listener listener) {
        System.err.println("we should implement this!");
    }

    @Override
    public void removeLifeCycleListener(Listener listener) {
        System.err.println("we should implement this!");
    }
}
