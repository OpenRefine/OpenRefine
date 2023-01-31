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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.openrefine.RefineModel;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.GridCache;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.operations.Operation;
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

    @JsonProperty("entries")
    protected List<HistoryEntry> _entries;
    @JsonProperty("position")
    protected int _position;
    /*
     * The position of the last expensive operation before the current position, which is cached in memory (space
     * permitting) or on disk
     */
    @JsonProperty("cachedPosition")
    protected int _cachedPosition;

    @JsonIgnore
    protected List<Grid> _states;
    // stores whether the grid at the same index is read directly from disk or not
    @JsonIgnore
    protected List<Boolean> _cachedOnDisk;
    @JsonIgnore
    protected ChangeDataStore _dataStore;
    @JsonIgnore
    protected GridCache _gridStore;

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
    public History(Grid initialGrid, ChangeDataStore dataStore, GridCache gridStore) {
        _entries = new ArrayList<>();
        _states = new ArrayList<>();
        _cachedOnDisk = new ArrayList<>();
        _states.add(initialGrid);
        _cachedOnDisk.add(true);
        _position = 0;
        _cachedPosition = 0;
        _dataStore = dataStore;
        _gridStore = gridStore;
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
     * @throws DoesNotApplyException
     *             if one step in the list of history entries failed to apply to the supplied grid
     */
    public History(
            Grid initialGrid,
            ChangeDataStore dataStore,
            GridCache gridStore,
            List<HistoryEntry> entries,
            int position) throws DoesNotApplyException {
        this(initialGrid, dataStore, gridStore);
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
            _states.add(grid);
            _cachedOnDisk.add(grid != null);
            _entries.add(entry);
        }

        // ensure the grid of the current position is computed (invariant)
        getGrid(position);
        _position = position;
        updateCachedPosition();
    }

    /**
     * Returns the state of the grid at the current position in the history.
     */
    @JsonIgnore
    public Grid getCurrentGrid() {
        // the current state is always assumed to be computed already
        Grid grid = _states.get(_position);
        if (grid == null) {
            throw new IllegalStateException("The current grid has not been computed yet");
        }
        return grid;
    }

    /**
     * Returns the state of the grid at before any operation was applied on it
     */
    @JsonIgnore
    public Grid getInitialGrid() {
        // the initial state is always assumed to be computed already
        if (_states.get(0) == null) {
            throw new IllegalStateException("The initial grid has not been computed yet");
        }
        return _states.get(0);
    }

    /**
     * Returns the state of the grid at a given index in the history
     * 
     * @param position
     *            a 0-based index in the list of successive grids
     */
    protected Grid getGrid(int position) throws DoesNotApplyException {
        Grid grid = _states.get(position);
        if (grid != null) {
            return grid;
        } else {
            // this state has not been computed yet,
            // so we compute it recursively from the previous one.
            // we know for sure that position > 0 because the initial grid
            // is always present
            Grid previous = getGrid(position - 1);
            HistoryEntry entry = _entries.get(position - 1);
            ChangeContext context = ChangeContext.create(entry.getId(), _dataStore);
            Change change = entry.getChange();
            Grid newState = change.apply(previous, context).getGrid();
            _states.set(position, newState);
            _cachedOnDisk.set(position, false);
            /*
             * // maybe a good optimization idea? but how do we keep track of which grid we have cached? if
             * (!change.isImmediate() && smallEnoughToCacheInMemory(newState)) { newState.cache(); }
             */
            return newState;
        }
    }

    @JsonProperty("position")
    public int getPosition() {
        return _position;
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
     * Adds a {@link HistoryEntry} to the list of past histories. Adding a new entry clears all currently held future
     * histories
     */
    public HistoryEntry addEntry(long id,
            String description,
            Operation operation,
            Change change) throws DoesNotApplyException, Operation.NotImmediateOperationException {
        // Any new change will clear all future entries.
        if (_position != _entries.size()) {

            // uncache all the grids that we are removing
            for (int i = _position; i < _entries.size(); i++) {
                HistoryEntry oldEntry = _entries.get(i);
                _dataStore.discardAll(oldEntry.getId());
                if (_cachedOnDisk.get(i)) {
                    try {
                        _gridStore.uncacheGrid(oldEntry.getId());
                    } catch (IOException e) {
                        logger.warn("Ignoring deletion of unreachable cached grid because it failed:");
                        e.printStackTrace();
                    }
                }
            }
            _entries = _entries.subList(0, _position);
            _states = _states.subList(0, _position + 1);
            _cachedOnDisk = _cachedOnDisk.subList(0, _position + 1);
        }

        ChangeContext context = ChangeContext.create(id, _dataStore);
        Change.ChangeResult changeResult = change.apply(getCurrentGrid(), context);
        Grid newState = changeResult.getGrid();
        HistoryEntry entry = new HistoryEntry(id, description, operation, change, changeResult.getGridPreservation());
        _states.add(newState);
        _cachedOnDisk.add(false);
        _entries.add(entry);
        _position++;
        updateCachedPosition();
        return entry;
    }

    protected void cacheIntermediateGridOnDisk(int position) throws DoesNotApplyException, IOException {
        Validate.isTrue(position > 0);
        // first, ensure that the grid is computed
        Grid grid = getGrid(position);
        long historyEntryId = _entries.get(position - 1).getId();
        Grid cached = _gridStore.cacheGrid(historyEntryId, grid);
        synchronized (this) {
            _states.set(position, cached);
            _cachedOnDisk.set(position, true);
            // invalidate the following states until the next cached grid
            for (int i = position + 1; i < _states.size() && !_cachedOnDisk.get(i); i++) {
                _states.set(i, null);
            }
            // make sure the current position is computed
            getGrid(_position);
        }
    }

    protected void updateCachedPosition() {
        int previousCachedPosition = _cachedPosition;
        // Find the last expensive operation before the current one.
        int newCachedPosition = _position;
        while (newCachedPosition > 0 &&
                _entries.get(newCachedPosition - 1).getChange().isImmediate() && // we found an expensive change
                _states.get(newCachedPosition - 1) != null // or we found a grid that is not computed yet, meaning it
                                                           // (or anything before it) is not currently needed
        ) {
            newCachedPosition--;
        }
        // Cache the new position
        _cachedPosition = newCachedPosition;
        Grid newCachedState = _states.get(newCachedPosition);
        boolean cachedSuccessfully = newCachedState.cache();
        if (!cachedSuccessfully) {
            // TODO cache on disk
        }

        if (newCachedPosition != previousCachedPosition) {
            _states.get(previousCachedPosition).uncache();
            if (previousCachedPosition > 0 && _cachedOnDisk.get(previousCachedPosition)) {
                // TODO uncache off disk
            }
        }
    }

    public List<HistoryEntry> getLastPastEntries(int count) {

        if (count <= 0) {
            return _entries.subList(0, _position);
        } else {
            return _entries.subList(Math.max(0, _position - count), _position);
        }
    }

    synchronized public void undoRedo(long lastDoneEntryID) throws DoesNotApplyException {
        if (lastDoneEntryID == 0) {
            _position = 0;
        } else {
            _position = entryIndex(lastDoneEntryID) + 1;
            getGrid(_position);
        }
        updateCachedPosition();
    }

    synchronized public long getPrecedingEntryID(long entryID) {
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

    protected HistoryEntry getEntry(long entryID) {
        try {
            return _entries.get(entryIndex(entryID));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    protected int entryIndex(long entryID) {
        for (int i = 0; i < _entries.size(); i++) {
            if (_entries.get(i).getId() == entryID) {
                return i;
            }
        }
        throw new IllegalArgumentException(String.format("History entry with id %d not found", entryID));
    }

    @SuppressWarnings("unchecked")
    static public Class<? extends Change> getChangeClass(String className) throws ClassNotFoundException {
        return (Class<? extends Change>) RefineModel.getClass(className);
    }
}
