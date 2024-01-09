/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.history;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.browsing.Engine;
import org.openrefine.model.ColumnId;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.model.changes.GridCache;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.RowMapOperation;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.util.NamingThreadFactory;

/**
 * Track done and undone changes. Done changes can be undone; undone changes can be redone. Each change is actually not
 * tracked directly but through a history entry. The history entry stores only the metadata, while the change object
 * stores the actual data. Thus, the history entries are much smaller and can be kept in memory, while the change
 * objects are only loaded into memory on demand.
 */
public class History {

    private static final Logger logger = LoggerFactory.getLogger(History.class);

    // required for passing as ChangeContext to all changes (who can rely on this, for instance in the cross function)
    @JsonProperty("projectId")
    protected final long _projectId;
    @JsonProperty("entries")
    protected List<HistoryEntry> _entries;
    @JsonProperty("position")
    protected int _position;
    /*
     * The position of the last expensive operation before the current position, which is cached in memory (space
     * permitting) or on disk
     */
    @JsonProperty("cachedPosition")
    protected int _cachedPosition = -1;
    /*
     * The step in the history that should ideally be cached given the current position. This might not be the actual
     * position because caching is in progress.
     */
    protected int _desiredCachedPosition = -1;
    /*
     * Position currently being cached, or -1 if no grid is currently being cached.
     */
    protected int _positionBeingCached = -1;
    protected Future<Void> _cachingFuture = null;
    protected final ExecutorService _cachingExecutorService;

    @JsonIgnore
    protected List<Step> _steps;
    @JsonIgnore
    protected ChangeDataStore _dataStore;
    @JsonIgnore
    protected GridCache _gridStore;
    @JsonIgnore
    protected Instant _lastModified;

    /**
     * A step in the history, which is a {@link Grid} with associated metadata.
     */
    protected static class Step {

        protected Grid grid;
        protected ChangeResult changeResult;
        // stores whether the grid at the same index is read directly from disk or not
        protected boolean cachedOnDisk;
        // stores whether the grid depends on change data being fetched, and is therefore worth refreshing regularly
        protected boolean inProgress;
        // if in progress, this flag stores whether all the operations since the last step that is fully computed are
        // row/record wise.
        // If this is the case, then it is safe to iterate synchronously from this step to compute another long-running
        // operation
        protected boolean streamable;

        protected Step(Grid grid, boolean cachedOnDisk, boolean inProgress, boolean streamable) {
            this.grid = grid;
            this.cachedOnDisk = cachedOnDisk;
            this.inProgress = inProgress;
            this.streamable = streamable;
        }
    }

    /**
     * Creates an empty on an initial grid.
     *
     * @param initialGrid
     *            the initial state of the project
     * @param dataStore
     *            where to store change data
     * @param gridStore
     *            where to store intermediate cached grids
     */
    public History(Grid initialGrid, ChangeDataStore dataStore, GridCache gridStore, long projectId) {
        _entries = new ArrayList<>();
        _steps = new ArrayList<>();
        _steps.add(new Step(initialGrid, false, false, false));
        _position = 0;
        _dataStore = dataStore;
        _gridStore = gridStore;
        _projectId = projectId;
        _cachingExecutorService = Executors.newFixedThreadPool(1, new NamingThreadFactory("History-" + projectId + "-caching"));
        _lastModified = Instant.now();
    }

