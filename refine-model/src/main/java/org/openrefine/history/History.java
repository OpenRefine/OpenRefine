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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.RefineModel;
import org.openrefine.model.GridState;
import org.openrefine.model.changes.CachedGridStore;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeDataStore;

/**
 * Track done and undone changes. Done changes can be undone; undone changes can be redone. Each change is actually not
 * tracked directly but through a history entry. The history entry stores only the metadata, while the change object
 * stores the actual data. Thus the history entries are much smaller and can be kept in memory, while the change objects
 * are only loaded into memory on demand.
 */
public class History {

    private static final Logger logger = LoggerFactory.getLogger(History.class);

    @JsonProperty("entries")
    protected List<HistoryEntry> _entries;
    @JsonProperty("position")
    protected int _position;

    @JsonIgnore
    protected List<GridState> _states;
    // stores whether the grid state at the same index is read directly from disk or not
    @JsonIgnore
    protected List<Boolean> _cachedOnDisk;
    @JsonIgnore
    protected ChangeDataStore _dataStore;
    @JsonIgnore
    protected CachedGridStore _gridStore;

    /**
     * Creates an empty on an initial grid state.
     * 
     * @param initialGrid
     *            the initial state of the project
     * @param dataStore
     *            where to store change data
     * @param gridStore
     *            where to store intermediate cached grids
     */
    public History(GridState initialGrid, ChangeDataStore dataStore, CachedGridStore gridStore) {
        _entries = new ArrayList<>();
        _states = new ArrayList<>();
        _cachedOnDisk = new ArrayList<>();
        _states.add(initialGrid);
        _cachedOnDisk.add(true);
        // initialGrid.cache();
        _position = 0;
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
     */
    public History(
            GridState initialGrid,
            ChangeDataStore dataStore,
            CachedGridStore gridStore,
            List<HistoryEntry> entries,
            int position) throws DoesNotApplyException {
        this(initialGrid, dataStore, gridStore);
        Set<Long> availableCachedStates = gridStore.listCachedGridIds();
        for (HistoryEntry entry : entries) {
            GridState grid = null;
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

        // ensure the grid state of the current position is computed (invariant)
        getGridState(position);
        _position = position;
    }

    /**
     * Returns the state of the grid at the current position in the history.
     * 
     * @return
     */
    @JsonIgnore
    public GridState getCurrentGridState() {
        // the current state is always assumed to be computed already
        GridState gridState = _states.get(_position);
        if (gridState == null) {
            throw new IllegalStateException("The current grid state has not been computed yet");
        }
        if (false && !gridState.isCached()) {
            logger.info("Caching grid state");
            gridState.cache();
            String grid = gridState.toString();
            logger.info(grid);
            logger.info("Done caching grid state");
        }
        return gridState;
    }

    /**
     * Returns the state of the grid at before any operation was applied on it
     * 
     * @return
     */
    @JsonIgnore
    public GridState getInitialGridState() {
        // the initial state is always assumed to be computed already
        if (_states.get(0) == null) {
            throw new IllegalStateException("The initial grid state has not been computed yet");
        }
        return _states.get(0);
    }

    /**
     * Returns the state of the grid at a given index in the history
     * 
     * @param position
     *            a 0-based index in the list of successive grid states
     * @return
     */
    protected GridState getGridState(int position) throws DoesNotApplyException {
        GridState grid = _states.get(position);
        if (grid != null) {
            return grid;
        } else {
            // this state has not been computed yet,
            // so we compute it recursively from the previous one.
            // we know for sure that position > 0 because the initial grid
            // is always present
            GridState previous = getGridState(position - 1);
            HistoryEntry entry = _entries.get(position - 1);
            ChangeContext context = ChangeContext.create(entry.getId(), _dataStore);
            GridState newState = entry.getChange().apply(previous, context);
            _states.set(position, newState);
            _cachedOnDisk.set(position, false);
            return newState;
        }
    }

    @JsonProperty("position")
    public int getPosition() {
        return _position;
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
    public CachedGridStore getCachedGridStore() {
        return _gridStore;
    }

    /**
     * Adds a HistoryEntry to the list of past histories Adding a new entry clears all currently held future histories
     * 
     * @param entry
     * @throws DoesNotApplyException
     */
    public void addEntry(HistoryEntry entry) throws DoesNotApplyException {
        // Any new change will clear all future entries.
        if (_position != _entries.size()) {

            // uncache all the grid states that we are removing
            for (int i = _position; i < _entries.size(); i++) {
                HistoryEntry oldEntry = _entries.get(i);
                _dataStore.discardAll(oldEntry.getId());
                if (_cachedOnDisk.get(i)) {
                    try {
                        _gridStore.uncacheGrid(oldEntry.getId());
                    } catch (IOException e) {
                        logger.warn("Ignoring deletion of unreachable cached grid state because it failed:");
                        e.printStackTrace();
                    }
                }
            }
            _entries = _entries.subList(0, _position);
            _states = _states.subList(0, _position + 1);
            _cachedOnDisk = _cachedOnDisk.subList(0, _position + 1);
        }

        ChangeContext context = ChangeContext.create(entry.getId(), _dataStore);
        GridState newState = entry.getChange().apply(getCurrentGridState(), context);
        _states.add(newState);
        _cachedOnDisk.add(false);
        _entries.add(entry);
        _position++;
    }

    /**
     * Makes sure the current grid state is cached on disk, to make project loading faster
     *
     * @throws IOException
     */
    public void cacheCurrentGridState() throws IOException {
        if (_position > 0 && !_cachedOnDisk.get(_position)) {
            try {
                cacheIntermediateGrid(_entries.get(_position - 1).getId());
            } catch (DoesNotApplyException e) {
                // cannot happen, since we assume that the current grid state is always computed
                throw new IllegalStateException("Current grid state is not computed");
            }
        }
    }

    protected void cacheIntermediateGrid(long historyEntryID) throws DoesNotApplyException, IOException {
        int historyEntryIndex = entryIndex(historyEntryID);
        // first, ensure that the grid is computed
        GridState grid = getGridState(historyEntryIndex + 1);
        long historyEntryId = _entries.get(historyEntryIndex).getId();
        GridState cached = _gridStore.cacheGrid(historyEntryId, grid);
        synchronized (this) {
            _states.set(historyEntryIndex + 1, cached);
            _cachedOnDisk.set(historyEntryIndex + 1, true);
            // invalidate the following states until the next cached grid
            for (int i = historyEntryIndex + 2; i < _states.size() && !_cachedOnDisk.get(i); i++) {
                _states.set(i, null);
            }
            // make sure the current position is computed
            getGridState(_position);
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
            getGridState(_position);
        }
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
