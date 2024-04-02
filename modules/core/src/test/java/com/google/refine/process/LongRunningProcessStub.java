
package com.google.refine.process;

/**
 * A long running process that we can actually run in the main thread of the test runner, because during tests it is
 * actually expected to be quick.
 * 
 * It wraps an existing LongRunningProcess
 * 
 * @author Antonin Delpeuch
 *
 */
public class LongRunningProcessStub extends LongRunningProcess {

    protected LongRunningProcess wrapped;

    public LongRunningProcessStub(Process process) {
        super("some description");
        this.wrapped = (LongRunningProcess) process;
    }

    public void run() {
        wrapped.getRunnable().run();
    }

    @Override
    protected Runnable getRunnable() {
        return wrapped.getRunnable();
    }

}