    /**
     * Constructs a history with an initial grid and a list of history entries.
     *
     * @param initialGrid
     *            the first grid of the project, at creation time
     * @param dataStore
     *            where change data is stored for all changes of the project
     * @param entries
     *            the list of entries of the history
     * @param position
     *            the current position in the history
     * @throws OperationException
     *             if one step in the list of history entries failed to apply to the supplied grid
     */
    public History(
            Grid initialGrid,
            ChangeDataStore dataStore,
            GridCache gridStore,
            List<HistoryEntry> entries,
            int position,
            long projectId) throws OperationException {
        this(initialGrid, dataStore, gridStore, projectId);
        Set<Long> availableCachedStates = gridStore.listCachedGridIds();
        _position = 0;
        for (HistoryEntry entry : entries) {
            Grid grid = null;
            if (availableCachedStates.contains(entry.getId())) {
                try {
                    grid = gridStore.getCachedGrid(entry.getId());
                } catch (IOException e) {
                    logger.warn(String.format("Ignoring cached grid for history entry %d as it cannot be loaded:", entry.getId()));
                    e.printStackTrace();
                }
            }
            _steps.add(new Step(grid, grid != null, false, false));
            _entries.add(entry);
        }

        // ensure the grid of the current position is computed (invariant)
        getGrid(position, false);
        _position = position;
        updateCachedPosition();
    }

    /**
     * Wait for any caching operation currently being executed asynchronously. Mostly for testing purposes.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void waitForCaching() throws InterruptedException, ExecutionException {
        while (_cachingFuture != null && !_cachingFuture.isDone()) {
            _cachingFuture.get();
        }
    }

    private void updateModified() {
        _lastModified = Instant.now();
    }

    /**
     * Returns the state of the grid at the current position in the history.
     */
    @JsonIgnore
    public Grid getCurrentGrid() {
        // the current state is always assumed to be computed already
        Grid grid = _steps.get(_position).grid;
        if (grid == null) {
            throw new IllegalStateException("The current grid has not been computed yet");
        }
        return grid;
    }

    /**
     * Is the current grid incomplete? If so, it should be refreshed with {@link #refreshCurrentGrid()} after a while.
     */
    @JsonIgnore
    public boolean currentGridNeedsRefreshing() {
        return _steps.get(_position).inProgress;
    }

    /**
     * Returns the state of the grid at before any operation was applied on it
     */
    @JsonIgnore
    public Grid getInitialGrid() {
        // the initial state is always assumed to be computed already
        if (_steps.get(0).grid == null) {
            throw new IllegalStateException("The initial grid has not been computed yet");
        }
        return _steps.get(0).grid;
    }

    /**
     * Returns the state of the grid at a given index in the history
     *
     * @param position
     *            a 0-based index in the list of successive grids
     * @param refresh
     *            whether the grid should be refreshed if it depends on change data being currently fetched
     */
    public synchronized Grid getGrid(int position, boolean refresh) throws OperationException {
        Step step = _steps.get(position);
        Grid grid = step.grid;
        if (grid != null && !(refresh && step.inProgress)) {
            return grid;
        } else {
            // this state has not been computed yet,
            // so we compute it recursively from the previous one.
            // we know for sure that position > 0 because the initial grid
            // is always present
            Grid previous = getGrid(position - 1, refresh);
            HistoryEntry entry = _entries.get(position - 1);
            ChangeContext context = ChangeContext.create(entry.getId(), _projectId, position - 1, this, _dataStore, entry.getDescription());
            Operation operation = entry.getOperation();
            ChangeResult changeResult = operation.apply(previous, context);
            if (changeResult.getGridPreservation() != entry.getGridPreservation()) {
                _entries.set(position - 1, new HistoryEntry(
                        entry.getId(), entry.getOperation(), entry.getTime(), changeResult.getGridPreservation()));
            }
            step.grid = markColumnsAsModified(changeResult, operation, entry.getId());
            Step previousStep = _steps.get(position - 1);
            step.changeResult = changeResult;
            step.inProgress = previousStep.inProgress || _dataStore.needsRefreshing(entry.getId());
            step.streamable = (previousStep.streamable || !previousStep.inProgress) && operation instanceof RowMapOperation;
            step.cachedOnDisk = false;
            return step.grid;
        }
    }

