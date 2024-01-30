
package org.openrefine.process;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.io.FileUtils;

import org.openrefine.browsing.Engine;
import org.openrefine.history.History;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataId;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.ChangeDataStore;

/**
 * A process which computes and stores the contents of a change data object to the store.
 */
public class ChangeDataStoringProcess<T> extends Process {

    final Optional<ChangeData<T>> storedChangeData;
    final ChangeDataId changeDataId;
    final ChangeDataStore changeDataStore;
    final ChangeDataSerializer<T> serializer;
    final Function<Optional<ChangeData<T>>, ChangeData<T>> completionProcess;
    final File temporaryDirToDelete;
    final History history;
    final int requiredStepIndex;

    /**
     * Constructor.
     *
     * @param description
     *            human-readable description of the process, shown to the user
     * @param storedChangeData
     *            any pre-existing change data which needs completing
     * @param changeDataId
     *            the id of the change data in the store
     * @param changeDataStore
     *            where to store the computed change data
     * @param serializer
     *            the serializer to use to persist the change data
     * @param completionProcess
     *            the process to use to compute the change data (or complete the existing one)
     * @param temporaryDirToDelete
     *            a temporary directory to delete at the end of the process (or null)
     * @param history
     *            the history of the project this computation pertains to
     * @param requiredStepIndex
     *            the step in the history which must be fully computed as a prerequisite for this process to run
     * @param engineMode
     *            whether this change data should be computed row- or record-wise
     */
    public ChangeDataStoringProcess(
            String description,
            Optional<ChangeData<T>> storedChangeData,
            ChangeDataId changeDataId,
            ChangeDataStore changeDataStore,
            ChangeDataSerializer<T> serializer, Function<Optional<ChangeData<T>>, ChangeData<T>> completionProcess,
            File temporaryDirToDelete,
            History history,
            int requiredStepIndex,
            Engine.Mode engineMode) {
        super(description, engineMode);
        this.storedChangeData = storedChangeData;
        this.changeDataId = changeDataId;
        this.changeDataStore = changeDataStore;
        this.serializer = serializer;
        this.completionProcess = completionProcess;
        this.temporaryDirToDelete = temporaryDirToDelete;
        this.history = history;
        this.requiredStepIndex = requiredStepIndex;
    }

    @Override
    @JsonIgnore
    protected ProgressingFuture<Void> getFuture() {
        // TODO we might want to run the completionProcess in the future itself, just in case this is expensive
        ChangeData<T> newChangeData = completionProcess.apply(storedChangeData);
        ProgressingFuture<Void> future = changeDataStore.storeAsync(newChangeData, changeDataId, serializer);
        FutureCallback<Void> callback = new FutureCallback<>() {

            @Override
            public void onSuccess(Void result) {
                if (temporaryDirToDelete != null && temporaryDirToDelete.exists()) {
                    try {
                        FileUtils.deleteDirectory(temporaryDirToDelete);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                // failure is handled by the logic in Process already
            }
        };
        Futures.addCallback(future, callback, MoreExecutors.directExecutor());
        future.onProgress(_reporter);
        return future;
    }

    @Override
    public boolean hasSatisfiedDependencies() {
        return history.isStreamableAtStep(requiredStepIndex);
    }

    @Override
    public ChangeDataId getChangeDataId() {
        return changeDataId;
    }

}
