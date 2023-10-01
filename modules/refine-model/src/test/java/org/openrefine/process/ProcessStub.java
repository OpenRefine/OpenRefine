
package org.openrefine.process;

import java.util.concurrent.Callable;

import org.openrefine.model.changes.ChangeDataId;

public class ProcessStub extends Process {

    Callable<ProgressingFuture<Void>> future;
    ChangeDataId changeDataId;
    boolean satisfiedDependencies;

    protected ProcessStub(String description, ChangeDataId changeDataId, Callable<ProgressingFuture<Void>> future) {
        super(description);
        this.future = future;
        this.changeDataId = changeDataId;
        this.satisfiedDependencies = false;
    }

    public void setException(Exception e) {
        this._exception = e;
    }

    public void setSatisfiedDependencies(boolean value) {
        satisfiedDependencies = value;
    }

    @Override
    protected ProgressingFuture<Void> getFuture() {
        try {
            return future.call();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean hasSatisfiedDependencies() {
        return satisfiedDependencies;
    }

    @Override
    public ChangeDataId getChangeDataId() {
        return changeDataId;
    }
}