    /**
     * Updates the column model of a grid computed by an operation to make sure that the columns are all marked as
     * modified unless the operation was able to precisely isolate the columns it modified.
     *
     * @param changeResult
     *            the outcome of running the operation
     * @return a copy of the grid with updated columns
     */
    protected static Grid markColumnsAsModified(ChangeResult changeResult, Operation operation, long historyEntryId) {
        if (operation instanceof RowMapOperation) {
            return changeResult.getGrid();
        } else {
            Grid grid = changeResult.getGrid();
            ColumnModel columnModel = grid.getColumnModel();
            return grid.withColumnModel(columnModel.markColumnsAsModified(historyEntryId));
        }
    }

    @JsonProperty("position")
    public int getPosition() {
        return _position;
    }

    @JsonProperty("currentEntryId")
    public long getCurrentEntryId() {
        if (_position == 0) {
            return 0L;
        } else {
            return _entries.get(_position - 1).getId();
        }
    }

    @JsonProperty("cachedPosition")
    public int getCachedPosition() {
        return _cachedPosition;
    }

    @JsonProperty("entries")
    public List<HistoryEntry> getEntries() {
        return _entries;
    }

    @JsonIgnore
    public ChangeDataStore getChangeDataStore() {
        return _dataStore;
    }

    @JsonIgnore
    public GridCache getGridCache() {
        return _gridStore;
    }

    /**
     * The last time this history was modified (or created).
     */
    @JsonIgnore
    public Instant getLastModified() {
        return _lastModified;
    }

    /**
     * Applies an operation on top of the existing history. This will modify this instance. If the operation application
     * failed, the exception will be returned in {@link OperationApplicationResult#getException()}.
     *
     * @param operation
     *            the operation to apply.
     */
    public OperationApplicationResult addEntry(Operation operation) {
        return addEntry(HistoryEntry.allocateID(), operation);
    }

    /**
     * Adds a {@link HistoryEntry} to the list of past histories. Adding a new entry clears all currently held future
     * histories
     */
    public synchronized OperationApplicationResult addEntry(long id, Operation operation) {
        // Any new change will clear all future entries.
        deleteFutureEntries();

        HistoryEntry entry = new HistoryEntry(id, operation, null);
        _entries.add(entry);
        _steps.add(new Step(null, false, false, false));
        try {
            // compute the grid at the new position (this applies the operation properly speaking)
            getGrid(_position + 1, false);
            _position++;
            updateCachedPosition();
            updateModified();
            return new OperationApplicationResult(entry, _steps.get(_position).changeResult);
        } catch (OperationException e) {
            return new OperationApplicationResult(e);
        }

    }

    /**
     * Permanently discards all history entries that are in the future.
     */
    public synchronized void deleteFutureEntries() {
        if (_position != _entries.size()) {
            logger.warn(String.format("Removing undone history entries from %d to %d", _position, _entries.size()));
            // stop the caching process if we are caching a grid in the future
            if (_positionBeingCached > _position && _cachingFuture != null && !_cachingFuture.isDone()) {
                _positionBeingCached = -1;
                _cachingFuture.cancel(true);
            }
            // uncache all the grids that we are removing
            for (int i = _position; i < _entries.size(); i++) {
                HistoryEntry oldEntry = _entries.get(i);
                _dataStore.discardAll(oldEntry.getId());
                Step step = _steps.get(i);
                if (step.cachedOnDisk) {
                    try {
                        _gridStore.uncacheGrid(oldEntry.getId());
                    } catch (IOException e) {
                        logger.warn("Ignoring deletion of unreachable cached grid because it failed:");
                        e.printStackTrace();
                    }
                }
                if (step.grid != null && step.grid.isCached()) {
                    step.grid.uncache();
                }
            }
            _entries = _entries.subList(0, _position);
            _steps = _steps.subList(0, _position + 1);
        }
    }

