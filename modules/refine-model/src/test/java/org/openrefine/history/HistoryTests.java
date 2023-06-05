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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openrefine.expr.ParsingException;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.model.changes.GridCache;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.exceptions.OperationException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HistoryTests {

    ChangeDataStore dataStore;
    GridCache gridStore;

    Grid initialState;
    Grid intermediateState;
    Grid finalState;
    Grid newState;

    ChangeResult intermediateResult;
    ChangeResult finalResult;
    ChangeResult newResult;

    ColumnModel columnModel;

    long firstChangeId = 1234L;
    long secondChangeId = 5678L;
    long newChangeId = 9012L;

    Operation firstOperation;
    Operation secondOperation;
    Operation newOperation;
    Operation failingOperation;

    HistoryEntry firstEntry;
    HistoryEntry secondEntry;
    HistoryEntry newEntry;

    List<HistoryEntry> entries;

    @BeforeMethod
    public void setUp() throws OperationException, ParsingException {
        dataStore = mock(ChangeDataStore.class);
        gridStore = mock(GridCache.class);
        initialState = mock(Grid.class);
        intermediateState = mock(Grid.class);
        newState = mock(Grid.class);
        finalState = mock(Grid.class);
        columnModel = new ColumnModel(Arrays.asList(new ColumnMetadata("foo")));
        firstOperation = mock(Operation.class);
        secondOperation = mock(Operation.class);
        newOperation = mock(Operation.class);
        failingOperation = mock(Operation.class);
        firstEntry = mock(HistoryEntry.class);
        secondEntry = mock(HistoryEntry.class);
        newEntry = mock(HistoryEntry.class);
        intermediateResult = mock(ChangeResult.class);
        finalResult = mock(ChangeResult.class);
        newResult = mock(ChangeResult.class);

        when(firstOperation.apply(eq(initialState), any())).thenReturn(intermediateResult);
        when(intermediateResult.getGrid()).thenReturn(intermediateState);
        when(firstOperation.isReproducible()).thenReturn(false);
        when(secondOperation.apply(eq(intermediateState), any())).thenReturn(finalResult);
        when(finalResult.getGrid()).thenReturn(finalState);
        when(secondOperation.isReproducible()).thenReturn(true);
        when(newOperation.apply(eq(intermediateState), any())).thenReturn(newResult);
        when(newResult.getGrid()).thenReturn(newState);
        when(newOperation.isReproducible()).thenReturn(true);
        when(failingOperation.apply(eq(intermediateState), any()))
                .thenThrow(new OperationException("some_error", "Some error occured"));

        when(initialState.getColumnModel()).thenReturn(columnModel);
        when(intermediateState.getColumnModel()).thenReturn(columnModel);
        when(newState.getColumnModel()).thenReturn(columnModel);
        when(finalState.getColumnModel()).thenReturn(columnModel);
        when(initialState.rowCount()).thenReturn(8L);
        when(intermediateState.rowCount()).thenReturn(10L);
        when(finalState.rowCount()).thenReturn(12L);
        when(newState.rowCount()).thenReturn(10000000L);

        when(firstEntry.getId()).thenReturn(firstChangeId);
        when(secondEntry.getId()).thenReturn(secondChangeId);
        when(newEntry.getId()).thenReturn(newChangeId);

        when(firstEntry.getOperation()).thenReturn(firstOperation);
        when(secondEntry.getOperation()).thenReturn(secondOperation);
        when(newEntry.getOperation()).thenReturn(newOperation);

        when(firstEntry.getGridPreservation()).thenReturn(GridPreservation.PRESERVES_RECORDS);
        when(secondEntry.getGridPreservation()).thenReturn(GridPreservation.PRESERVES_ROWS);
        when(newEntry.getGridPreservation()).thenReturn(GridPreservation.NO_ROW_PRESERVATION);

        entries = Arrays.asList(firstEntry, secondEntry);
    }

    @Test(enabled = false) // TODO reenable after restoring caching
    public void testConstruct() throws OperationException, ParsingException {
        when(gridStore.listCachedGridIds()).thenReturn(Collections.emptySet());

        History history = new History(initialState, dataStore, gridStore, entries, 1, 1234L);

        Assert.assertEquals(history.getPosition(), 1);
        Assert.assertEquals(history.getCachedPosition(), 1); // the first operation is expensive, so this state is
                                                             // cached
        Assert.assertEquals(history.getCurrentGrid(), intermediateState);
        verify(history.getCurrentGrid(), times(1)).cache();
        Assert.assertEquals(history.getEntries(), entries);

        GridPreservation gridPreservation = history.undoRedo(secondChangeId);

        Assert.assertEquals(history.getPosition(), 2);
        Assert.assertEquals(history.getCurrentEntryId(), secondChangeId);
        Assert.assertEquals(history.getCachedPosition(), 1); // the second operation is not expensive
        Assert.assertEquals(history.getCurrentGrid(), finalState);
        Assert.assertEquals(history.getEntries(), entries);
        Assert.assertEquals(gridPreservation, GridPreservation.PRESERVES_ROWS);

        GridPreservation gridPreservation2 = history.undoRedo(0);

        Assert.assertEquals(history.getPosition(), 0);
        Assert.assertEquals(history.getCurrentEntryId(), 0L);
        Assert.assertEquals(history.getCachedPosition(), 0);
        Assert.assertEquals(history.getCurrentGrid(), initialState);
        Assert.assertEquals(history.getEntries(), entries);
        Assert.assertEquals(gridPreservation2, GridPreservation.PRESERVES_ROWS);

        // All changes were called only once
        verify(firstOperation, times(1)).apply(eq(initialState), any());
        verify(secondOperation, times(1)).apply(eq(intermediateState), any());

        // Test some getters too
        Assert.assertEquals(history.getEntry(firstChangeId), firstEntry);
        Assert.assertEquals(history.getPrecedingEntryID(secondChangeId), firstChangeId);
        Assert.assertEquals(history.getPrecedingEntryID(firstChangeId), 0L);
        Assert.assertEquals(history.getPrecedingEntryID(0L), -1L);
    }

    @Test
    public void testApplyFailingOperation() throws OperationException {
        when(gridStore.listCachedGridIds()).thenReturn(Collections.emptySet());

        History history = new History(initialState, dataStore, gridStore, entries, 1, 1234L);
        Assert.assertEquals(history.getPosition(), 1);
        Assert.assertEquals(history.getCurrentGrid(), intermediateState);

        OperationApplicationResult result = history.addEntry(failingOperation);
        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(result.getException().getCode(), "some_error");
        Assert.assertEquals(result.getException().getMessage(), "Some error occured");
    }

    @Test
    public void testConstructWithCachedGrids() throws OperationException, IOException, ParsingException {
        HistoryEntry thirdEntry = mock(HistoryEntry.class);
        Operation thirdChange = mock(Operation.class);
        Grid thirdState = mock(Grid.class);
        Grid fourthState = mock(Grid.class);
        ChangeResult changeResult = mock(ChangeResult.class);

        when(gridStore.listCachedGridIds()).thenReturn(Collections.singleton(secondChangeId));
        when(gridStore.getCachedGrid(secondChangeId)).thenReturn(thirdState);
        when(thirdEntry.getOperation()).thenReturn(thirdChange);
        when(changeResult.getGrid()).thenReturn(fourthState);
        when(thirdChange.apply(eq(thirdState), any())).thenReturn(changeResult);

        List<HistoryEntry> fullEntries = Arrays.asList(firstEntry, secondEntry, thirdEntry);
        History history = new History(initialState, dataStore, gridStore, fullEntries, 3, 1234L);

        // Inspect the states which are loaded and those which aren't
        Assert.assertEquals(history._states.get(0), initialState);
        Assert.assertNull(history._states.get(1));
        Assert.assertEquals(history._states.get(2), thirdState); // loaded from cache
        Assert.assertEquals(history._states.get(3), fourthState);
    }

    @Test
    public void testCacheIntermediateGrid() throws OperationException, IOException, ParsingException {
        HistoryEntry thirdEntry = mock(HistoryEntry.class);
        Operation thirdOperation = mock(Operation.class);
        Grid fourthState = mock(Grid.class);
        Grid cachedSecondState = mock(Grid.class);
        Grid rederivedThirdState = mock(Grid.class);
        ChangeResult fourthResult = mock(ChangeResult.class);
        ChangeResult rederivedThirdResult = mock(ChangeResult.class);
        long thirdEntryId = 39827L;

        when(gridStore.listCachedGridIds()).thenReturn(Collections.emptySet());
        when(gridStore.cacheGrid(firstChangeId, intermediateState)).thenReturn(cachedSecondState);
        when(thirdEntry.getOperation()).thenReturn(thirdOperation);
        when(thirdOperation.apply(eq(finalState), any())).thenReturn(fourthResult);
        when(fourthResult.getGrid()).thenReturn(fourthState);
        when(thirdEntry.getId()).thenReturn(thirdEntryId);
        when(secondOperation.apply(eq(cachedSecondState), any())).thenReturn(rederivedThirdResult);
        when(rederivedThirdResult.getGrid()).thenReturn(rederivedThirdState);

        List<HistoryEntry> fullEntries = Arrays.asList(firstEntry, secondEntry, thirdEntry);
        History history = new History(initialState, dataStore, gridStore, fullEntries, 3, 1234L);
        // make sure all changes have been derived
        Assert.assertEquals(history._states.get(3), fourthState);
        // Set position to two
        history.undoRedo(secondChangeId);

        history.cacheIntermediateGridOnDisk(1);

        // the state was cached
        Assert.assertEquals(history._states.get(1), cachedSecondState);
        // the current state was rederived from the cached state
        Assert.assertEquals(history._states.get(2), rederivedThirdState);
        // any further non-cached state is discarded
        Assert.assertNull(history._states.get(3));

    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUnknownChangeId() throws OperationException {
        when(gridStore.listCachedGridIds()).thenReturn(Collections.emptySet());

        History history = new History(initialState, dataStore, gridStore, entries, 1, 1234L);

        history.undoRedo(34782L);
    }

    @Test
    public void testEraseUndoneChanges() throws OperationException, ParsingException {
        when(gridStore.listCachedGridIds()).thenReturn(Collections.emptySet());

        History history = new History(initialState, dataStore, gridStore, entries, 1, 1234L);

        Assert.assertEquals(history.getPosition(), 1);
        Assert.assertEquals(history.getCurrentGrid(), intermediateState);
        Assert.assertEquals(history.getEntries(), entries);

        // Adding an entry when there are undone changes erases those changes
        history.addEntry(newEntry.getId(), newEntry.getOperation());

        Assert.assertEquals(history.getPosition(), 2);
        Assert.assertEquals(history.getCurrentGrid(), newState);
        Assert.assertEquals(history.getEntries().size(), 2);
        verify(dataStore, times(1)).discardAll(secondChangeId);
    }

}
