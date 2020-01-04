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
package org.openrefine.operations.recon;
import org.openrefine.RefineTest;
import org.openrefine.browsing.facets.FacetConfigResolver;
import org.openrefine.browsing.facets.RangeFacet.RangeFacetConfig;
import org.openrefine.browsing.facets.TimeRangeFacet.TimeRangeFacetConfig;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.recon.ReconMatchBestCandidatesOperation;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class ReconMatchBestCandidatesOperationTests extends RefineTest {
    @BeforeSuite
    public void registerOperation() {
    	FacetConfigResolver.registerFacetConfig("core", "range", RangeFacetConfig.class);
    	FacetConfigResolver.registerFacetConfig("core", "timerange", TimeRangeFacetConfig.class);
        OperationRegistry.registerOperation("core", "recon-match-best-candidates", ReconMatchBestCandidatesOperation.class);
    }
    
    @Test
    public void serializeReconMatchBestCandidatesOperation() throws Exception {
        String json = "{"
                + "\"op\":\"core/recon-match-best-candidates\","
                + "\"description\":\"Match each cell to its best recon candidate in column organization_name\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":["
                + "       {\"selectNumeric\":true,\"expression\":\"cell.recon.best.score\",\"selectBlank\":false,\"selectNonNumeric\":true,\"selectError\":true,\"name\":\"organization_name: best candidate's score\",\"from\":13,\"to\":101,\"type\":\"core/range\",\"columnName\":\"organization_name\"},"
                + "       {\"selectNonTime\":true,\"expression\":\"grel:toDate(value)\",\"selectBlank\":true,\"selectError\":true,\"selectTime\":true,\"name\":\"start_year\",\"from\":410242968000,\"to\":1262309184000,\"type\":\"core/timerange\",\"columnName\":\"start_year\"}"
                + "]},"
                + "\"columnName\":\"organization_name\""
                + "}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconMatchBestCandidatesOperation.class), json, ParsingUtilities.defaultWriter);
    }
}