    protected void cacheGridOnDisk(int position) throws OperationException, IOException {
        Validate.isTrue(position > 0, "attempting to cache the initial grid to disk, this is unnecessary");
        // first, ensure that the grid is computed
        Grid grid = getGrid(position, false);
        long historyEntryId = _entries.get(position - 1).getId();
        // this operation can take a long time, so we avoid running it in a synchronized block
        // to make it possible to apply further operations while this caching is happening.
        Grid cached = _gridStore.cacheGrid(historyEntryId, grid);
        synchronized (this) {
            // check that the history has not changed since we started caching this grid
            if (_entries.get(position - 1).getId() != historyEntryId) {
                _gridStore.uncacheGrid(historyEntryId);
                return;
            }
            Step step = _steps.get(position);
            if (step.grid != null && step.grid.isCached()) {
                return; // this grid is already cached in memory, no need to replace it with the one from the disk
            }
            step.grid = cached;
            step.cachedOnDisk = true;
            // invalidate the following states until the next cached grid
            for (int i = position + 1; i < _steps.size() && !_steps.get(i).cachedOnDisk; i++) {
                _steps.get(i).grid = null;
            }
            // make sure the current position is computed
            getGrid(_position, false);
        }
    }

    protected synchronized void uncacheGridFromDisk(int position) throws IOException, OperationException {
        Validate.isTrue(position > 0, "attempting to uncache the initial grid from the disk, this is impossible");
        long historyEntryId = _entries.get(position - 1).getId();
        if (!_gridStore.listCachedGridIds().contains(historyEntryId)) {
            // nothing to do
            return;
        }
        // invalidate all further grids until the next disk-cached one
        for (int i = position + 1; i < _steps.size() && !_steps.get(i).cachedOnDisk; i++) {
            _steps.get(i).grid = null;
        }
        // uncache the grid
        Step step = _steps.get(position);
        _gridStore.uncacheGrid(historyEntryId);
        step.grid = null;
        step.cachedOnDisk = false;
        // make sure the current position is computed
        getGrid(_position, false);
    }

    protected synchronized void updateCachedPosition() {
        // Find the last expensive operation before the current one.
        int newCachedPosition = _position;
        while (newCachedPosition > 0 &&
                (!isChangeExpensive(newCachedPosition - 1) || _steps.get(newCachedPosition).inProgress) &&
                _steps.get(newCachedPosition - 1).grid != null) {
            newCachedPosition--;
        }

        if (newCachedPosition == _desiredCachedPosition || _steps.get(newCachedPosition).inProgress) {
            logger.info("No cached position change needed, it remains {}", _desiredCachedPosition);
            return;
        }
        logger.info("Changing cached position from {} to {}", _desiredCachedPosition, newCachedPosition);
        if (_positionBeingCached != -1) {
            if (_positionBeingCached > newCachedPosition) {
                // the position currently being cached is
                // not interesting for us anymore, because the position
                // we want to cache does not rely on it.
                if (_cachingFuture != null) {
                    _positionBeingCached = -1;
                    _cachingFuture.cancel(true);
                    // TODO cleanup any leftover files on disk
                }
            } else { // _positionBeingCached < newCachedPosition
                return;
            }
        }
        _desiredCachedPosition = newCachedPosition;

        // Start caching the desired position, since we now know
        // that no other caching is currently happening.
        _positionBeingCached = newCachedPosition;
        int toCache = newCachedPosition;
        _cachingFuture = _cachingExecutorService.submit(() -> {
            Grid newCachedState = _steps.get(toCache).grid;
            Grid oldCachedState = _cachedPosition >= 0 ? _steps.get(_cachedPosition).grid : null;
            int oldCachedPosition = _cachedPosition;
            boolean cachedSuccessfully = false;
            try {
                if (_cachedPosition > toCache) {
                    logger.info("Uncaching previous cached position {}", _cachedPosition);
                    oldCachedState.uncache();
                    uncacheGridFromDisk(_cachedPosition);
                }
                // try to cache in memory first
                logger.info("Caching new position {}", toCache);
                Instant cachingStart = Instant.now();
                cachedSuccessfully = newCachedState.cache();
                Instant cachingEnd = Instant.now();
                if (cachedSuccessfully) {
                    logger.info("Successfully cached position {} in memory in {} ms", toCache,
                            cachingEnd.toEpochMilli() - cachingStart.toEpochMilli());
                    _cachedPosition = toCache;
                    if (oldCachedState != null && _cachedPosition < toCache) {
                        oldCachedState.uncache();
                        uncacheGridFromDisk(oldCachedPosition);
                    }
                }
                // also cache the grid on disk if it is not the initial one
                if (toCache > 0) {
                    cacheGridOnDisk(toCache);
                    _cachedPosition = toCache;
                }
            } finally {
                synchronized (History.this) {
                    _positionBeingCached = -1;
                    updateCachedPosition();
                }
            }
            return null;
        });
    }

