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

package org.openrefine.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ReconStatsTests {

    @Test
    public void serializeReconStats() {
        ReconStats rs = new ReconStats(3, 1, 2);
        TestUtils.isSerializedTo(rs, "{\"nonBlanks\":3,\"newTopics\":1,\"matchedTopics\":2}", ParsingUtilities.defaultWriter);
    }

    @Test
    public void testCreateFromColumn() {
        ReconStats rs = new ReconStats(3, 1, 2);
        GridState state = mock(GridState.class);
        when(state.computeRowFacets(Mockito.any())).thenReturn(Collections.singletonList(rs));
        when(state.getColumnModel()).thenReturn(new ColumnModel(Collections.singletonList(new ColumnMetadata("some column"))));

        Assert.assertEquals(ReconStats.create(state, "some column"), rs);
    }

    @Test
    public void testEquals() {
        ReconStats reconA = new ReconStats(10, 8, 1);
        ReconStats reconB = new ReconStats(10, 8, 1);
        ReconStats reconC = new ReconStats(10, 8, 2);

        Assert.assertEquals(reconA, reconB);
        Assert.assertNotEquals(reconA, reconC);
        Assert.assertNotEquals(reconA, 18);
    }

    @Test
    public void testToString() {
        ReconStats recon = new ReconStats(10, 8, 1);
        Assert.assertTrue(recon.toString().contains("ReconStats"));
    }
}
