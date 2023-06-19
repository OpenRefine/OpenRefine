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

import org.apache.commons.lang.Validate;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.model.changes.GridCache;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.exceptions.OperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    /**
     * A step in the history, which is a {@link Grid} with associated metadata.
     */
    protected static class Step {

        protected Grid grid;
        // stores whether the grid depends on change data being fetched, and is therefore worth refreshing regularly
        protected boolean cachedOnDisk;
        // stores whether the grid at the same index is read directly from disk or not
        protected boolean inProgress;

        protected Step(Grid grid, boolean cachedOnDisk, boolean inProgress) {
            this.grid = grid;
            this.cachedOnDisk = cachedOnDisk;
            this.inProgress = inProgress;
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
        _steps.add(new Step(initialGrid, false, false));
        _position = 0;
        _dataStore = dataStore;
        _gridStore = gridStore;
        _projectId = projectId;
        _cachingExecutorService = Executors.newFixedThreadPool(1);
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
            _steps.add(new Step(grid, grid != null, false));
            _entries.add(entry);
        }

        // ensure the grid of the current position is computed (invariant)
        getGrid(position, false);
        _position = position;
        updateCachedPosition();
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
    protected synchronized Grid getGrid(int position, boolean refresh) throws OperationException {
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
            ChangeContext context = ChangeContext.create(entry.getId(), _projectId, _dataStore, entry.getDescription());
            Operation operation = entry.getOperation();
            Grid newState;
            newState = operation.apply(previous, context).getGrid();
            step.grid = newState;
            step.inProgress = _steps.get(position - 1).inProgress || _dataStore.needsRefreshing(entry.getId());
            step.cachedOnDisk = false;
            return newState;
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

        // TODO refactor this so that it does not duplicate the logic of getGrid
        ChangeContext context = ChangeContext.create(id, _projectId, _dataStore, operation.getDescription());
        ChangeResult changeResult = null;
        try {
            changeResult = operation.apply(getCurrentGrid(), context);
        } catch (OperationException e) {
            return new OperationApplicationResult(e);
        }
        Grid newState = changeResult.getGrid();
        HistoryEntry entry = new HistoryEntry(id, operation, changeResult.getGridPreservation());
        _steps.add(new Step(newState, false, _steps.get(_position).inProgress || _dataStore.needsRefreshing(entry.getId())));
        _entries.add(entry);
        _position++;
        updateCachedPosition();
        return new OperationApplicationResult(entry, changeResult);
    }

    protected synchronized void cacheIntermediateGridOnDisk(int position) throws OperationException, IOException {
        Validate.isTrue(position > 0);
        // first, ensure that the grid is computed
        Grid grid = getGrid(position, false);
        long historyEntryId = _entries.get(position - 1).getId();
        Grid cached = _gridStore.cacheGrid(historyEntryId, grid);
        synchronized (this) {
            Step step = _steps.get(position);
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
            boolean cachedSuccessfully = false;
            try {
                if (_cachedPosition > toCache) {
                    logger.info("Uncaching previous cached position {}", _cachedPosition);
                    oldCachedState.uncache();
                    // TODO uncache off disk if it is cached there
                }
                // try to cache in memory first
                logger.info("Caching new position {}", toCache);
                Instant cachingStart = Instant.now();
                cachedSuccessfully = newCachedState.cache();
                Instant cachingEnd = Instant.now();
                if (cachedSuccessfully) {
                    logger.info("Successfully cached position {} in {} ms", toCache,
                            cachingEnd.toEpochMilli() - cachingStart.toEpochMilli());
                    _cachedPosition = toCache;
                    if (oldCachedState != null && _cachedPosition < toCache) {
                        logger.info("Uncaching previous state");
                        oldCachedState.uncache();
                    }
                    // TODO uncache off disk if it is cached there
                } else {
                    // TODO cache on disk
                }
            } finally {
                logger.info("Done caching position {}", toCache);
                synchronized (History.this) {
                    if (cachedSuccessfully) {
                        _positionBeingCached = -1;
                    }
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

    public void refreshCurrentGrid() {
        try {
            getGrid(_position, true);
        } catch (OperationException e) {
            throw new IllegalStateException("Recomputing the current grid failed", e);
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

    /**
     * Utility method to wait for any caching operation currently being executed asynchronously. Mostly for testing
     * purposes.
     * 
     * @throws ExecutionException
     * @throws InterruptedException
     */
    protected void waitForCaching() throws InterruptedException, ExecutionException {
        while (_cachingFuture != null && !_cachingFuture.isDone()) {
            _cachingFuture.get();
        }
    }

}