    /**
     * Determines if the change at the given index was expensive to compute or not.
     */
    private boolean isChangeExpensive(int index) {
        long historyEntryId = _entries.get(index).getId();
        return !_dataStore.getChangeDataIds(historyEntryId).isEmpty();
    }

    /**
     * Locates the earliest step in the history from which a {@link org.openrefine.model.changes.ChangeData} can be
     * computed. This requires the following conditions:
     * <ul>
     * <li>the step must be at or before the step provided as argument;</li>
     * <li>the grid at this step must contain all column dependencies supplied as second argument;</li>
     * <li>all of the transformations between the two steps must preserve row ids. If the change is computed in records
     * mode, then they must also preserve record ids.</li>
     * </ul>
     *
     * @param beforeStepIndex
     *            upper bound on the index to return, which must contain the column dependencies supplied. This means
     *            that the grid at this step must be present (while not necessarily complete).
     * @param dependencies
     *            list of column dependencies, or null if dependencies could not be isolated, in which case the supplied
     *            upper bound will be returned
     * @param engineMode
     *            the mode of the engine, determining the type of grid preservation to require
     * @return the index of the step
     */
    public int earliestStepContainingDependencies(int beforeStepIndex, List<ColumnId> dependencies, Engine.Mode engineMode) {
        if (dependencies == null || beforeStepIndex == 0) {
            return beforeStepIndex;
        } else {
            List<ColumnId> fullDependencies = dependencies;
            if (engineMode == Engine.Mode.RecordBased) {
                // check that the record key is listed as a dependency, and add it otherwise
                Grid grid = _steps.get(beforeStepIndex).grid;
                if (grid == null) {
                    throw new IllegalStateException("The latest grid to apply the operation on is not computed yet");
                }
                ColumnModel originalColumnModel = grid.getColumnModel();
                int keyColumnIndex = originalColumnModel.getKeyColumnIndex();
                if (keyColumnIndex < 0 || keyColumnIndex >= originalColumnModel.getColumns().size()) {
                    throw new IllegalStateException("Invalid key column index to run a record-based operation");
                }
                ColumnId keyColumnId = originalColumnModel.getColumnByIndex(keyColumnIndex).getColumnId();
                if (!dependencies.contains(keyColumnId)) {
                    fullDependencies = new ArrayList<>(dependencies.size() + 1);
                    // add the key column in first position by convention
                    fullDependencies.add(keyColumnId);
                    fullDependencies.addAll(dependencies);
                }
            }
            int currentIndex = beforeStepIndex - 1;
            while (currentIndex >= 0 &&
                    (_entries.get(currentIndex).getGridPreservation() != GridPreservation.NO_ROW_PRESERVATION) &&
                    (engineMode == Engine.Mode.RowBased
                            || _entries.get(currentIndex).getGridPreservation() == GridPreservation.PRESERVES_RECORDS)
                    &&
                    stepSatisfiesDependencies(currentIndex, fullDependencies)) {
                currentIndex--;
            }
            return currentIndex + 1;
        }
    }

    private boolean stepSatisfiesDependencies(int index, List<ColumnId> dependencies) {
        Grid grid = _steps.get(index).grid;
        if (grid == null) {
            return false;
        }
        ColumnModel columnModel = grid.getColumnModel();
        return dependencies.stream()
                .allMatch(columnModel::hasColumnId);
    }

