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

import java.util.ArrayList;
import java.util.List;

import org.openrefine.RefineModel;
import org.openrefine.model.GridState;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeDataStore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Track done and undone changes. Done changes can be undone; undone changes can be redone.
 * Each change is actually not tracked directly but through a history entry. The history
 * entry stores only the metadata, while the change object stores the actual data. Thus
 * the history entries are much smaller and can be kept in memory, while the change objects
 * are only loaded into memory on demand.
 */
public class History  {

    @JsonProperty("entries")
    protected List<HistoryEntry>       _entries;
    @JsonProperty("position")
    protected int                      _position;
    
    @JsonIgnore
    protected List<GridState>    _states;
    @JsonIgnore
    protected ChangeDataStore    _dataStore;

    /**
     * Creates an empty on an initial grid state.
     * 
     * @param initialGrid
     *    the initial state of the project
     * @param dataStore
     *    where to store change data
     */
    public History(GridState initialGrid, ChangeDataStore dataStore) {
        _entries = new ArrayList<>();
        _states = new ArrayList<>();
        _states.add(initialGrid);
        _position = 0;
        _dataStore = dataStore;
    }
    
    /**
     * Constructs a history with an initial grid
     * and a list of history entries.
     * @param initialGrid
     * @param metadata
     * @throws DoesNotApplyException 
     */
    public History(
            GridState initialGrid,
            ChangeDataStore dataStore,
            List<HistoryEntry> entries,
            int position) throws DoesNotApplyException {
        this(initialGrid, dataStore);
        for(HistoryEntry entry : entries) {
            addEntry(entry);
        }
        _position = position;
    }

    /**
     * Returns the state of the grid at the current position in the
     * history.
     * @return
     */
    @JsonIgnore
    public GridState getCurrentGridState() {
        return _states.get(_position);
    }
    
    /**
     * Returns the state of the table at a certain index in the history.
     * @return
     */
    @JsonIgnore
	public GridState getInitialGridState() {
		return _states.get(0);
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

    /**
     * Adds a HistoryEntry to the list of past histories
     * Adding a new entry clears all currently held future histories
     * @param entry
     * @throws DoesNotApplyException 
     */
    public void addEntry(HistoryEntry entry) throws DoesNotApplyException {
        // Any new change will clear all future entries.
        if(_position != _entries.size()) {
            _entries = _entries.subList(0, _position);
            _states = _states.subList(0, _position+1);
        }
        
        ChangeContext context = ChangeContext.create(entry.getId(), _dataStore);
        GridState newState = entry.getChange().apply(getCurrentGridState(), context);
        _states.add(newState);
        _entries.add(entry);
        _position++;
    }

    public List<HistoryEntry> getLastPastEntries(int count) {
        
        if (count <= 0) {
            return _entries.subList(0, _position);
        } else {
            return _entries.subList(Math.max(0,_position-count), _position);
        }
    }

    synchronized public void undoRedo(long lastDoneEntryID) {
        if (lastDoneEntryID == 0) {
            _position = 0;
        } else {
            for (int i = 0; i < _entries.size(); i++) {
                if (_entries.get(i).getId() == lastDoneEntryID) {
                    _position = i+1;
                    return;
                }
            }
            throw new IllegalArgumentException(String.format("History entry id %d not found", lastDoneEntryID));
        }
    }

    synchronized public long getPrecedingEntryID(long entryID) {
        if (entryID == 0) {
            return -1;
        } else {
            for (int i = 0; i < _entries.size(); i++) {
                if (_entries.get(i).getId() == entryID) {
                    return i == 0 ? 0 : _entries.get(i - 1).getId();
                }
            }
        }
        return -1;
    }

    protected HistoryEntry getEntry(long entryID) {
        for (int i = 0; i < _entries.size(); i++) {
            if (_entries.get(i).getId() == entryID) {
                return _entries.get(i);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static public Class<? extends Change> getChangeClass(String className) throws ClassNotFoundException {
        return (Class<? extends Change>) RefineModel.getClass(className);
    }
}
