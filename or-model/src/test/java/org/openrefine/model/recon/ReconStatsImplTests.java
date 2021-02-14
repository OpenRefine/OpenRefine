/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package org.openrefine.model.recon;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.recon.ReconStats;
import org.openrefine.model.recon.ReconStatsImpl;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ReconStatsImplTests {

    @Test
    public void serializeReconStats() {
        ReconStats rs = new ReconStatsImpl(3, 1, 2);
        TestUtils.isSerializedTo(rs, "{\"nonBlanks\":3,\"newTopics\":1,\"matchedTopics\":2}", ParsingUtilities.defaultWriter);
    }

    @Test
    public void testCreateFromColumn() {
        ReconStats rs = new ReconStatsImpl(3, 1, 2);
        GridState state = mock(GridState.class);
        when(state.aggregateRows(Mockito.any(), Mockito.eq(ReconStats.ZERO))).thenReturn(rs);
        when(state.getColumnModel()).thenReturn(new ColumnModel(Collections.singletonList(new ColumnMetadata("some column"))));

        Assert.assertEquals(ReconStatsImpl.create(state, "some column"), rs);
    }

    @Test
    public void testCreateMultipleColumns() {
        ReconConfig reconConfig = mock(ReconConfig.class);
        ColumnModel initialColumnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("foo").withReconConfig(reconConfig),
                new ColumnMetadata("bar"),
                new ColumnMetadata("hey").withReconConfig(reconConfig)));
        GridState state = mock(GridState.class);
        when(state.aggregateRows(Mockito.any(), Mockito.any())).thenReturn(new ReconStatsImpl.MultiReconStats(
                Arrays.asList(
                        new ReconStatsImpl(3, 1, 2),
                        new ReconStatsImpl(8, 3, 4))));
        when(state.getColumnModel()).thenReturn(initialColumnModel);

        ColumnModel expectedColumnModel = initialColumnModel
                .withReconStats(0, new ReconStatsImpl(3, 1, 2))
                .withReconStats(2, new ReconStatsImpl(8, 3, 4));
        GridState expectedGridState = mock(GridState.class);
        when(expectedGridState.getColumnModel()).thenReturn(expectedColumnModel);
        when(state.withColumnModel(expectedColumnModel))
                .thenReturn(expectedGridState);

        GridState updatedGrid = ReconStatsImpl.updateReconStats(state);
        ColumnModel columnModel = updatedGrid.getColumnModel();
        Assert.assertEquals(columnModel, expectedColumnModel);
    }

    @Test
    public void testEquals() {
        ReconStats reconA = new ReconStatsImpl(10, 8, 1);
        ReconStats reconB = new ReconStatsImpl(10, 8, 1);
        ReconStats reconC = new ReconStatsImpl(10, 8, 2);

        Assert.assertEquals(reconA, reconB);
        Assert.assertNotEquals(reconA, reconC);
        Assert.assertNotEquals(reconA, 18);
    }

    @Test
    public void testToString() {
        ReconStats recon = new ReconStatsImpl(10, 8, 1);
        Assert.assertTrue(recon.toString().contains("ReconStats"));
    }
}