    /**
     * Is the grid already fully computed at this step in the history?
     *
     * @param stepIndex
     *            the 0-based index of the step of the request
     */
    public boolean isFullyComputedAtStep(int stepIndex) {
        refreshGrid(stepIndex);
        return !_steps.get(stepIndex).inProgress;
    }

    /**
     * Is it possible to iterate from the grid synchronously at this step in the history? If the grid is already fully
     * computed, then it is possible. Otherwise, it is only possible if all the operations leading to this step which
     * have not been fully computed yet are row/record wise (instances of {@link RowMapOperation}).
     *
     * @param stepIndex
     *            the 0-based index of the requested step
     */
    public boolean isStreamableAtStep(int stepIndex) {
        refreshGrid(stepIndex);
        return isFullyComputedAtStep(stepIndex) || _steps.get(stepIndex).streamable;
    }

    public void refreshCurrentGrid() {
        refreshGrid(_position);
    }

    /**
     * Refresh a grid which can potentially need refreshing.
     */
    protected void refreshGrid(int position) {
        Validate.isTrue(position < _steps.size() && _steps.get(position).grid != null);
        try {
            getGrid(position, true);
        } catch (OperationException e) {
            throw new IllegalStateException("Recomputing an existing grid failed", e);
        }
    }

    public List<HistoryEntry> getLastPastEntries(int count) {

        if (count <= 0) {
            return _entries.subList(0, _position);
        } else {
            return _entries.subList(Math.max(0, _position - count), _position);
        }
    }

    /**
     * Rewinds or brings the history forward.
     *
     * @param lastDoneEntryID
     *            the id of the last change to be performed before the desired state of the project. Use 0L for the
     *            initial state.
     * @return the degree to which the grid was preserved while changing the position in the history
     * @throws OperationException
     *             if the application of changes required for this move did not succeed
     */
    public synchronized GridPreservation undoRedo(long lastDoneEntryID) throws OperationException {
        int oldPosition = _position;
        if (lastDoneEntryID == 0) {
            _position = 0;
        } else {
            _position = entryIndex(lastDoneEntryID) + 1;
            getGrid(_position, false);
        }

        GridPreservation gridPreservation = _position == oldPosition ? GridPreservation.PRESERVES_RECORDS
                : _entries.subList(Math.min(oldPosition, _position), Math.max(oldPosition, _position)).stream()
                        .map(item -> item.getGridPreservation() == null ? GridPreservation.NO_ROW_PRESERVATION : item.getGridPreservation())
                        .min(Comparator.naturalOrder()).get();

        updateCachedPosition();
        updateModified();
        return gridPreservation;
    }

    /**
     * Given an history entry id, return the id of the preceding history entry, or -1 if there is none.
     */
    public synchronized long getPrecedingEntryID(long entryID) {
        if (entryID == 0) {
            return -1;
        } else {
            try {
                int index = entryIndex(entryID);
                return index == 0 ? 0 : _entries.get(index - 1).getId();
            } catch (IllegalArgumentException e) {
                return -1;
            }
        }
    }

    /**
     * Uncaches all grids from memory and stops all related fetching and caching processes.
     */
    public void dispose() {
        _cachingExecutorService.shutdown();
        if (_cachedPosition >= 0) {
            _steps.get(_cachedPosition).grid.uncache();
        }
        _dataStore.dispose();
    }

    /**
     * Return the position of the history entry with the supplied id, or throws {@link IllegalArgumentException} if that
     * id cannot be found.
     */
    public synchronized int entryIndex(long entryID) {
        for (int i = 0; i < _entries.size(); i++) {
            if (_entries.get(i).getId() == entryID) {
                return i;
            }
        }
        throw new IllegalArgumentException(String.format("History entry with id %d not found", entryID));
    }

    protected synchronized HistoryEntry getEntry(long entryID) {
        try {
            return _entries.get(entryIndex(entryID));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
